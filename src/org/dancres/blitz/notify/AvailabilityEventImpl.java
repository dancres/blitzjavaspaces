package org.dancres.blitz.notify;

import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.space.AvailabilityEvent;
import net.jini.space.JavaSpace;

import org.dancres.blitz.mangler.MangledEntry;

public class AvailabilityEventImpl extends AvailabilityEvent {
    private MangledEntry theEntry;

    AvailabilityEventImpl(JavaSpace aSource, long aSourceId, long aSeqNum,
                          MarshalledObject aHandback, MangledEntry anEntry,
                          boolean isVisible) {
        super(aSource, aSourceId, aSeqNum, aHandback, isVisible);
        theEntry = anEntry;
    }

    public Entry getEntry() throws UnusableEntryException {
        return theEntry.get();
    }

    public Entry getSnapshot() {
        return theEntry.getSnapshot();
    }
}