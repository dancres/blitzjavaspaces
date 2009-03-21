package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import com.sun.jini.outrigger.AdminIterator;

import org.dancres.blitz.EntryChit;

import org.dancres.blitz.mangler.EntryMangler;

import org.dancres.blitz.remote.view.EntryViewUID;

public class AdminIteratorImpl implements AdminIterator {
    static final int CHUNK_SIZE = 50;
    
    private EntryViewUID theViewId;
    private AdminServer theStub;
    private EntryMangler theMangler;

    private boolean isDone;

    private EntryChit[] theCurrentBatch;
    private int theOffset = -1;

    private int theChunkSize;

    AdminIteratorImpl(EntryMangler aMangler, EntryViewUID aViewId,
                      AdminServer aStub) {
        this(aMangler, aViewId, CHUNK_SIZE, aStub);
    }

    AdminIteratorImpl(EntryMangler aMangler, EntryViewUID aViewId,
                      AdminServer aStub, EntryChit[] anInitialBatch) {
        this(aMangler, aViewId, CHUNK_SIZE, aStub, anInitialBatch);
    }

    AdminIteratorImpl(EntryMangler aMangler, EntryViewUID aViewId,
                      int aChunkSize, AdminServer aStub) {
        theViewId = aViewId;
        theStub = aStub;
        theMangler = aMangler;
        theChunkSize = aChunkSize;
    }

    AdminIteratorImpl(EntryMangler aMangler, EntryViewUID aViewId,
                      int aChunkSize, AdminServer aStub, EntryChit[] anInitialBatch) {
        theViewId = aViewId;
        theStub = aStub;
        theMangler = aMangler;
        theChunkSize = aChunkSize;

        if (anInitialBatch != null) {
            theCurrentBatch = anInitialBatch;
            theOffset = 0;
        }
    }

    public Entry next()	throws UnusableEntryException, RemoteException {
        if (isDone)
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

        return theMangler.unMangle(theCurrentBatch[theOffset++].getEntry());
    }

    public void delete() throws RemoteException {
        if (theCurrentBatch != null)
            theStub.delete(theCurrentBatch[theOffset - 1].getCookie());
    }

    public void close() throws RemoteException {
        theStub.close(theViewId);
    }
}