package org.dancres.blitz.remote.debug;

import java.rmi.Remote;

import java.rmi.server.ExportException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.jini.jeri.ProxyTrustILFactory;
import net.jini.jeri.ObjectEndpoint;

import net.jini.core.constraint.MethodConstraints;

/**
   <p>A custom <code>InvocationLayerFactory</code> for JERI which allows a
   Blitz instance to be debugged or monitored in custom fashions.  Simply
   replace the <code>BasicILFactory</code> or <code>ProxyTrustILFactory</code>
   in the configuration file with an instance of <code>DebugILFactory</code>
   (which accepts the same paramters).</p>

   <p>This factory inserts a custom <code>InvocationHandler</code>,
   <code>TimingInvocationHandler</code>. </p>

   @see org.dancres.blitz.remote.test.debug.TimingInvocationHandler
 */
public class DebugILFactory extends ProxyTrustILFactory {

    public DebugILFactory(MethodConstraints aConstraints,
                          Class aPermissionsClass) {
        super(aConstraints, aPermissionsClass, null);
    }

    public DebugILFactory(MethodConstraints aConstraints,
                          Class aPermissionsClass,
                          ClassLoader aLoader) {
        super(aConstraints, aPermissionsClass, aLoader);
    }

    protected InvocationHandler
        createInvocationHandler(Class[] aSetOfInterfaces, Remote anImpl,
                                ObjectEndpoint anEndpoint) 
        throws ExportException {

        for (int i = aSetOfInterfaces.length; --i >= 0; ) {
            if (aSetOfInterfaces[i] == null) {
                throw new NullPointerException();
            }
        }
        if (anImpl == null) {
            throw new NullPointerException();
        }
        return new TimingInvocationHandler(anEndpoint, getServerConstraints());
    }
}
