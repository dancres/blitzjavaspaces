package org.prevayler.implementation;

import org.dancres.blitz.txn.TxnManager;

import java.io.File;
import java.io.FileFilter;

import java.util.Arrays;
import java.util.logging.Level;

/**
   <p>Cleans up old log files and snapshots.  Helper class produced from a
   NumberFileCreator which can be used by a programmer to clean up no longer
   required log files and snapshots.  Not all deployments will want to use this
   as some will wish to archive all such logs and snapshots for recovery
   purposes or to satisfy certain legal requirements.</p>
 */
class NumberFileCleaner {
    File theDirectory;

    NumberFileCleaner(File aDirectory) {
        theDirectory = aDirectory;
    }

    void clean() {
        File[] myFiles = theDirectory.listFiles(new SnapshotFilter());

        Arrays.sort(myFiles);

        /*
        for (int i = (myFiles.length -1); i >-1; i--) {
            TxnManager.theLogger.log(Level.SEVERE, "Ordered: " + myFiles[i]);
        }
        */

        int myLastSnapshot = findLastSnapshot(myFiles);

        if (myLastSnapshot != -1) {
            for (int i = (myLastSnapshot - 1); i > -1; i--) {
                // TxnManager.theLogger.log(Level.SEVERE, "Delete: " + myFiles[i]);
                myFiles[i].delete();
            }
        }
    }

    private int findLastSnapshot(File[] aFiles) {
        for (int i = (aFiles.length - 1); i > -1; i--) {
            File myFile = aFiles[i];
            if (myFile.getName().endsWith("." +
                                          NumberFileCreator.SNAPSHOT_SUFFIX)) {
                return i;
            }
        }

        return -1;
    }

    private static class SnapshotFilter implements FileFilter {
        public boolean accept(File aFile) {
            String myName = aFile.getName();

            if ((aFile.isDirectory()) ||
                ((!myName.endsWith("." +
                                   NumberFileCreator.SNAPSHOT_SUFFIX)) &&
                 (!myName.endsWith("." +
                                   NumberFileCreator.LOGFILE_SUFFIX))))
                return false;

            try {
                Long.parseLong(myName.substring(0,myName.indexOf('.')));

                return true;
            } catch (NumberFormatException nfx) {
                return false;
            }
        }
    }


}
