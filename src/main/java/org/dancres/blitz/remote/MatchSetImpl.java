package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.core.lease.Lease;

import net.jini.space.MatchSet;

import org.dancres.blitz.EntryChit;

import org.dancres.blitz.mangler.EntryMangler;

import org.dancres.blitz.remote.view.EntryViewUID;

/**
 * @todo Consider making this thread safe, even though multi-threading against a MatchSet makes little sense in most cases
 * (like multi-thread JS transactions).
 * 
 * @author dan
 */
public class MatchSetImpl implements MatchSet {
    static final int CHUNK_SIZE = 50;
    
    private Lease theLease;
    private EntryViewUID theViewId;
    private BlitzServer theStub;

    private boolean isDone;

    private EntryChit[] theCurrentBatch;
    private int theOffset = -1;

    private int theChunkSize;

    private long theLimit;

    MatchSetImpl(BlitzServer aStub, LeaseImpl aLease, long aLimit) {
        this(aStub, aLease, CHUNK_SIZE, aLimit);
    }

    MatchSetImpl(BlitzServer aStub, LeaseImpl aLease, long aLimit,
                 EntryChit[] anInitialBatch) {
        this(aStub, aLease, CHUNK_SIZE, aLimit, anInitialBatch);
    }

    MatchSetImpl(BlitzServer aStub, LeaseImpl aLease,
                 int aChunkSize, long aLimit) {
        theStub = aStub;
        theChunkSize = aChunkSize;
        theLimit = aLimit;
        theViewId = (EntryViewUID) aLease.getUID();
        theLease = aLease;
    }

    MatchSetImpl(BlitzServer aStub, LeaseImpl aLease,
                 int aChunkSize, long aLimit, EntryChit[] aFirstBatch) {
        theStub = aStub;
        theChunkSize = aChunkSize;
        theLimit = aLimit;
        theViewId = (EntryViewUID) aLease.getUID();
        theLease = aLease;

        // Setup index offset if we got a batch
        if (aFirstBatch != null) {            
            theOffset = 0;
            theCurrentBatch = aFirstBatch;
        }
    }

    public Lease getLease() {
        return theLease;
    }

    public Entry next()	throws UnusableEntryException, RemoteException {
        if (amDone())
            return null;
        
        if ((theOffset == -1) || (theOffset >= theCurrentBatch.length)) {
            // Haven't got a batch yet
            theCurrentBatch = theStub.getNext(theViewId, theChunkSize);
            theOffset = 0;
        }

        if (theCurrentBatch == null) {
            isDone = true;
            return null;
        }

        --theLimit;

        return EntryMangler.getMangler().unMangle(theCurrentBatch[theOffset++].getEntry());
    }

    public Entry getSnapshot() {
        if (amDone())
            throw new RuntimeException("No entry to snapshot - iteration is over");
        if (theCurrentBatch == null)
            throw new RuntimeException("Call next first!");

        return theCurrentBatch[theOffset - 1].getEntry();
    }

    private boolean amDone() {
        return ((isDone) || (theLimit == 0));
    }
}