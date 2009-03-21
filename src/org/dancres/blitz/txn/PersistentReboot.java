package org.dancres.blitz.txn;

import java.util.logging.Level;

import org.dancres.blitz.meta.Registry;
import org.dancres.blitz.meta.RegistryFactory;

import org.dancres.blitz.config.PersistentBase;

import org.dancres.blitz.BootContext;

import org.dancres.blitz.disk.DiskTxn;

/**
   Certain steps need to be carried out as part of a reboot of a persistent
   (normal or time-barrier) instance of Blitz.  Those steps are the
   responsibility of this class.
 */
class PersistentReboot {
    private static final String BOOT_STATE = "BootState";

    private static final byte[] MAX_LOGS_KEY = {0x00, 0x00, 0x00, 0x01};

    private PersistentBase theBaseStorageModel;

    PersistentReboot(PersistentBase aBaseModel) {
        theBaseStorageModel = aBaseModel;
    }

    public void execute() throws Exception {
        int myCurrentMaxLogsBeforeSync =
            theBaseStorageModel.getMaxLogsBeforeSync();

        Registry myRegistry = RegistryFactory.get(BOOT_STATE, null);

        UnsyncdOps myBarrier = null;

        // Recover the old barrier information and use that for BootContext
        //
        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            myBarrier = (UnsyncdOps)
                myRegistry.getAccessor().load(MAX_LOGS_KEY);

            if (myBarrier != null) {
                TxnManager.theLogger.log(Level.INFO,
                                         "Restoring UnsyncdOps state: " +
                                         myBarrier.getOpsSinceLastCheckpoint());

                BootContext.add(myBarrier);
            }
        } finally {
            myTxn.commit();
        }

        // Update recovery barrier - this can only ever increase, we can't
        // decrease it due to recovery constraints
        //
        if ((myBarrier == null) ||
            (myCurrentMaxLogsBeforeSync >
             myBarrier.getOpsSinceLastCheckpoint())) {

            // System.err.println("Updating UnsyncdOps to: " +
            //                    myCurrentMaxLogsBeforeSync);

            myTxn = DiskTxn.newTxn();

            try {
                myRegistry.getAccessor().save(MAX_LOGS_KEY,
                                              new UnsyncdOps(myCurrentMaxLogsBeforeSync));
            } finally {
                myTxn.commit();
            }
        }

        myRegistry.close();
    }
}
