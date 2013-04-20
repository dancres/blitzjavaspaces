package org.dancres.blitz.notify;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.lease.SpaceUID;

/**
   Opaque space-level unique identifier for an Notify. <P>

   This will be the cookie we place inside a lease.  We should make this
   cookie responsible for doing a renew.  i.e. put the renew code in here. <P>
 */
final class SpaceNotifyUID implements SpaceUID {
    private OID theOID;

    SpaceNotifyUID(OID aOID) {
        theOID = aOID;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof SpaceNotifyUID) {
            SpaceNotifyUID myUID = (SpaceNotifyUID) anObject;

            return (theOID.equals(myUID.theOID));
        }

        return false;
    }

    OID getOID() {
        return theOID;
    }

    public int hashCode() {
        return theOID.hashCode();
    }

    public String toString() {
        return "N" + theOID;
    }
}
