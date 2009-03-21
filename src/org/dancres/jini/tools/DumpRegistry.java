package org.dancres.jini.tools;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.discovery.LookupDiscoveryManager;

import org.dancres.jini.util.DiscoveryUtil;

import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
   This is an example of a simple lookup via multicast.  It gets of rid of
   the need to be aware of static lookup information because we can "discover"
   lookup services dynamically at startup.  This basic implementation has
   one fault which is that it only scans each LookupService it finds once
   for service matches (see the comments in DiscoveryListenerImpl).  We could
   go to the trouble of fixing this all ourselves but there's an easier way - 
   ServiceDiscoveryManager.
*/
public class DumpRegistry {
    public static void main(String args[]) throws Exception {
        /*
          Gotta do this to enable remote class downloading
         */
        System.setSecurityManager(new RMISecurityManager());

        /*
          We're going to look for all ServiceRegistrar instances in all
          groups - everything we can find via a multicast
         */
        String[] myGroups = LookupDiscovery.ALL_GROUPS;
        LookupLocator[] myLocators = new LookupLocator[0];

        if (args.length != 0) {
            ArrayList mySpecGroups = new ArrayList();
            ArrayList mySpecLocs = new ArrayList();

            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("jini:")) {
                    mySpecLocs.add(new LookupLocator(args[i]));
                } else {
                    mySpecGroups.add(args[i]);
                }
            }

            myGroups = new String[mySpecGroups.size()];
            myGroups = (String[]) mySpecGroups.toArray(myGroups);

            myLocators = new LookupLocator[mySpecLocs.size()];
            myLocators = (LookupLocator[]) mySpecLocs.toArray(myLocators);
        }

        try {
            LookupDiscoveryManager myDiscovery =
                new LookupDiscoveryManager(myGroups, myLocators,
                        new DiscoveryListenerImpl());
        } catch (IOException anIOE) {
            System.err.println("Couldn't setup LookupDiscovery - exiting");
            anIOE.printStackTrace(System.err);
            System.exit(-1);
        }

        /*
           The thing we need to remember about this kind of discovery is
           that things take *time*.  Locating a ServiceRegistrar doesn't happen
           immediately and is actually being done *asynchronously* (on another
           thread).  So, we wait a while or, in this case, forever.  Note
           that, depending on implementation, this might not be necessary.
           It may be enough to have an active LookupDiscovery instance if it
           uses normal threads.  However, we assume it uses all daemon threads.
         */
        try {
            Object myLock = new Object();

            synchronized(myLock) {
                myLock.wait(0);
            }
        } catch (InterruptedException anIE) {
            System.err.println("Whoops main thread interrupted");
        }
    }

    private static class DiscoveryListenerImpl implements DiscoveryListener {
        public void discarded(DiscoveryEvent anEvent) {
            // We don't care about these
        }

        public void discovered(DiscoveryEvent anEvent) {
            ServiceRegistrar[] myRegs = anEvent.getRegistrars();

            for (int i = 0; i < myRegs.length; i++) {

                try {
                    ServiceRegistrar myRegistrar = myRegs[i];

                    if (myRegistrar != null) {
                        System.out.println("Found registrar:");

                        DiscoveryUtil.dumpRegistrar(myRegistrar);

                        DiscoveryUtil.dumpContents(myRegistrar);
                    }

                } catch (RemoteException aRE) {
                    System.err.println("Couldn't talk to ServiceRegistrar");
                    aRE.printStackTrace(System.err);
                } catch (IOException anIOE) {
                    System.err.println("Whoops couldn't talk to ServiceRegistrar");
                    anIOE.printStackTrace(System.err);
                }
            }
        }
    }
}
