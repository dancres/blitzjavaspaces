package org.dancres.blitz.notify;

import java.io.IOException;

import java.util.logging.*;

import org.dancres.blitz.task.Task;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.oid.OID;

/**
   If SendTask determines that a particular client and it's associated
   notify() registration are no longer useful (due to particular exceptions
   being received from the client), an instance of this task will be scheduled
   to do the cleanup.
 */
class CleanTask implements Task {
    private OID theId;

    CleanTask(OID anId) {
        theId = anId;
    }

    public void run() {
        try {
            EventGeneratorFactory.get().killTemplate(theId);
        } catch (IOException anIOE) {
            RemoteEventDispatcher.theLogger.log(Level.SEVERE,
                                                "Failed to clean dead lease",
                                                anIOE);
        }
    }
}
