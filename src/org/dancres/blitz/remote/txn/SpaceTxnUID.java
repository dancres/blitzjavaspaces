package org.dancres.blitz.remote.txn;

import org.dancres.blitz.lease.SpaceUID;

/**
 */
public class SpaceTxnUID implements SpaceUID {
    private long theMagic;
    private long theId;

    SpaceTxnUID(long anId, long aMagic) {
        theId = anId;
        theMagic = aMagic;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof SpaceTxnUID) {
            SpaceTxnUID myUID = (SpaceTxnUID) anObject;

            return ((theId == myUID.theId) && (theMagic == myUID.theMagic));
        }

        return false;
    }

    long getId() {
        return theId;
    }

    long getMagic() {
        return theMagic;
    }

    public int hashCode() {
        return (int) (theId ^ (theId >>> 32));
    }

    public String toString() {
        return "LT:" + theId + "," + theMagic;
    }
}
