package org.dancres.blitz.disk;

import java.io.IOException;
import java.io.File;

import java.util.logging.Level;

import org.dancres.io.FileCopier;

class BackupTask implements Runnable {
    private File theSourceDir;
    private File theDestDir;

    private FileCopier theCopier = new FileCopier();

    private boolean isComplete = false;
    private IOException theException = null;

    BackupTask(File aSource, File aDest) {
        theSourceDir = aSource;
        theDestDir = aDest;
    }

    public void run() {
        Disk.theLogger.log(Level.SEVERE, "Start backup: " + theSourceDir
                           + ", " + theDestDir);

        File[] myFiles = theSourceDir.listFiles();

        Disk.theLogger.log(Level.SEVERE, "Number of files: " +
                           myFiles.length);

        try {
            for (int i = 0; i < myFiles.length; i++) {
                if (myFiles[i].isFile()) {
                    Disk.theLogger.log(Level.SEVERE, "copy file: " +
                                       myFiles[i]);

                    theCopier.copy(myFiles[i], theDestDir);
                }
            }
        } catch (IOException anIOE) {
            theException = anIOE;
        }

        Disk.theLogger.log(Level.SEVERE, "Backup complete");

        synchronized(this) {
            isComplete = true;
            notify();
        }
    }

    void waitForCompletion() throws IOException {
        synchronized(this) {
            while (!isComplete) {
                try {
                    wait();
                } catch (InterruptedException anIE) {
                }
            }
        }

        if (theException != null)
            throw theException;
    }
}

