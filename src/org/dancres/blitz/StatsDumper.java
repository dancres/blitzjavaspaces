package org.dancres.blitz;

import java.net.*;

import java.util.logging.Level;

import org.dancres.blitz.disk.Disk;

import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.Stat;

/**
   <p>This class implements a remote access feature which allows a programmer
   to trigger dumping of useful debug information.  It is triggered by
   <code>telnet</code>'ing to the debug port. StatsDumper detects the connection,
   closes it and dumps relevant output to console.</p>

   <p>For example, if Blitz were suffering from a deadlock, one could arrange
   for a connection on the debug port, to dump lock information, the default
   implementation (note that it requires a tweak to Db for it to provide the
   right information, contact dan@dancres.org, for information).</p>

   <p>If the port number is non-zero, the StatsDumper is enabled otherwise it's
   disabled.  See <code>debugPort</code> in the configuration file.</p>

   @todo Find a nice way to handle the stack tracing stuff - only works on
   JDK 1.5
 */
class StatsDumper implements Runnable, ActiveObject {

    private static StatsDumper theStatsDumper;

    private Thread theDebugThread;
    private boolean stopDebugging;
    private long theCycleTime;
    
    static void start(long aPause) {
        try {
            if (aPause == 0)
                return;

            System.err.println("Starting Stats Dumper: " + aPause);

            theStatsDumper = new StatsDumper(aPause);
        } catch (Exception anE) {
            System.err.println("StatsDumper didn't start!");
            anE.printStackTrace(System.err);
        }
    }

    private StatsDumper(long aPause) throws Exception {
        theCycleTime = aPause;
        ActiveObjectRegistry.add(this);
    }

    public void begin() {
        stopDebugging = false;
        theDebugThread = new Thread(this, "StatsDumper");
        theDebugThread.start();
    }

    public void halt() {
        synchronized(this) {
            stopDebugging = true;
        }

        theDebugThread.interrupt();

        try {
            theDebugThread.join();
            theDebugThread = null;
        } catch (InterruptedException anIE) {
            SpaceImpl.theLogger.log(Level.SEVERE,
                                    "Failed to wait for StatsDumper",
                                    anIE);
        }
    }

    public void run() {
        synchronized(this) {            
            while(!stopDebugging) {
                try {
                    Stat[] myStats = StatsBoard.get().getStats();
                    for (int i = 0; i < myStats.length; i++) {
                        System.err.println(myStats[i]);
                    }

                    wait(theCycleTime);
                    
                    if (stopDebugging) {
                        continue;
                    }
                    
                /*
                   Only works on JDK 1.5!!!!!!!!
                 
                 
                Map myTraces = Thread.getAllStackTraces();
                 
                Iterator myThreads = myTraces.keySet().iterator();
                while (myThreads.hasNext()) {
                    Thread myThread = (Thread) myThreads.next();
                    StackTraceElement[] myTrace =
                        (StackTraceElement[]) myTraces.get(myThread);
                 
                    System.err.println();
                    System.err.println(myThread);
                    System.err.println();
                    for (int i = 0; i < myTrace.length; i++) {
                        System.err.println("       " + myTrace[i]);
                    }
                }
                 
                 */
                } catch (InterruptedException anIE) {
                    SpaceImpl.theLogger.log(Level.SEVERE, "StatsDumper interrupted");
                } catch (Exception anE) {
                    System.err.println("Whoops couldn't do dump stats");
                }
            }
        }
    }
}
