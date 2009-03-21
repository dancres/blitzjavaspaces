package org.dancres.blitz.txn;

import java.io.File;

import java.util.logging.Level;

import org.prevayler.implementation.NullPrevayler;
import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.PrevalentSystem;

import org.dancres.blitz.disk.Disk;

class TransientPersonality implements StoragePersonality {
    private String theLogDir;

    TransientPersonality(String aLogDir) {
        theLogDir = aLogDir;

        TxnManager.theLogger.log(Level.INFO, "TransientPersonality");

        destroy();

        Disk.setTransient(true);
        Disk.init();
    }

    public CheckpointTrigger getCheckpointTrigger(Checkpointer aCheckpointer) {
        return new NeverTrigger();
    }

    public SnapshotPrevayler getPrevayler(PrevalentSystem aSystem)
        throws Exception {

        return new NullPrevayler(aSystem);
    }

    public void destroy() {
        Disk.destroy();
        Disk.clean(theLogDir);
    }
}
