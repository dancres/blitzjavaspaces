package org.dancres.blitz.config;

/**
   All persistent modes share this same set of base settings
 */
public abstract class PersistentBase implements StorageModel {

    private boolean shouldResetLogStream;
    private boolean shouldCleanLogs;
    private int theLogBufferSize;
    private int theMaxLogsBeforeSync;

    PersistentBase(boolean shouldReset, boolean shouldClean,
                   int aLogBufferSize, int aMaxLogsBeforeSync) {
        shouldResetLogStream = shouldReset;
        shouldCleanLogs = shouldClean;
        theLogBufferSize = aLogBufferSize;
        theMaxLogsBeforeSync = aMaxLogsBeforeSync;
    }

    public boolean shouldResetLogStream() {
        return shouldResetLogStream;
    }

    public boolean shouldCleanLogs() {
        return shouldCleanLogs;
    }

    public int getLogBufferSize() {
        return theLogBufferSize;
    }

    public int getMaxLogsBeforeSync() {
        return theMaxLogsBeforeSync;
    }
}
