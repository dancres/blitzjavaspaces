package org.dancres.blitz.disk;

import java.util.logging.Level;

import com.sleepycat.je.Environment;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.CheckpointConfig;

import org.dancres.blitz.task.Task;

/**
   <p>We pass one of these to the WriteDaemon which calls back on it once
   it has "forced" all write requests lodged before the call to force.  This
   ensures we checkpoint Db having flushed the relevant updates.</p>

   <p>Optionally, the SyncFinalizer can callback a user-defined
   task to perform additional work post sync.  If this mechanism is used, the
   thread which calls <code>waitForCompletion()</code> will not be blocked
   because all work will be done asynchronously.  Otherwise, the calling thread
   is blocked until completion.</p>
 */
class SyncFinalizer implements Task {
    private boolean isDone = false;
    private Runnable theCompletionTask;
    private Environment theEnv;

    SyncFinalizer(Environment anEnv, Runnable aCompletionTask) {
        theEnv = anEnv;
        theCompletionTask = aCompletionTask;
    }

    public void run() {
        try {
            theEnv.sync();

            CheckpointConfig myConfig = new CheckpointConfig();
            myConfig.setForce(true);
            theEnv.checkpoint(myConfig);

        } catch (DatabaseException aDbe) {
            WriteDaemon.theLogger.log(Level.SEVERE,
                                      "Warning, failed to checkpoint", aDbe);
        }

        if (theCompletionTask != null)
            theCompletionTask.run();
        else 
            synchronized(this) {
                isDone = true;
                notify();
            }
    }

    /**
       Only blocks when there's no completion task.  i.e. The completion task
       is assumed to imply that the caller requires asynchronous completion
       and does not wish to block.
     */
    void waitForCompletion() {
        // No completion task means block.
        //
        if (theCompletionTask == null) {
            long myStart = System.currentTimeMillis();

            synchronized(this) {
                while (!isDone) {
                    try {
                        wait();
                    } catch (InterruptedException anIE) {
                    }
                }
            }

            long myEnd = System.currentTimeMillis();
            WriteDaemon.theLogger.log(Level.FINE,
                                      "Queue blocked for: " +
                                      (myEnd - myStart));
        }
    }
}
