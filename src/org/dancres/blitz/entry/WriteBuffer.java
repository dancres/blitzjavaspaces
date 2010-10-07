package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.disk.WriteDaemon;

import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.DirtyBufferStat;
import org.dancres.blitz.entry.ci.CacheIndexer;

/**
   <p> Writes to disk from Storage are done asynchronously.  With this being
   the case, it's possible for SleeveCache to flush an EntrySleeveImpl in
   favour of another and then reload it later with a resultant loss of accurate
   state due to the fact that the write has yet to be done. </p>

   <p> WriteBuffer tracks all pending writes and ensures that "dirty" state
   that has yet to reach disk is always available to SleeveCache so that
   on-disk state appears to be correct. This works because we also persistently
   log such changes and ensure they've been applied at recovery or an
   intermediate checkpoint. </p>

   <p> It's possible that several updates for one EntrySleeveImpl will
   be submitted to WriteBuffer.  Rather than handle each update separately,
   WriteBuffer consolidates them all into a single disk op which improves
   performance and reduces latency.</p>
 */
class WriteBuffer {
    private Map theJobInfo = new HashMap();
    private EntryEditor theEditor;

    WriteBuffer(EntryEditor anEditor) {
        theEditor = anEditor;
        StatsBoard.get().add(new DirtyBufferGenerator(theEditor.getType(), theJobInfo));
    }

