package org.dancres.blitz.txn;

import java.util.logging.Level;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.SnapshotPrevaylerImpl;

import org.prevayler.PrevalentSystem;

import org.dancres.blitz.config.Persistent;

import org.dancres.blitz.disk.Disk;

import org.dancres.blitz.txn.batch.*;

/**
   Understands how to translate Persistent into core component configuration.

   @see org.dancres.blitz.config.Persistent
 */
class PersistentPersonality implements StoragePersonality {
    private Persistent theModel;
    private String theLogDir;

    PersistentPersonality(Persistent aModel, String aLogDir) {
        theModel = aModel;
        theLogDir = aLogDir;

        TxnManager.theLogger.log(Level.INFO, "PersistentPersonality");
        TxnManager.theLogger.log(Level.INFO, "Max logs before sync: " + 
                          theModel.getMaxLogsBeforeSync());
        TxnManager.theLogger.log(Level.INFO, "Reset log stream: " +
                          theModel.shouldResetLogStream());
        TxnManager.theLogger.log(Level.INFO, "Write barrier window: " +
                          theModel.getBatchWriteWindowSize());

        if (theModel.useConcurrentWriteBatcher())
                TxnManager.theLogger.log(Level.INFO,
                                         "*** Using concurrent batcher ***");

        if (theModel.shouldCleanLogs()) {
            TxnManager.theLogger.log(Level.WARNING,
                                     "*** Automatically cleaning logs *** [EXPERIMENTAL]");
        }

        Disk.init();
    }

    public CheckpointTrigger getCheckpointTrigger(Checkpointer aCheckpointer) {
        return
            new OpCountingCheckpointTrigger(aCheckpointer,
                                            theModel.getMaxLogsBeforeSync());
    }

    public SnapshotPrevayler getPrevayler(PrevalentSystem aSystem)
        throws Exception {

        PersistentReboot myReboot = new PersistentReboot(theModel);
        myReboot.execute();

        SnapshotPrevayler myPrevayler =
            new SnapshotPrevaylerImpl(aSystem,
                                      theLogDir,
                                      theModel.shouldResetLogStream(),
                                      theModel.shouldCleanLogs(),
                                      theModel.getLogBufferSize());

        if (theModel.getBatchWriteWindowSize() != 0) {
            int myWindowSize = theModel.getBatchWriteWindowSize();

            if (theModel.useConcurrentWriteBatcher()) {
                myPrevayler =
                    new ConcurrentWriteBatcher(myPrevayler, myWindowSize);
            } else {
                myPrevayler =
                    new WriteBatcher(myPrevayler, myWindowSize);
            }
        }

        return myPrevayler;
    }

    public void destroy() {
        Disk.destroy();
        Disk.clean(theLogDir);
    }
}
