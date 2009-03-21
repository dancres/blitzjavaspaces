package org.dancres.blitz.txn;

import java.io.IOException;

import java.util.logging.Level;

import org.dancres.blitz.task.Task;

class CheckpointTask implements Task {
    private boolean isDone = false;
    private IOException theIOE = null;

    public void run() {
        try {
            TxnManager.get().requestAsyncCheckpoint();
        } catch (IOException anIOE) {
            TxnManager.theLogger.log(Level.SEVERE,
                                     "Checkpoint failed to complete",
                                     anIOE);
            theIOE = anIOE;
        }

        synchronized(this) {
            isDone = true;
            notify();
        }
    }

    void waitForCompletion() throws IOException {
        synchronized(this) {
            while (!isDone) {
                try {
                    wait();
                } catch (InterruptedException anIE) {
                    TxnManager.theLogger.log(Level.SEVERE,
                                             "Failed to wait for checkpoint completion",
                                             anIE);
                }
            }

            if (theIOE != null)
                throw theIOE;
        }
    }
}
