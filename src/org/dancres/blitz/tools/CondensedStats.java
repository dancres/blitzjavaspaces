package org.dancres.blitz.tools;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;

import org.dancres.blitz.remote.StatsAdmin;
import org.dancres.blitz.stats.BlockingOpsStat;
import org.dancres.blitz.stats.EventQueueStat;
import org.dancres.blitz.stats.IOStat;
import org.dancres.blitz.stats.InstanceCount;
import org.dancres.blitz.stats.MemoryStat;
import org.dancres.blitz.stats.MissedOpsStat;
import org.dancres.blitz.stats.OpStat;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.TaskQueueStat;
import org.dancres.blitz.stats.ThreadStat;
import org.dancres.blitz.stats.TxnStat;

/**
   <p>CondensedStats accepts a spacename as an argument and a loop time.  It
   then regularly prints a condensed view of some important space stats every
   loop time seconds.  Specifying a loop time of 0 will cause MonitorStats to
   dump stats once and exit.</p>

   <p>Typical usage:

   <pre>
   java -Xmx256m -Djava.security.policy=config/policy.all
     -classpath /home/dan/jini/jini2_1/lib/jsk-platform.jar:/home/dan/src/jini/space/lib/blitz.jar:/home/dan/jini/jini2_1/lib/jsk-lib.jar:/home/dan/jini/jini2_0/lib/sun-util.jar
     org.dancres.blitz.tools.CondensedStats dancres 20
   </pre>
   
   @see org.dancres.blitz.remote.BlitzAdmin
 */
public class CondensedStats extends MonitorStats {
    public static void main(String args[]) {
        new CondensedStats().startup(args);
    }

    @Override
    Runnable getWatcher(StatsAdmin anAdmin, long aTimeout)
    {
        return new CondensedWatcher(anAdmin, aTimeout);
    }

    /**
     * Provides a vmstat-style one line repeating summary of Blitz statistics.
     * Includes operation counts, active txns/ops, listeners and queue stats,
     * writer I/O and VM stats.
     */
    static class CondensedWatcher implements Runnable {
        private StatsAdmin theAdmin;
        private long theTimeout;
        private Stat[] theLastStats;

        CondensedWatcher(StatsAdmin anAdmin, long aTimeout) {
            theAdmin = anAdmin;
            theTimeout = aTimeout;
        }

        public void run() {
            boolean run = true;
            while (run) {
                try {
                    // if timeout is 0, run once over a 1 second period
                    if (theTimeout == 0) {
                        run = false;
                        theTimeout = 1000;
                    }
                    
                    printHeader(System.out);
                    
                    theLastStats = theAdmin.getStats();
                    
                    for (int i = 0;; i++) {
                        Thread.sleep(theTimeout);
                        
                        print(System.out, (int)theTimeout / 1000);
                        if ((i + 1) % 48 == 0)
                            printHeader(System.out);
                    }
                } catch (Exception anE) {
                    System.err.println(anE);
                    anE.printStackTrace();
                }
            }
        }

        public void printHeader(PrintStream out) {
            out.println("-----ops----  " +
                        " missed   " +
                        "obj  " +
                        "---active--  " +
                        "listeners  " +
                        "---queues--  " +
                        "------io-----  " +
                        "-----vm-----");
            
            out.println("   r   t   w  " +
                        "  r   t   " +
                        "+/-  " +
                        "txn   r   t  " +
                        "  tr   pt  " +
                        "evt rem oth  " +
                        " i/o  qsz thr  " +
                        "thrd mem max");
        }
        
