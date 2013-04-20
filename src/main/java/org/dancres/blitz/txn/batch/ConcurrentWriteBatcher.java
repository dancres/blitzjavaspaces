package org.dancres.blitz.txn.batch;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;

import java.util.logging.Level;

import org.prevayler.PrevalentSystem;
import org.prevayler.Command;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.PrevaylerCore;
import org.prevayler.implementation.Snapshotter;

/**
   <p>Batches commands issued in the same time period together in an attempt
   to improve log throughput via Prevayler.</p>

   <p>Batch-writing can reduce the number of forced flushes to disk whilst
   increasing the amount of data written with each flush.  This has a
   positive effect on throughput under concurrent load.</p>
   
   <p>Time is specified in ms, first thread into barrier waits this amount
   of time for other writers.  Other writers entering the barrier are
   now blocked until the first entrant commits all writes to log.  When first
   thread awakes, all writes are done as a group followed by a single sync.</p>

   <p>Unlike <code>WriteBatcher</code>, this version supports more concurrency
   allowing for queuing of the next batch of log entries whilst the current
   bunch is being written.</p>
 */
public class ConcurrentWriteBatcher implements SnapshotPrevayler {
    private PrevaylerCore thePrevayler;

    private boolean amFirst = true;

    private ArrayList theWrites = new ArrayList();

    // Might be able to buff up to 60ms which gives average of 30 but
    // we'll see.
    private long theWindowTimeMs = 20;
    private int theWindowTimeNs = 0;


    public ConcurrentWriteBatcher(PrevaylerCore aPrevayler,
        long aWindowTimeMs, int aWindowTimeNs) {
        
        thePrevayler = aPrevayler;
        theWindowTimeMs = aWindowTimeMs;
        theWindowTimeNs = aWindowTimeNs;
    }

    /**
     * Returns the underlying PrevalentSystem.
     */
    public PrevalentSystem system() {
        return thePrevayler.system();
    }

    public Serializable executeCommand(Command aCommand) throws Exception {
        return write(aCommand);
    }

    public Serializable executeCommand(Command aCommand, boolean sync)
        throws Exception {
        return write(aCommand);
    }

    private Serializable write(Command aComm) throws Exception {
        WriteRequest myReq = new WriteRequest(aComm, system());

        boolean wasFirst = false;

        WriteRequest[] myRequests  = null;

        synchronized(this) {
            if (amFirst) {
                theWrites.add(myReq);
                amFirst = false;
                wasFirst = true;

                try {
                    wait(theWindowTimeMs, theWindowTimeNs);
                } catch (InterruptedException anIE) {
                }

                myRequests = new WriteRequest[theWrites.size()];
                myRequests = (WriteRequest[]) theWrites.toArray(myRequests);

                theWrites.clear();
                amFirst = true;

            } else {
                theWrites.add(myReq);
            }
        }

        if (wasFirst) {
            flushAll(myRequests);
        }

        return myReq.getResult();
    }

    public Snapshotter takeSnapshot() throws IOException {
        return thePrevayler.takeSnapshot();
    }

    private void flushAll(WriteRequest[] aRequests) {
        int myLast = aRequests.length - 1;

        if (WriteBatcher.theLogger.isLoggable(Level.FINE))
            WriteBatcher.theLogger.log(Level.FINE,
                                       "Flushing " +
                                       aRequests.length + " to log");

        // Do all writes, last with sync
        for (int i = 0; i < aRequests.length; i++) {
            WriteRequest myReq = aRequests[i];

            myReq.execute(thePrevayler, (i == myLast));                
        }
    }

    private static class WriteRequest {
        private PrevalentSystem theSystem;
        private Command theCommand;

        private Exception theException;

        private boolean isDone;

        WriteRequest(Command aCommand, PrevalentSystem aSystem) {
            theCommand = aCommand;
            theSystem = aSystem;
        }

        void execute(PrevaylerCore aPrev, boolean doSync) {
            try {
                aPrev.logCommand(theCommand, doSync);
            } catch (Exception anE) {
                theException = anE;
            } finally {
                synchronized(this) {
                    isDone = true;
                    notify();
                }
            }
        }

        Serializable getResult() throws Exception{
            synchronized(this) {
                while (!isDone) {
                    try {
                        wait();
                    } catch (InterruptedException anIE) {
                    }
                }
            }

            if (theException != null)
                throw theException;
            else {
                Serializable myResult = theCommand.execute(theSystem);
                return myResult;
            }
        }
    }
}
