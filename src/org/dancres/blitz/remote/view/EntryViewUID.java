package org.dancres.blitz.remote.view;

import net.jini.id.Uuid;

import org.dancres.blitz.lease.SpaceUID;

public class EntryViewUID implements SpaceUID {
    private Uuid theViewId;

    EntryViewUID(Uuid aUuid) {
        theViewId = aUuid;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof EntryViewUID) {
            EntryViewUID myOther = (EntryViewUID) anObject;

            return (myOther.theViewId.equals(theViewId));
        }

        return false;
    }

    public int hashCode() {
        return theViewId.hashCode();
    }

    Uuid getViewId() {
        return theViewId;
    }
}