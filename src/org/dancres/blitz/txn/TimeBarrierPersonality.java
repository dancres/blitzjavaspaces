package org.dancres.blitz.txn;

import java.util.logging.Level;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.BufferingPrevaylerImpl;

import org.prevayler.PrevalentSystem;

import org.dancres.blitz.config.TimeBarrierPersistent;

import org.dancres.blitz.disk.Disk;

/**
   Understands how to translate TimeBarrierPersistent into core component
   configuration.

   @see org.dancres.blitz.config.TimeBarrierPersistent
 */
class TimeBarrierPersonality implements StoragePersonality {
    private TimeBarrierPersistent theModel;
    private String theLogDir;

    TimeBarrierPersonality(TimeBarrierPersistent aModel, String aLogDir) {
        theModel = aModel;
        theLogDir = aLogDir;

        TxnManager.theLogger.log(Level.INFO, "TimeBarrierPersonality");

        TxnManager.theLogger.log(Level.INFO, "Max logs before sync: " + 
                          theModel.getMaxLogsBeforeSync());
        TxnManager.theLogger.log(Level.INFO, "Reset log stream: " +
                          theModel.shouldResetLogStream());

        if (theModel.shouldCleanLogs()) {
            TxnManager.theLogger.log(Level.WARNING,
                                     "*** Automatically cleaning logs *** [EXPERIMENTAL]");
        }

        Disk.init();
    }

    public CheckpointTrigger getCheckpointTrigger(Checkpointer aCheckpointer) {
        /*
        return
            new OpCountingCheckpointTrigger(aCheckpointer,
                                            theModel.getMaxLogsBeforeSync());
                                            */
        return
            new TimeoutCheckpointTrigger(aCheckpointer,
                theModel.getMaxLogsBeforeSync(), theModel.getFlushTime());
    }

    public SnapshotPrevayler getPrevayler(PrevalentSystem aSystem)
        throws Exception {

        PersistentReboot myReboot = new PersistentReboot(theModel);
        myReboot.execute();

        SnapshotPrevayler myPrevayler =
            new BufferingPrevaylerImpl(aSystem, theLogDir,
                                       theModel.shouldResetLogStream(),
                                       theModel.shouldCleanLogs(),
                                       theModel.getLogBufferSize());

        return myPrevayler;
    }

    public void destroy() {
        Disk.destroy();
        Disk.clean(theLogDir);
    }
}
