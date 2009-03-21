package org.dancres.blitz.remote.perf;

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
class ProxyFactory {
    private static boolean isSecure(Server aStub) {
        return (aStub instanceof RemoteMethodControl);
    }

    static LeaseImpl newLeaseImpl(Server aStub, Uuid aUuid,
                                  FakeUID aUID, long anExpiration) {
        if (isSecure(aStub))
            return new ConstrainableLeaseImpl(aStub, aUuid, aUID,
                                              anExpiration);
        else
            return new LeaseImpl(aStub, aUuid, aUID, anExpiration);
    }
}
