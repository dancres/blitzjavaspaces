package org.prevayler.implementation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import org.prevayler.PrevalentSystem;

/**
   Represents a snapshot waiting to be persisted to disk.  Invoke save() to
   place the snapshot on disk - should only be called once all dirty state
   (if any) has been flushed to disk.

   @see org.dancres.blitz.txn.TxnManager
 */
class SnapshotterImpl implements Snapshotter {
    private File theTemp;
    private File theReal;
    private byte[] theCachedCopy;

    private NumberFileCreator theFileCreator;

    private boolean shouldClean;

    SnapshotterImpl(NumberFileCreator aCreator, boolean doClean)
        throws IOException {

        theTemp = aCreator.newTempSnapshot();
        theReal = aCreator.newSnapshot();

        theFileCreator = aCreator;
        shouldClean = doClean;
    }

    void cacheSnapshot(PrevalentSystem aSystem) throws IOException {
        ByteArrayOutputStream myCache = new ByteArrayOutputStream();
        ObjectOutputStream mySnapper = new ObjectOutputStream(myCache);

        mySnapper.writeObject(aSystem);
        mySnapper.close();

        theCachedCopy = myCache.toByteArray();
    }

    public void save() throws IOException {
        FileOutputStream myOutput = new FileOutputStream(theTemp);
        myOutput.write(theCachedCopy);
        myOutput.close();

        if (!theTemp.renameTo(theReal))
            throw new IOException("Unable to rename " + theTemp +
                                  " to " + theReal);

        if (shouldClean)
            theFileCreator.newCleaner().clean();
    }
}
