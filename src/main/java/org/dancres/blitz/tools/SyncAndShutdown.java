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
   <p>SyncAndShutdown accepts a spacename as an argument and attempts to
   shut it down cleanly having sync'd it's contents to disk (this second
   step isn't strictly necessary but does ensure that all state is available
   from the underlying databases without any dependency on the log files).
   This tool will only work with a Blitz JavaSpace owing to it's dependence
   upon the <code>BlitzAdmin</code> interface.</p>

   <p>Once a Blitz instance has been shutdown in this manner, one may, for
   example, use <code>DumpEntries</code> to examine it's Entry content.</p>

   <p>Typical usage:

   <pre>
   java -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/jini/jini2_0/lib/jsk-platform.jar:/home/dan/src/jini/space/build:/home/dan/jini/jini2_0/lib/jini-ext.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.SyncAndShutdown dancres
   </pre>

   <p>This tool is non-destructive so it's perfectly possible to shut a
   Blitz instance down, run <code>DumpEntries</code> and then restart the
   space.</p>

   <p>Note if there are active transactions, Blitz will by default refuse
   to honour the request.  You can force Blitz to shutdown by adding
   <code>-Dforce=true</code> to the command-line(s) above.</p>
 
   @see org.dancres.blitz.remote.BlitzAdmin
   @see org.dancres.blitz.tools.DumpEntries
 */
public class SyncAndShutdown {
    private static final long MAX_DISCOVER_TIME = 15 * 1000;

    public static void main(String args[]) {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        boolean amForced = Boolean.getBoolean("force");

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

                            try {
                                System.err.println("Invoking checkpoint");
                                myBlitzAdmin.requestSnapshot();
                            } catch (TransactionException aTE) {
                                System.err.println("Failed to checkpoint");
                                aTE.printStackTrace(System.err);

                                if (amForced) {
                                    System.err.println("Ignoring checkpoint " +
                                        "failure and forcing shutdown");
                                } else {
                                    System.exit(-1);
                                }
                            }

                            System.err.println("Invoking shutdown");
                            myBlitzAdmin.shutdown();
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
