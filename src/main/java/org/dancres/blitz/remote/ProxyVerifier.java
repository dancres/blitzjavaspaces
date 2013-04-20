package org.dancres.blitz.remote;

import java.io.Serializable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

import java.rmi.RemoteException;

import net.jini.security.TrustVerifier;

import net.jini.security.proxytrust.TrustEquivalence;

import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.constraint.MethodConstraints;

import net.jini.id.Uuid;

/**
   This class is responsible for verifying any of Blitz's proxy implementations
   including LeaseImpl, BlitzProxy, AdminProxy and TxnParticipantProxy
 */
class ProxyVerifier implements TrustVerifier, Serializable {
    private RemoteMethodControl theOriginalStub;
    private Uuid theOriginalUuid;

    /**
       Ensures that the passed stub meets the necessary criteria for
       TrustVerification.  If the stub does not qualify, we throw an
       UnsupportedOperationException.  This set of tests is necessary due
       to the fact that the stub's compliance is determind, in part by
       configuration of the appropriate Exporter in the config file.
     */
    ProxyVerifier(BlitzServer aServer, Uuid aUuid) {
        if (! (aServer instanceof RemoteMethodControl))
            throw new UnsupportedOperationException("Server stub does not support RemoteMethodControl - wrong Exporter?");

        if (! (aServer instanceof TrustEquivalence))
            throw new UnsupportedOperationException("Server stub does not support TrustEquivalance - wrong Exporter?");

        theOriginalStub = (RemoteMethodControl) aServer;
        theOriginalUuid = aUuid;
    }

    public boolean isTrustedObject(Object anObject,
                                   TrustVerifier.Context aContext)
        throws RemoteException {

        RemoteMethodControl myOtherServer;
        Uuid myOtherUuid;

        /*
          One might be tempted to implement all of this by having all proxies
          implement a particular interface and obtain the details like that
          but it opens the way to a "foreign" proxy implementing the interface
          and nothing else such that it passes all our tests but actually isn't
          our proxy - thus we test the concrete class.
         */
        if (anObject instanceof ConstrainableBlitzProxy) {
            ConstrainableBlitzProxy myProxy = (ConstrainableBlitzProxy)
                anObject;

            myOtherServer = (RemoteMethodControl) myProxy.theStub;
            myOtherUuid = myProxy.theUuid;
        } else if (anObject instanceof ConstrainableTxnParticipantProxy) {
            ConstrainableTxnParticipantProxy myProxy = 
                (ConstrainableTxnParticipantProxy) anObject;

            myOtherServer = (RemoteMethodControl) myProxy.theStub;
            myOtherUuid = myProxy.theUuid;
        } else if (anObject instanceof ConstrainableAdminProxy) {
            ConstrainableAdminProxy myProxy =
                (ConstrainableAdminProxy) anObject;

            myOtherServer = (RemoteMethodControl) myProxy.theStub;
            myOtherUuid = myProxy.theUuid;
        } else if (anObject instanceof ConstrainableLeaseImpl) {
            ConstrainableLeaseImpl myProxy =
                (ConstrainableLeaseImpl) anObject;

            myOtherServer = (RemoteMethodControl) myProxy.theStub;
            myOtherUuid = myProxy.theUuid;
        } else if ((anObject instanceof BlitzServer) &&
                   (anObject instanceof RemoteMethodControl)) {
            // Contributed services have this their code - might this be due
            // to Activation?
            myOtherServer = (RemoteMethodControl) anObject;
            myOtherUuid = theOriginalUuid;
        } else {
            // It's nothing we know about - fail it.
            return false;
        }

        if (! theOriginalUuid.equals(myOtherUuid))
            return false;

        // Get client constraints from passed proxy
        MethodConstraints myConstraints = myOtherServer.getConstraints();

        // Create copy of original server stub with constraints applied
        TrustEquivalence myConstrainedStub =
            (TrustEquivalence) theOriginalStub.setConstraints(myConstraints);

        return myConstrainedStub.checkTrustEquivalence(myOtherServer);
    }

    /**
       We override this method to check that integrity of the Verifier has
       been maintained.  There are a number of potential sources of compromise
       such as "fiddling" with the serialized steam or a "misbehaving" JVM
       implementation.
     */
    private void readObject(ObjectInputStream anOIS) 
        throws IOException, ClassNotFoundException {

        anOIS.defaultReadObject();

        if ((theOriginalStub == null) || (theOriginalUuid == null)) {
            throw new InvalidObjectException("Internal state has been compromised");
        }

        if (! (theOriginalStub instanceof TrustEquivalence))
            throw new InvalidObjectException("Stub doesn't implement TrustEquivalence");
    }
}


