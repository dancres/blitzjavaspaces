package org.dancres.blitz.disk;

import com.sleepycat.je.DatabaseException;

public interface RetryableOperation {
    public Object perform(DiskTxn aTxn) throws DatabaseException;
}