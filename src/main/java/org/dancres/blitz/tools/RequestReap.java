package org.dancres.blitz.tools;

import java.io.IOException;

import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;

import net.jini.discovery.*;

import net.jini.lookup.*;
import net.jini.lookup.entry.Name;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;

import net.jini.core.entry.Entry;

import net.jini.admin.Administrable;

import net.jini.space.JavaSpace;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.remote.BlitzAdmin;

import org.dancres.jini.util.DiscoveryUtil;
import org.dancres.jini.util.ServiceLocator;

/**
   <p>RequestReap accepts a spacename as an argument and attempts to
   trigger a manual reap of any lease expired contents.</p>

   <p>Typical usage:

   <pre>
   java -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/jini/jini2_0/lib/jsk-platform.jar:/home/dan/src/jini/space/build:/home/dan/jini/jini2_0/lib/jini-ext.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.RequestReap dancres
   </pre>

   @see org.dancres.blitz.remote.BlitzAdmin
 */
public class RequestReap {
    private static final long MAX_DISCOVER_TIME = 15 * 1000;

    public static void main(String args[]) {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        try {
            try {
                Object myProxy = null;

                if (args.length == 1)
                    myProxy = ServiceLocator.getService(JavaSpace.class,
                                                        args[0],
                                                        MAX_DISCOVER_TIME);
                else if (args.length == 2) {
                    myProxy = ServiceLocator.getService(args[0],
                                                        JavaSpace.class,
                                                        args[1]);
                } else {
                    System.err.println("Wrong number of arguments - should be <spacename> or <LUS host> <spacename>");
                    System.exit(-1);
                }
                    
                if (myProxy != null) {
                    System.err.println("Found space: " + myProxy);

                    DiscoveryUtil.dumpInterfaces(myProxy.getClass());

                    if (DiscoveryUtil.hasInterface(myProxy,
                                                   Administrable.class)) {
                        Administrable myAdmin = (Administrable) myProxy;

                        Object myAdminProxy = myAdmin.getAdmin();

                        DiscoveryUtil.dumpInterfaces(myAdminProxy.getClass());

                        if (DiscoveryUtil.hasInterface(myAdminProxy,
                                                       BlitzAdmin.class)) {
                            BlitzAdmin myBlitzAdmin = 
                                (BlitzAdmin) myAdminProxy;

                            System.err.println("Invoking reap");
                            myBlitzAdmin.reap();
                        } else {
                            System.err.println("No BlitzAdmin interface found - can't be Blitz");
                        }
                    } else {
                        System.err.println("No admin interface present - can't be Blitz");
                    }
                }
            } catch (InterruptedException anIE) {
                System.err.println("!!! Whoops service not found :( !!!");
            }
        } catch (ClassNotFoundException aCNFE) {
            System.err.println("ClassNotFound exception");
            aCNFE.printStackTrace(System.err);
        } catch (RemoteException anRE) {
            System.err.println("Remote exception");
            anRE.printStackTrace(System.err);
        } catch (IOException anIOE) {
            System.err.println("Failed to configure discovery");
            anIOE.printStackTrace(System.err);
        }
    }
}