    void add(EntrySleeveImpl aSleeve) throws IOException {
        try {
            OID myUid = aSleeve.getOID();

            if (aSleeve.getState().test(SleeveState.PINNED))
                throw new RuntimeException("Panic, shouldn't be seeing PINNED");

            // We must duplicate to ensure that further changes by the
            // upper layer, don't pollute us.
            //
            WriteRequest myRequest = new WriteRequest(aSleeve.getState().get(),
                aSleeve.getPersistentRep().duplicate());

            boolean schedule = false;

            synchronized(theJobInfo) {
                ArrayList myUpdates = 
                    (ArrayList) theJobInfo.get(myUid);

                // Queue exists?
                if (myUpdates == null) {

                    /*
                      MULTI-THREAD WRITING:

                      At this point, go to WriteDaemon and ask for a queue
                      to dispatch to.  Stamp this onto the myUpdates
                      (which can therefore no longer be a simple ArrayList)
                     */
                    myUpdates = new ArrayList();
                    theJobInfo.put(myUid, myUpdates);
                }

                synchronized(myUpdates) {
                    // If nothing is pending, buffer it and indicate we need
                    // a job scheduled.
                    if (myUpdates.size() == 0) {
                        myUpdates.add(myRequest);
                        schedule = true;
                    } else {
                        // Recover last update (there should be only one?)
                        WriteRequest myLast = (WriteRequest)
                            myUpdates.get(myUpdates.size() - 1);

                        try {
                            boolean wasActive;

                            myLast.lock();

                            wasActive = myLast.isActive();

                            // If the last request is not active we can merge
                            // without queue'ing another job.
                            if (!wasActive) {
                                myLast.merge(myRequest);
                            }

                            myLast.unlock();

                            // Last request was active, queue a new one and
                            // ask for a job to be scheduled.
                            if (wasActive) {
                                myUpdates.add(myRequest);
                                schedule = true;
                            }

                        } catch (InterruptedException anIE) {
                            EntryStorage.theLogger.log(Level.SEVERE,
                                                       "Failed to lock previous request", anIE);
                        }
                    }
                }

                /*
                  Make sure we flip NOT_ON_DISK because we're really
                  writing.  This might seem odd - for example, what would
                  happen if we had a Sleeve with NOT_ON_DISK | DELETE?
                  Well, because the Sleeve is DELETE, no other thread
                  should dirty the cache entry again.  Were it to do so
                  if will cause a write problem but it would also be a bug.
                */
                aSleeve.getState().clear(SleeveState.NOT_ON_DISK);

                if (schedule) {
                    Job myJob = new Job(myUid, this);

                    /*
                      MULTI-THREAD WRITING:

                      Use the queue id gathered from the list of updates
                      above as an argument to this method.

                      In this way, we can ensure all updates for one
                      UID are dispatched in the same queue which ensures
                      FIFO ordering.
                     */
                    WriteDaemon.get().queue(myJob);
                }
            }

        } catch (Exception anE) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Failed to add write to cache: " +
                                       theEditor, anE);
        }
    }

    /**
       @return copy of the entry sleeve or <code>null</code> if
       there is no entry in the cache.  
     */
    EntrySleeveImpl dirtyRead(OID aUID) {
        EntrySleeveImpl myResult = null;

        synchronized(theJobInfo) {
            ArrayList myStates = (ArrayList) theJobInfo.get(aUID);

            if (myStates != null) {
                synchronized(myStates) {
                    WriteRequest myRequest = (WriteRequest)
                        myStates.get(myStates.size() - 1);

                    try {
                        myRequest.lock();

                        myResult = myRequest.newSleeve();

                        myRequest.unlock();
                    } catch (InterruptedException anIE) {
                        EntryStorage.theLogger.log(Level.SEVERE, 
                                                   "Failed to copy sleeve",
                                                   anIE);
                    }
                }
            }
        }

        return myResult;
    }

    /**
       Callback for the Job when it is executed by the WriteDaemon
     */
    void update(OID aUID) {
        try {
            ArrayList myQueue;

            synchronized(theJobInfo) {
                myQueue = (ArrayList) theJobInfo.get(aUID);
            }

            WriteRequest myRequest;

            synchronized(myQueue) {
                myRequest = (WriteRequest) myQueue.get(0);

                try {
                    myRequest.lock();

                    myRequest.markActive();

                    myRequest.unlock();
                } catch (InterruptedException anIE) {
                    EntryStorage.theLogger.log(Level.SEVERE, 
                                               "Failed to lock request for processing", anIE);
                }
            }

            myRequest.flush(theEditor);

            /*
                Regardless of which write this is, once we've done one, the
                indexes on disk contain any search material we require and
                thus we can clear the CacheIndexer.
             */
            if ((myRequest.getStateFlags() & SleeveState.NOT_ON_DISK) != 0) {
                /*
                    If it's not also marked as deleted (in which case the
                    indexer will already be updated)
                 */
                if ((myRequest.getStateFlags() & SleeveState.DELETED) == 0) {
                    CacheIndexer.getIndexer(
                        theEditor.getType()).flushed(myRequest.getSleeve());
                }
            }

            synchronized(theJobInfo) {
                synchronized(myQueue) {
                    
                    // Remove the consolidated image stored previously
                    myQueue.remove(0);

                    if (myQueue.size() == 0) {
                        theJobInfo.remove(aUID);
                    }
                }
            }

        } catch (Exception anE) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Failed to sync cache: " + theEditor,
                                       anE);
        }
    }

    private static final class Job implements Runnable {
        private OID theUID;
        private WriteBuffer theBuffer;

        Job(OID aUID, WriteBuffer aBuffer) {
            theUID = aUID;
            theBuffer = aBuffer;
        }

        public void run() {
            theBuffer.update(theUID);
        }
    }

    private static final class WriteRequest {
        /*
          For performance measurement only
         */
        private static final Object theLockObject = new Object();
        private static int numMerges;

        private int theStateFlags;

        private boolean isActive;

        private PersistentEntry theEntry;

        private ReentrantLock theLock = new ReentrantLock();

        WriteRequest(int aState, PersistentEntry anEntry) {
            theEntry = anEntry;
            theStateFlags = aState;
        }

        void markActive() {
            isActive = true;
        }

        boolean isActive() {
            return isActive;
        }

        void lock() throws InterruptedException {
            theLock.lock();
        }

        void unlock() {
            theLock.unlock();
        }

        int getStateFlags() {
            return theStateFlags;
        }

        EntrySleeveImpl getSleeve() {
            EntrySleeveImpl mySleeve = new EntrySleeveImpl(theEntry);

            mySleeve.getState().setExplicit(theStateFlags);
            mySleeve.getState().clear(SleeveState.NOT_ON_DISK);

            return mySleeve;
        }

        void merge(WriteRequest anOther) {
            /*
            synchronized(theLockObject) {
                ++numMerges;
                System.err.println("Coalesce: " + numMerges);
            }
            */

            theStateFlags |= anOther.theStateFlags;

            theEntry = anOther.getSleeve().getPersistentRep();
        }

        EntrySleeveImpl newSleeve() {
            EntrySleeveImpl mySleeve =
                new EntrySleeveImpl(theEntry.duplicate());
            
            mySleeve.getState().setExplicit(theStateFlags);
            mySleeve.getState().clear(SleeveState.NOT_ON_DISK);

            return mySleeve;
        }

        void flush(EntryEditor anEditor) throws IOException {
            if ((theStateFlags & SleeveState.DELETED) != 0) {
                if ((theStateFlags & SleeveState.NOT_ON_DISK) == 0) {
                    anEditor.delete(theEntry);
                }
            } else if ((theStateFlags & SleeveState.NOT_ON_DISK) != 0){
                anEditor.write(theEntry);
            } else {
                anEditor.update(theEntry);
            }
        }
    }

    public class DirtyBufferGenerator implements StatGenerator {
        private long _id = StatGenerator.UNSET_ID;
        
        private Map _jobInfo;
        private String _type;

        public DirtyBufferGenerator(String aType, Map aJobInfo) {
            _type = aType;
            _jobInfo = aJobInfo;
        }

        public long getId() {
            return _id;
        }

        public void setId(long anId) {
            _id = anId;
        }

        public Stat generate() {
            int myBufferSize;

            synchronized(_jobInfo) {
                myBufferSize = _jobInfo.size();
            }

            return new DirtyBufferStat(_id, _type, myBufferSize);
        }
    }
}
