package org.dancres.blitz.task;

import java.util.Iterator;

import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.ActiveObject;
import org.dancres.blitz.ActiveObjectRegistry;
import org.dancres.blitz.Logging;
import org.dancres.blitz.stats.StatsBoard;

import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.util.QueueStatGenerator;

/**
   Various operations within the space must be handled in background.
   This class encapsulates a pool of threads which execute operations
   (Task instances) as they are queued. <P>

   It might, at first, appear to make sense to have separate pools for
   event notification, blocked call wakeups etc. in the belief that one can
   better control the balance of dispatch of, for example, notifies in
   comparison with searches.  <P>

   In reality, this won't work as each pool has a set of threads all with the
   same priority.  i.e.  They share whatever CPU is available in a manner
   driven by the number of tasks they must perform.  Thus, if one wishes
   to truly balance, say, notification rate against search wakeups, one
   must assign differing priorities to these <I>tasks</I> as opposed to
   <I>threads</I> to ensure CPU consumption is bounded and that, whichever
   tasks have priority, get to use the CPU first.
 */
public class Tasks implements ActiveObject {
    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.task.Tasks");
    
    private static final String DEFAULT_QUEUE = "DefaultTask";

    private static Tasks theTasks = new Tasks();

    private static int MAX_TASK_THREADS;

    private static int TASK_QUEUE_BOUND;

    static {
        try {
            MAX_TASK_THREADS = ((Integer)
                ConfigurationFactory.getEntry("maxTaskThreads", 
                                              int.class,
                                              new Integer(10))).intValue();
            TASK_QUEUE_BOUND = ((Integer)
                ConfigurationFactory.getEntry("taskQueueBound", 
                                              int.class,
                                              new Integer(0))).intValue();

            theLogger.log(Level.INFO, "Maximum task threads: " +
                          MAX_TASK_THREADS);
            theLogger.log(Level.INFO, "Task queue bound: " +
                          TASK_QUEUE_BOUND);

        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Failed to load config", aCE);
        }
    }

    private ConcurrentMap<String, ExecutorService> theExecutors = new ConcurrentHashMap();

    private Tasks() {
        ActiveObjectRegistry.add(this);
    }

    public static void queue(Task aTask) throws InterruptedException {
        queue(DEFAULT_QUEUE, aTask);
    }

    public static void queue(String aQueue, 
                             Task aTask) throws InterruptedException {
        theTasks.execute(aQueue, aTask);
    }

    private void execute(String aQueue, Task aTask)
        throws InterruptedException {

        getExecutor(aQueue).execute(aTask);
    }

    public void begin() {
    }

    public void halt() {
        for (ExecutorService e: theExecutors.values()) {
            e.shutdownNow();
        }

        theExecutors.clear();
    }

    private ExecutorService getExecutor(String aName) {
        ExecutorService myExec = theExecutors.get(aName);

        if (myExec == null) {

            LinkedBlockingQueue myQueue;

            if (TASK_QUEUE_BOUND == 0) {
                theLogger.log(Level.INFO,
                        "Creating task pool with no bounds [ " + aName + " ]");


                myQueue = new LinkedBlockingQueue(Integer.MAX_VALUE);
                myExec = new ThreadPoolExecutor(MAX_TASK_THREADS, MAX_TASK_THREADS, 0, TimeUnit.MILLISECONDS,
                        myQueue);
            } else {
                theLogger.log(Level.INFO,
                        "Creating task pool with bounds: " +
                                TASK_QUEUE_BOUND);

                myQueue = new LinkedBlockingQueue(TASK_QUEUE_BOUND);
                myExec = new ThreadPoolExecutor(MAX_TASK_THREADS, MAX_TASK_THREADS, 0, TimeUnit.MILLISECONDS,
                        myQueue);

            }

            ExecutorService myCollision = theExecutors.putIfAbsent(aName, myExec);
            if (myCollision == null)
                StatsBoard.get().add(new QueueStatGenerator(aName, myQueue));
            else {
                myExec.shutdownNow();
                myExec = myCollision;
            }
        }

        return myExec;
    }
}
