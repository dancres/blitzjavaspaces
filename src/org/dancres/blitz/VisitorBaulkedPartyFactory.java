package org.dancres.blitz;

import org.dancres.blitz.txnlock.BaulkedParty;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.oid.OID;

/**
 * <code>SearchVisitor</code>s must supply a conflict resolution implementation
 * to <code>TxnLock</code>s however they have insufficient context to determine
 * what that implementation is and we need the code using
 * <code>SearchVisitor</code>s to be able to interact with the conflict
 * resolution code.  To handle both these issues, we have
 * <code>SearchVisitor</code>s accept an instance of the below and use it
 * to generate conflict handlers for the transaction code.
 */
public interface VisitorBaulkedPartyFactory {
    public BaulkedParty newParty(SingleMatchTask aMatchTask);
    public BaulkedParty newParty(BulkTakeVisitor aMatchTask);
    public void enableResolutionSignal();

    /**
     * Common class to be used as handbacks for the instances of
     * <code>BaulkedParty</code> returned by the underlying factory
     * implementation.
     */
    static class Handback {
        private String theType;
        private OID theOID;
        private MangledEntry theEntry;

        Handback(String aType, OID anOID, MangledEntry anEntry) {
            theType = aType;
            theOID = anOID;
            theEntry = anEntry;
        }

        String getType() {
            return theType;
        }

        OID getOID() {
            return theOID;
        }

        MangledEntry getEntry() {
            return theEntry;
        }
    }
}
