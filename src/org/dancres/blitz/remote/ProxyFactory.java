package org.dancres.blitz.remote;

import net.jini.id.Uuid;

import net.jini.core.constraint.RemoteMethodControl;

import org.dancres.blitz.lease.SpaceUID;

/**
   This class instantiates appropriate proxy instances of the given types
   based on whether we are running in secure mode or not (secure mode is
   defined to be active when our exported stub reference supports
   RemoteMethodControl which will be the result of config file containing
   an appropriate Exporter setup).
 */
public class ProxyFactory {
    private static boolean isSecure(BlitzServer aStub) {
        return (aStub instanceof RemoteMethodControl);
    }

    public static BlitzProxy newBlitzProxy(BlitzServer aStub, Uuid aUuid) {
        if (isSecure(aStub))
            return new ConstrainableBlitzProxy(aStub, aUuid);
        else
            return new BlitzProxy(aStub, aUuid);
    }

    static AdminProxy newAdminProxy(BlitzServer aStub, Uuid aUuid) {
        if (isSecure(aStub))
            return new ConstrainableAdminProxy(aStub, aUuid);
        else
            return new AdminProxy(aStub, aUuid);
    }

    static TxnParticipantProxy newTxnParticipantProxy(BlitzServer aStub,
                                                      Uuid aUuid) {
        if (isSecure(aStub))
            return new ConstrainableTxnParticipantProxy(aStub, aUuid);
        else
            return new TxnParticipantProxy(aStub, aUuid);
    }

    public static LeaseImpl newLeaseImpl(BlitzServer aStub, Uuid aUuid,
                                         SpaceUID aUID, long anExpiration) {
        if (isSecure(aStub))
            return new ConstrainableLeaseImpl(aStub, aUuid, aUID,
                                              anExpiration);
        else
            return new LeaseImpl(aStub, aUuid, aUID, anExpiration);
    }

    /**
     * @return a lease with no stub set.  This will be done by the <code>
     * BlitzProxy</code> on receipt.  This saves the transfer of a stub which
     * is a significant serialization cost.
     */
    public static LeaseImpl newEntryLeaseImpl(BlitzServer aStub,
        Uuid aUuid, SpaceUID aUID, long anExpiration) {
        if (isSecure(aStub))
            return new ConstrainableLeaseImpl(null, null, aUID,
                anExpiration);
        else
            return new LeaseImpl(null, null, aUID, anExpiration);
    }
}
