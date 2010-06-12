package org.dancres.blitz.txn;

import java.util.logging.Level;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.PrevaylerCore;

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
                          theModel.getBatchWriteWindowSizeMs() + ", " + theModel.getBatchWriteWindowSizeNs());

        if (!theModel.dontUseExperimentalBatcher())
                TxnManager.theLogger.log(Level.INFO,
                                         "*** Experimental batcher enabled ***");

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

        PrevaylerCore myPrevayler =
            new PrevaylerCore(aSystem,
                                      theLogDir,
                                      theModel.shouldResetLogStream(),
                                      theModel.shouldCleanLogs(),
                                      theModel.getLogBufferSize());

        if ((theModel.getBatchWriteWindowSizeMs() != 0) ||
            (theModel.getBatchWriteWindowSizeNs() != 0)) {
            long myWindowSizeMs = theModel.getBatchWriteWindowSizeMs();
            int myWindowSizeNs = theModel.getBatchWriteWindowSizeNs();

            if (theModel.dontUseExperimentalBatcher()) {
                return new ConcurrentWriteBatcher(myPrevayler, myWindowSizeMs, myWindowSizeNs);
            } else {
                return new OptimisticBatcher(myPrevayler);
            }
        } else
            return new NullBatcher(myPrevayler);
    }

    public void destroy() {
        Disk.destroy();
        Disk.clean(theLogDir);
    }
}
