package org.dancres.blitz.txn.batch;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.prevayler.Prevayler;
import org.prevayler.PrevalentSystem;
import org.prevayler.Command;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.Snapshotter;

import org.dancres.blitz.Logging;
import org.prevayler.implementation.PrevaylerCore;

/**
   <p>Batches commands issued in the same time period together in an attempt
   to improve log throughput via Prevayler.</p>

   <p>Batch-writing can reduce the number of forced flushes to disk whilst
   increasing the amount of data written with each flush.  This has a
   positive effect on throughput under concurrent load.</p>
   
   <p>Time is specified in ms, first thread into barrier waits this amount
   of time for other writers.  Other writers entering the barrier are
   now blocked until the first entrant commits all writes to log.  When first
   thread awakes, all writes are done as a group followed by a single sync.</p>
 */
public class WriteBatcher implements SnapshotPrevayler {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.txn.LogBatcher");

    private PrevaylerCore thePrevayler;

    private boolean amFirst = true;

    private ArrayList theWrites = new ArrayList();

    // Might be able to buff up to 60ms which gives average of 30 but
    // we'll see.
    private long theWindowTimeMs = 0;
    private int theWindowTimeNs = 0;

    public WriteBatcher(PrevaylerCore aPrevayler, long aWindowTimeMs, int aWindowTimeNs) {
        thePrevayler = aPrevayler;
        theWindowTimeMs = aWindowTimeMs;
        theWindowTimeNs = aWindowTimeNs;
    }

    /**
     * Returns the underlying PrevalentSystem.
     */
    public PrevalentSystem system() {
        return thePrevayler.system();
    }

    public Serializable executeCommand(Command aCommand) throws Exception {
        return write(aCommand);
    }

    public Serializable executeCommand(Command aCommand, boolean sync)
        throws Exception {
        return write(aCommand);
    }

    private Serializable write(Command aComm) throws Exception {
        WriteRequest myReq = new WriteRequest(system(), aComm);

        synchronized(this) {
            if (amFirst) {
                theWrites.add(myReq);
                amFirst = false;

                try {
                    wait(theWindowTimeMs, theWindowTimeNs);
                } catch (InterruptedException anIE) {
                }

                flushAll();
                amFirst = true;

            } else {
                theWrites.add(myReq);
            }
        }

        return myReq.getResult();
    }

	public Snapshotter takeSnapshot() throws IOException {
        return thePrevayler.takeSnapshot();
    }

    private void flushAll() {

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Flushing " +
                          theWrites.size() + " to log");

        int myLast = theWrites.size() - 1;

        // Do all writes, last with sync
        for (int i = 0; i < theWrites.size(); i++) {
            WriteRequest myReq = (WriteRequest) theWrites.get(i);

            myReq.execute(thePrevayler, (i == myLast));                
        }

        theWrites.clear();
    }

    private static class WriteRequest {
        private Command theCommand;
        private PrevalentSystem theSystem;

        private Exception theException;

        private boolean isDone;

        WriteRequest(PrevalentSystem aSystem, Command aCommand) {
            theCommand = aCommand;
            theSystem = aSystem;
        }

        void execute(PrevaylerCore aPrev, boolean doSync) {
            try {
                aPrev.logCommand(theCommand, doSync);
            } catch (Exception anE) {
                theException = anE;
            } finally {
                synchronized(this) {
                    isDone = true;
                    notify();
                }
            }
        }

        Serializable getResult() throws Exception{
            synchronized(this) {
                while (!isDone) {
                    try {
                        wait();
                    } catch (InterruptedException anIE) {
                    }
                }

                if (theException != null)
                    throw theException;
                else
                    return theCommand.execute(theSystem);
            }
        }
    }
}
