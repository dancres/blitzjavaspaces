package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.HashMap;

import java.util.logging.Level;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.entry.ci.CacheIndexer;

/**
   <p> Transactional writes add a further complication to the caching process
   because, whilst they can be flushed from SleeveCache, they can't necessarily
   be written to disk.  Consider the case where a transaction starts and
   writes an entry, other activity causes that entry to be flushed from
   SleeveCache into the asynchronous write queue and then saved to disk.
   The system now crashes prior to the original writing transaction issuing
   a successful prepare.  At recovery, we will have an additional entry on
   disk unprotected by transactional locks and it would be valid for matching.
   </p>

   <p> WriteScheduler helps solve this problem by supporting "Transient"
   EntrySleeveImpls.  EntrySleeveImpls marked as transient are held in
   WriteScheduler but not schedule to be written to disk.  WriteScheduler
   maintains an index of all Entry's to assist in searching so these
   Transient's are available for matching.  When a transaction commits, the
   Transient mark is removed from EntrySleeveImpls (these sleeves must be
   reloaded into SleeveCache for this to happen) and they are re-submitted to
   WriteScheduler at which point they are scheduled for writing. </p>

   <p> Thus, until a transaction is commited/aborted, any EntrySleeveImpls
   written are marked Transient.  During commit/abort, these Sleeves have the
   mark removed and are thus eligible for writing to disk. </p>
*/
class WriteScheduler {
    private HashMap theTransients = new HashMap();

    private WriteBuffer theBuffer;
	private String theType;
	
    WriteScheduler(EntryEditor anEditor) {
		theType = anEditor.getType();
        theBuffer = new WriteBuffer(anEditor);
    }

    public int getSize() {
        throw new org.dancres.util.NotImplementedException();
    }

    void add(EntrySleeveImpl aSleeve) throws IOException {
        /*
            Cache has no concept of dirty, it flushes content out of a block
            whenever it wishes to use the block for something else.  So,
            it's possible we'll be asked to flush an empty block.
         */
        if (aSleeve == null)
            return;

        /*
            Whether the sleeve is dirty or not, if it's pinned we remember it.
            Pinned implies some entity will do more with this sleeve later
            including potentially dirtying state which might be clean at
            this stage.  Thus we must remember the state clean or dirty until
            the pin is released.
         */
        SleeveState myState = aSleeve.getState();

        if (myState.test(SleeveState.PINNED)) {
            synchronized(theTransients) {
                theTransients.put(aSleeve.getOID(), aSleeve);
            }

            return;
        }

        /*
          Cache flushed the entry - do we need to save it?
        */
        if (!aSleeve.isDirty())
            return;

        boolean dontWrite = myState.test(SleeveState.NOT_ON_DISK) &&
            myState.test(SleeveState.DELETED);

        /*
            If we're not writing this entry, clear it from the indexer now
            otherwise leave it to the write buffer to do when it's pushed
            the entry to disk
        if (dontWrite) {
            CacheIndexer.getIndexer(theType).flushed(aSleeve);
        }
         */

        synchronized(theTransients) {
            theTransients.remove(aSleeve.getOID());

            if (!dontWrite)
                theBuffer.add(aSleeve);
        }

        /*
         Note we do not manage NOT_ON_DISK - this is dealt with in the
         WriteBuffer as it's vital state it needs to determine whether or
         not to actually update disk.
        */
        aSleeve.clearDirty();
    }

    EntrySleeveImpl dirtyRead(OID aUID) {
        EntrySleeveImpl mySleeve = null;

        synchronized(theTransients) {
            mySleeve = (EntrySleeveImpl) theTransients.get(aUID);
        }

        if (mySleeve == null)
            mySleeve = theBuffer.dirtyRead(aUID);

        return mySleeve;
    }
}
