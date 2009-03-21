package stats;

import java.rmi.RMISecurityManager;

import java.net.InetAddress;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.admin.Administrable;

import org.dancres.blitz.remote.StatsAdmin;

import org.dancres.blitz.stats.*;

/**
   A Blitz specific example.  This client locates a JavaSpace and checks
   for the Blitz admin interfaces.  If the JavaSpace has these interfaces
   the program proceeds to recover the currently active statistics,
   activates some additional statistics, generates some activity (via a
   Write to the space) and then prints out the new statistics.
 */
public class TestClient {
    public static void main(String args[]) {
        try {
            /*
              Make sure we've installed the RMI security manager otherwise we
              won't be able to download code.
             */
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            /*
              Locates an already running service - WILL NOT notice Space
              instances run after Lookup is started.
             */
            Lookup myFinder = new Lookup(JavaSpace.class);

            /*
              Locate JavaSpace service.
             */
            JavaSpace mySpace = (JavaSpace) myFinder.getService();

            /*
              Blitz is Administrable so if the current proxy isn't we haven't
              found a Blitz instance
             */
            if (! (mySpace instanceof Administrable)) {
                System.out.println("Whoops, this space isn't administrable");
                return;
            }

            Object myAdmin = ((Administrable) mySpace).getAdmin();

            /*
              If the AdminProxy doesn't have a StatsAdmin it's not Blitz.
             */
            if (! (myAdmin instanceof StatsAdmin)) {
                System.out.println("Whoops, this space hasn't got a StatsAdmin - can't be Blitz");
                return;
            }

            /*
              Stats access is via StatsAdmin on the Admin proxy.
             */
            StatsAdmin myStatsAdmin = (StatsAdmin) myAdmin;

            /*
              Display current stats
             */
            Stat[] myStats = myStatsAdmin.getStats();

            System.out.println("Stats are currently:");
            dumpStats(myStats);

            /*
              Enable tracking of takes and writes for TestEntry
              (we could enable tracking for all types)
             */
            OpSwitch myOpSwitch1 = new OpSwitch(TestEntry.class.getName(),
                                               OpSwitch.WRITE_OPS, true);
            OpSwitch myOpSwitch2 = new OpSwitch(TestEntry.class.getName(),
                                               OpSwitch.TAKE_OPS, true);

            /*
              Enable instance counts for all types (could specify
              particular types if we wish).
             */
            InstanceSwitch myInstSwitch = 
                new InstanceSwitch(InstanceSwitch.ALL_TYPES, true);

            Switch[] mySwitches = new Switch[3];
            mySwitches[0] = myOpSwitch1;
            mySwitches[1] = myOpSwitch2;
            mySwitches[2] = myInstSwitch;

            myStatsAdmin.setSwitches(mySwitches);


            System.out.println();
            System.out.println("Set switches, doing write");
            System.out.println();
            
            /*
              Write a TestEntry
             */
            mySpace.write(new TestEntry("blah"), null, Lease.FOREVER);

            myStats = myStatsAdmin.getStats();

            /*
              Does it appear in the stats?
             */
            System.out.println("Stats are now:");
            dumpStats(myStats);

        } catch (Exception aRE) {
            System.err.println("Ooops");
            aRE.printStackTrace(System.err);
        }
    }

    public static class TestEntry implements Entry {
        public String theName;

        public TestEntry(String aName) {
            theName = aName;
        }
    }

    private static void dumpStats(Stat[] aStats) {
        for (int i = 0; i < aStats.length; i++) {

            if (aStats[i] instanceof InstanceCount) {
                InstanceCount myCount = (InstanceCount) aStats[i];
                System.out.println("Instances of type " +
                                   myCount.getType() + " = " +
                                   myCount.getCount());

            } else if (aStats[i] instanceof TxnStat) {
                TxnStat myTxns = (TxnStat) aStats[i];
                System.out.println("Total active txns: " +
                                   myTxns.getActiveTxnCount());

            } else if (aStats[i] instanceof OpStat) {
                OpStat myOp = (OpStat) aStats[i];
                System.out.println("Total " + myOp.getOpTypeAsString() +
                                   " on " + myOp.getType() + " = " +
                                   myOp.getCount());

            } else if (aStats[i] instanceof MemoryStat) {
                MemoryStat myMem = (MemoryStat) aStats[i];
                System.out.println("Memory used: " +
                                   myMem.getCurrentMemory() + " out of " +
                                   myMem.getMaxMemory());
            } else if (aStats[i] instanceof BlockingOpsStat) {
                BlockingOpsStat myBlockers = (BlockingOpsStat) aStats[i];
                System.out.println("Blocking reads: " +
                                   myBlockers.getReaders() + " takes: " +
                                   myBlockers.getTakers());
            } else if (aStats[i] instanceof HostStat) {
                HostStat myHostStat = (HostStat) aStats[i];

                System.out.println("Default host address: " +
                                   myHostStat.getHostAddr());
                InetAddress[] myIfAddrs = myHostStat.getAllAddr();

                System.out.println("All known interface addresses");
                for (int j = 0; j < myIfAddrs.length; j++) {
                    System.out.println(myIfAddrs[j]);
                }
            } else
                System.out.println(aStats[i]);
        }
    }
}
