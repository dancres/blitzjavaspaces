package org.dancres.blitz.remote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InvalidObjectException;

import java.lang.reflect.Method;

import net.jini.id.Uuid;

import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.constraint.MethodConstraints;

import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;

import net.jini.core.lease.Lease;

import net.jini.admin.JoinAdmin;

import net.jini.id.Uuid;

import com.sun.jini.proxy.ConstrainableProxyUtil;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.util.ReflectUtil;

/**
   When running in "secure mode", Leases should be constrainable as per
   other proxies.
 */
public final class ConstrainableLeaseImpl extends
    LeaseImpl implements RemoteMethodControl {

    /**
       The outer-layer calls all omit SpaceUID whilst the server
       calls use SpaceUID so we must translate constraints which will be
       applied against the outer-layer methods to the internal methods.
     */
    private static final Method[] theMethodMapping = {
        ReflectUtil.findMethod(Lease.class, "renew",
                               new Class[] {long.class}),
        ReflectUtil.findMethod(Landlord.class, "renew",
                               new Class[] {SpaceUID.class, long.class}),
        ReflectUtil.findMethod(Lease.class, "cancel",
                               new Class[] {}),
        ReflectUtil.findMethod(Landlord.class, "cancel",
                               new Class[] {SpaceUID.class})
    };

    private static Landlord constrainStub(Landlord aServer,
                                          MethodConstraints aConstraints) {
        RemoteMethodControl myServer = (RemoteMethodControl) aServer;

        MethodConstraints myStubConstraints =
            ConstrainableProxyUtil.translateConstraints(aConstraints,
                                                        theMethodMapping);

        myServer.setConstraints(myStubConstraints);
        return (Landlord) myServer;
    }

    private final MethodConstraints theConstraints;

    ConstrainableLeaseImpl(Landlord aLandlord, Uuid aUuid, SpaceUID aUID,
                           long aDuration) {
        super(aLandlord, aUuid, aUID, aDuration);
        theConstraints = null;
    }

    ConstrainableLeaseImpl(Landlord aLandlord, Uuid aUuid, SpaceUID aUID,
                           long aDuration, MethodConstraints aConstraints) {
        super(constrainStub(aLandlord, aConstraints), aUuid, aUID,
              aDuration);
        theConstraints = aConstraints;
    }

    private ProxyTrustIterator getProxyTrustIterator() {
        return new SingletonProxyTrustIterator(theStub);
    }

    public RemoteMethodControl setConstraints(MethodConstraints aConstraints) {
        return new ConstrainableLeaseImpl(theStub, theUuid, getUID(),
                                          expiration, aConstraints);
    }

    public MethodConstraints getConstraints() {
        return theConstraints;
    }
}
