package org.dancres.blitz.tools;

import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

import net.jini.admin.Administrable;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.StatsAdmin;
import org.dancres.blitz.stats.Stat;
import org.dancres.jini.util.DiscoveryUtil;
import org.dancres.jini.util.ServiceLocator;

/**
   <p>MonitorStats accepts a spacename as an argument and a loop time.  It then
   attempts to list all available statistics on the space every loop time
   seconds.  Specifying a loop time of 0 will cause MonitorStats to dump
   stats once and exit.</p>

   <p>Typical usage:

   <pre>
   java -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/jini/jini2_0/lib/jsk-platform.jar:/home/dan/src/jini/space/build:/home/dan/jini/jini2_0/lib/jini-ext.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.MonitorStats dancres 20
   </pre>
   
   @see org.dancres.blitz.remote.BlitzAdmin
 */
public class MonitorStats {
    private static final long MAX_DISCOVER_TIME = 15 * 1000;

    public static void main(String args[]) {
        new MonitorStats().startup(args);
    }

    void startup(String args[]) {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        try {
            try {
                long myTimeout = 20 * 1000;

                Object myProxy = null;

                if (args.length == 2) {
                    myProxy = ServiceLocator.getService(JavaSpace.class,
                                                        args[0],
                                                        MAX_DISCOVER_TIME);
                    myTimeout = Integer.parseInt(args[1]) * 1000;
                } else if (args.length == 3) {
                    myProxy = ServiceLocator.getService(args[0],
                                                        JavaSpace.class,
                                                        args[1]);
                    myTimeout = Integer.parseInt(args[2]) * 1000;
                } else {
                    System.err.println(
                        "Wrong number of arguments - should be <spacename> <seconds> or <LUS host> <spacename> <seconds>");
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
                                                       StatsAdmin.class)) {
                            StatsAdmin myStatsAdmin = 
                                (StatsAdmin) myAdminProxy;

                            getWatcher(myStatsAdmin, myTimeout).run();
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

    Runnable getWatcher(StatsAdmin anAdmin, long aTimeout) {
        return new Watcher(anAdmin, aTimeout);
    }

    static class Watcher implements Runnable {
        private StatsAdmin theAdmin;
        private long theTimeout;

        Watcher(StatsAdmin anAdmin, long aTimeout) {
            theAdmin = anAdmin;
            theTimeout = aTimeout;
        }

        public void run() {
            do {
                try {
                    Stat[] myStats = theAdmin.getStats();

                    System.out.println("Snapshot: " +
                                       System.currentTimeMillis());
                    for (int i = 0; i < myStats.length; i++) {
                        System.out.println(myStats[i]);
                    }

                    System.out.println();

                    Thread.sleep(theTimeout);
                } catch (Exception anE) {
                    System.err.println(anE);
                }
            } while (theTimeout > 0);  // if timeout == 0, only run once
        }
    }
}
