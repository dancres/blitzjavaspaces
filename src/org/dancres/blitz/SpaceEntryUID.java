package org.dancres.blitz;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.lease.SpaceUID;

/**
   Opaque space-level unique identifier for an Entry. <P>

   This will be the cookie we place inside a lease.  We should make this
   cookie responsible for doing a renew.  i.e. put the renew code in here.
 */
final class SpaceEntryUID implements SpaceUID, Comparable {
    private OID theOID;
    private String theType;

    SpaceEntryUID(String aType, OID aOID) {
        theType = aType;
        theOID = aOID;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof SpaceEntryUID) {
            SpaceEntryUID myUID = (SpaceEntryUID) anObject;

            if (theType.equals(myUID.theType)) {
                return (theOID.equals(myUID.theOID));
            }
        }

        return false;
    }

    public int hashCode() {
        return theOID.hashCode();
    }

    public int compareTo(Object anObject) {
        SpaceEntryUID myOther = (SpaceEntryUID) anObject;

        if (myOther.theType.equals(theType)) {
            return theOID.compareTo(myOther.theOID);
        } else
            return theType.compareTo(myOther.theType);
    }

    String getType() {
        return theType;
    }

    OID getOID() {
        return theOID;
    }

    public String toString() {
        return "EGUID:" + theType + ":" + theOID;
    }
}
