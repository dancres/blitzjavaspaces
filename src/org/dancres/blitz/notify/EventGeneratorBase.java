package org.dancres.blitz.notify;

import org.dancres.blitz.oid.OID;

public abstract class EventGeneratorBase implements EventGenerator {
    protected OID theOID;

    public void assign(OID anOID) {
        theOID = anOID;
    }

    public final OID getId() {
        return theOID;
    }

    public int hashCode() {
        return theOID.hashCode();
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof EventGeneratorBase) {
            EventGeneratorBase myOther = (EventGeneratorBase) anObject;

            return myOther.getId().equals(theOID);
        }

        return false;
    }

    public int compareTo(Object anObject) {
        EventGeneratorBase myOther = (EventGeneratorBase) anObject;

        return theOID.compareTo(myOther.getId());
    }
}
