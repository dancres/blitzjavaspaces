package org.dancres.blitz.disk;

import java.util.concurrent.*;
import java.util.logging.*;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Logging;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.task.Task;

import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.IOStat;

/**
   <p> Certain elements of Blitz (such as ArcCache) require all writes to be
   asynchronous to avoid deadlock situations and reduce time spent waiting for
   I/O completion by a client thread.  WriteDaemon provides the asynchronous
   I/O infrastructure to satisfy such requirements. </p>

   <p> We stop when Disk is stop'd which allows for proper sync'ing </p>

   <p>We can only have one thread for writing at this moment because:
   <ol>
   <li>WriteBuffer assumes serialized updating of images.  If two threads
   start performing jobs on the same UID, then there's currently scope for
   a collision.  The problem is compounded by the fact that we can't simply
   requeue the job because that will break checkpointing/sync'ing</li>
   <li>Sync'ing assumes that there's only one thread which will execute the
   appropriate callback placed in the queue once all prior Jobs have been
   dispatched.  This ordering dependency is what complicates the WriteBuffer
   issue above.</li>
   <li>There's an ordering issue - we must write before we decide to then
   delete something that is on disk.</li>
   </ol>
   </p>

   @see org.dancres.blitz.arc.ArcCache
*/
public class WriteDaemon implements StatGenerator {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.WriteDaemon");

    private static int MAX_WRITE_THREADS;
    private static int THREAD_KEEPALIVE;
    private static int DESIRED_PENDING_WRITES;
    private static int THROTTLE_PENDING_WRITES;
    private static long THROTTLE_PAUSE;

    static {
        try {
            MAX_WRITE_THREADS = ((Integer)
                ConfigurationFactory.getEntry("maxWriteThreads", 
                                              int.class,
                                              new Integer(5))).intValue();
            THREAD_KEEPALIVE = ((Integer)
                ConfigurationFactory.getEntry("threadKeepalive", 
                                              int.class,
                                              new Integer(60000))).intValue();
            DESIRED_PENDING_WRITES = ((Integer)
                ConfigurationFactory.getEntry("desiredPendingWrites", 
                                              int.class,
                                              new Integer(20))).intValue();

            THROTTLE_PENDING_WRITES = ((Integer)
                ConfigurationFactory.getEntry("throttlePendingWrites", 
                                              int.class,
                                              new Integer(Integer.MAX_VALUE))).intValue();
            THROTTLE_PAUSE = ((Long)
                ConfigurationFactory.getEntry("throttlePause", 
                                              long.class,
                                              new Long(50))).longValue();
        } catch (ConfigurationException aCE) {
        }
    }

    private static WriteDaemon theDaemon;

    private int thePendingCount;
    private LinkedBlockingQueue thePendingUpdates = new LinkedBlockingQueue();

    private ExecutorService theWriters;
    private ExecutorService theCompleters;

    private IOStats theIOStats = new IOStats();

    private long theStatId = StatGenerator.UNSET_ID;
    private int theThrottleCount = 0;

    private WriteDaemon() {
        theWriters = Executors.newFixedThreadPool(MAX_WRITE_THREADS);
        theCompleters = Executors.newFixedThreadPool(1);

        theLogger.log(Level.INFO, "Async keepalive: " + THREAD_KEEPALIVE);
        theLogger.log(Level.INFO, "Pending write size: " +
                      DESIRED_PENDING_WRITES);
        theLogger.log(Level.INFO, "Throttle write size: " + 
                      THROTTLE_PENDING_WRITES);
        theLogger.log(Level.INFO, "Throttle pause: " + 
                      THROTTLE_PAUSE);

        StatsBoard.get().add(this);
    }

    public static void init() {
        theDaemon = new WriteDaemon();
    }

    public static WriteDaemon get() {
        return theDaemon;
    }

    /**
       Queue a task for execution by the WriteDaemon thread.
     */
    public void queue(Runnable anUpdate) {

        // If the write queue is getting too large, start stalling
        if (theIOStats.getQueueSize() > THROTTLE_PENDING_WRITES) {
            ++theThrottleCount;

            theLogger.log(Level.WARNING,
                "Write queue overflowing - THROTTLING");

            try {
                Thread.sleep(THROTTLE_PAUSE);
            } catch (InterruptedException anIE) {
                theLogger.log(Level.SEVERE, "Throttle broken!");
            }
        }

        synchronized(this) {
            try {
                thePendingUpdates.put(new OutputTracker(anUpdate));
                ++thePendingCount;

                if (thePendingCount >= DESIRED_PENDING_WRITES) {
                    pushImpl();
                }
            } catch (InterruptedException anIE) {
                theLogger.log(Level.SEVERE, "Failed to queue update", anIE);
            }
        }

        theIOStats.incAsyncInCount();
    }

    /**
       Should only be called from within a sync block.
     */
    private void pushImpl() {
        Object myTask;

        try {
            while ((myTask = thePendingUpdates.poll(0, TimeUnit.MILLISECONDS)) != null) {
                theWriters.execute((Runnable) myTask);
            }
        } catch (InterruptedException anIE) {
        }
        
        thePendingCount = 0;
    }

    /**
       Force the queue to be processed
     */
    void push() {
        synchronized(this) {
            pushImpl();
        }
    }

    /**
       <p>Force the updates in the queue to disk.  On completion, invoke the
       passed task.  Note this task is processed asynchronously outside
       of the WriteDaemon thread.  This allows the WriteDaemon to continue
       processing updates whilst the completion task runs.</p>

       <p>If you want a task to be performed synchronously by the WriteDaemon
       after other requests, <code>queue</code> the task and invoke
       <code>push</code>.</p>
     */
    void push(Task aCompletionTask) {
        if (aCompletionTask == null)
            throw new IllegalArgumentException();
        else {
            synchronized(this) {
                /*
                  We ensure the passed task doesn't execute until the
                  queue has been emptied by putting a barrier task at the end 
                  of the queue.  Thus, when we force the queue, this barrier
                  will only execute once all preceeding updates have been
                  performed.
                 */
                queue(new Scheduler(aCompletionTask));
                pushImpl();
            }
        }
    }

    void halt() {
        theLogger.log(Level.INFO, "WriteDaemon doing halt");
        theWriters.shutdown();
        theCompleters.shutdown();
        theLogger.log(Level.INFO, "WriteDaemon done halt");
    }

    /**
       We do not run the completion task in-line.  This is potentially time
       consuming and we have more important things to do (write updates) so
       we palm the completion task off to a separate thread pool when we've
       done the necessary work.
     */
    private class Scheduler implements Runnable {
        private Task theTask;

        Scheduler(Task aCompletionTask) {
            theTask = aCompletionTask;
        }

        public void run() {
            theCompleters.execute(theTask);
        }
    }

    private class OutputTracker implements Runnable {
        private Runnable theWrite;

        OutputTracker(Runnable aRunnable) {
            theWrite = aRunnable;
        }

        public void run() {
            theWrite.run();

            theIOStats.incAsyncOutCount();
        }
    }

    /**
       @return the id of the StatGenerator that produced the stat
       AdministrableStat.UNSET_ID if the id has never been set
     */
    public long getId() {
        return theStatId;
    }

    public void setId(long anId) {
        theStatId = anId;
    }

    public Stat generate() {
        return new IOStat(theStatId, theIOStats.getTimePerIn(),
            theIOStats.getTimePerOut(), theIOStats.getInOutRatio(),
            theIOStats.getQueueSize(), theThrottleCount);
    }
}
