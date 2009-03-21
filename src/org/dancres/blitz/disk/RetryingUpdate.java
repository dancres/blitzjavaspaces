package org.dancres.blitz.disk;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.LockNotGrantedException;

import org.dancres.blitz.Logging;

public class RetryingUpdate {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.RetryingUpdate");

    private RetryableOperation theOp;

    public RetryingUpdate(RetryableOperation anOp) {
        theOp = anOp;
    }

    public Object commit() throws IOException {
        int myRetryCount = 0;

        do {
            DiskTxn myTxn = DiskTxn.newNonBlockingStandalone();

            try {
                Object myResult = theOp.perform(myTxn);

                myTxn.commit();

                if (myRetryCount != 0) {
                    if (theLogger.isLoggable(Level.FINE))
                        theLogger.log(Level.FINE,
                                      "Total retries: " + myRetryCount);
                }

                return myResult;
            } catch (DatabaseException aDbe) {
                if ((aDbe instanceof DeadlockException) ||
                    (aDbe instanceof LockNotGrantedException)) {

                    if (theLogger.isLoggable(Level.FINEST))
                        theLogger.log(Level.FINEST, "Got lock exception", aDbe);

                    myTxn.abort();

                    
                    // System.err.println("Aborting" + theOp + " retry: " +
                    //                    myRetryCount);

                    ++myRetryCount;

                    BackoffGenerator.pause();

                } else {
                    theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
                    throw new IOException("Dbe");
                }
            }
        } while (true);
    }
}