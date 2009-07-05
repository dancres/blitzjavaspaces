package org.dancres.blitz.config;

/**
   Configures Blitz to be maximally durable with all operations being recorded
   on disk immediately they are commited.

   @todo Add MAX_LOGS_BEFORE_SYNC disable
 */
public class Persistent extends PersistentBase {

    private boolean useConcurrentBatcher;
    private long theBatchWriteWindowSizeMs;
    private int theBatchWriteWindowSizeNs;

    /**
       @param shouldReset specifies whether to reset the ObjectOutputStream
       used for logging. This is a performance vs memory tradeoff

       @param shouldClean specifies whether old log files and snapshots should
       be cleaned up or left for archiving.

       @param aBatchWriteWindowSize specifies a batch-write window for logging.
       Set this to zero to disable batching. Batch-writing can reduce the
       number of forced flushes to disk whilst increasing the amount of data
       written with each flush.  This has a positive effect on throughput
       under concurrent load. Time is specified in ms, first thread into
       barrier waits this amount of time for other writers.  Other writers
       entering the barrier are now blocked until the first entrant commits all
       writes to log.

       @param useConcurrent should increase log throughput by allowing the
       next batch to form whilst the current batch is being written to log.

       @param aMaxLogsBeforeSync is the maximum number of log entries before
       a checkpoint is forced.

       @param aLogBufferSize especially useful when doing batching.
       All commands are rendered to the buffer before going to disk in one
       large block. Without the buffer, each command will trickle to disk as a
       small update which isn't good for throughput!
     */
    public Persistent(boolean shouldReset, boolean shouldClean,
                      int aBatchWriteWindowSize, boolean useConcurrent,
                      int aMaxLogsBeforeSync, int aLogBufferSize) {

        super(shouldReset, shouldClean, aLogBufferSize, aMaxLogsBeforeSync);
        useConcurrentBatcher = useConcurrent;
        theBatchWriteWindowSizeMs = aBatchWriteWindowSize;
    }

    public boolean useConcurrentWriteBatcher() {
        return useConcurrentBatcher;
    }

    public long getBatchWriteWindowSizeMs() {
        return theBatchWriteWindowSizeMs;
    }

    public int getBatchWriteWindowSizeNs() {
        return theBatchWriteWindowSizeNs;
    }

    public Persistent(boolean shouldReset, boolean shouldClean,
                      long aBatchWindowSizeMs, int aBatchWindowSizeNs, boolean useConcurrent,
                      int aMaxLogsBeforeSync, int aLogBufferSize) {

        super(shouldReset, shouldClean, aLogBufferSize, aMaxLogsBeforeSync);
        useConcurrentBatcher = useConcurrent;
        theBatchWriteWindowSizeMs = aBatchWindowSizeMs;
        theBatchWriteWindowSizeNs = aBatchWindowSizeNs;
    }
}