        public void print(PrintStream out, int interval) throws RemoteException {
            Stat[] last = theLastStats;
            Stat[] stats = theAdmin.getStats();
            
            // Operation count since last iteration
            long opReads  = getOpCount(stats, OpStat.READS) -
                            getOpCount(last, OpStat.READS);
            long opTakes  = getOpCount(stats, OpStat.TAKES) -
                            getOpCount(last, OpStat.TAKES);
            long opWrites = (getOpCount(stats, OpStat.WRITES) -
                             getOpCount(last, OpStat.WRITES)) / interval;
            
            // Missed operations, not available until first operation
            long missReads = 0, missTakes = 0;
            MissedOpsStat missedStat = getStatistic(stats, MissedOpsStat.class);
            MissedOpsStat missedLast = getStatistic(stats, MissedOpsStat.class);
            
            if (missedStat != null && missedLast != null) {
                missReads = (missedStat.getMissedReads() - missedLast.getMissedReads())
                                 / interval;
                missTakes = (missedStat.getMissedTakes() - missedLast.getMissedTakes())
                                 / interval;
            }
            
            // Change op counts to include misses
            opReads = (opReads + missReads) / interval;
            opTakes = (opTakes + missTakes) / interval;
            
            // Overall instance count
            long instances = (getInstanceCount(stats) - getInstanceCount(last))
                                  / interval;
            
            // Active operations/txns
            int activeTxn = getStatistic(stats, TxnStat.class).getActiveTxnCount();
            
            // Not available until after first space operation
            int activeRead = 0, activeTake = 0;
            BlockingOpsStat blockedOps = getStatistic(stats, BlockingOpsStat.class);
            if (blockedOps != null) {
                activeRead = blockedOps.getReaders();
                activeTake = blockedOps.getTakers();
            }
            
            // Notify listeners
            int listenTrans = getStatistic(stats, EventQueueStat.class).getTransientCount();
            int listenPers = getStatistic(stats, EventQueueStat.class).getPersistentCount();
            
            // Queues, internal notify queue, remote notify queue and other tasks
            int queueEvents = getTaskQueue(stats, "Events");
            int queueRemote = getTaskQueue(stats, "RemoteEvent");
            int queueOther = getTaskQueue(stats, "DefaultTask");
            
            // Writer I/O if persistent storage model in use
            double ioRatio = 0;
            int ioQueueSize = 0, ioThrottle = 0;
            
            IOStat myIoStat = getStatistic(stats, IOStat.class);
            if (myIoStat != null) {
                ioRatio = myIoStat.getInOutRatio();
                ioQueueSize = getStatistic(stats, IOStat.class).getQueueSize();
                ioThrottle = (getStatistic(stats, IOStat.class).getThrottleCount() -
                              getStatistic(last, IOStat.class).getThrottleCount())
                                  / interval;
            }
            
            // Virtual machine
            int vmThreads = getStatistic(stats, ThreadStat.class).getThreadCount();
            
            double vmMemory = getStatistic(stats, MemoryStat.class).getCurrentMemory();
            vmMemory = vmMemory / 1024 / 1024 / 1024;
            
            double vmMaxMem = getStatistic(stats, MemoryStat.class).getMaxMemory();
            vmMaxMem = vmMaxMem / 1024 / 1024 / 1024;
            
            //                       r   t   w$  r   w$ +/-$txn  r   t$ tr  pt
            String formatString = "%4d %3d %3d %4d %3d %5d %4d %3d %3d %5d %4d ";
            //                     evt rem oth$i/o   qsz thr$thrd mem  max
            formatString       += "%4d %3d %3d %5.1f %4d %3d %5d %3.1f %3.1f";
            
            String line = new Formatter().format(formatString,
                                                 opReads, opTakes, opWrites,
                                                 missReads, missTakes, instances,
                                                 activeTxn, activeRead, activeTake,
                                                 listenTrans, listenPers,
                                                 queueEvents, queueRemote, queueOther,
                                                 ioRatio, ioQueueSize, ioThrottle,
                                                 vmThreads, vmMemory, vmMaxMem).toString();
            
            out.println(line);
            
            theLastStats = stats;
        }
        
        private <T extends Stat> Collection<T> getStatistics(Stat[] allStats,
                                                             Class<T> statClazz) {
            Collection<T> ret = new ArrayList<T>();
            for (Stat stat : allStats)
                if (statClazz.isInstance(stat))
                    ret.add(statClazz.cast(stat));
            
            return ret;
        }
        
        private <T extends Stat> T getStatistic(Stat[] allStats,
                                                Class<T> statClazz) {
            Collection<T> subset = getStatistics(allStats, statClazz);
            return (subset.isEmpty()) ? null : subset.iterator().next();
        }
        
        /** Adds up operations across all entry types.
         * @param opType OpStat.TAKES/READS/WRITES
         */
        private long getOpCount(Stat[] stats, int opType) {
            long count = 0;
            for (OpStat ops : getStatistics(stats, OpStat.class))
                if (ops.getOp() == opType)
                    count += ops.getCount();
            
            return count;
        }
        
        /** Adds up instance counts across all entry types. */
        private long getInstanceCount(Stat[] stats) {
            long count = 0;
            for (InstanceCount ops : getStatistics(stats, InstanceCount.class))
                count += ops.getCount();
            
            return count;
        }
        
        /** Gets size of a task queue with a particular task name */
        private int getTaskQueue(Stat[] stats, String taskName) {
            for (TaskQueueStat ops : getStatistics(stats, TaskQueueStat.class))
                if (taskName.equals(ops.getQueueName()))
                    return ops.getQueueSize();
            
            return 0;
        }
    }
}
