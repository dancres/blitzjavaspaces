package org.dancres.blitz.config;

/**
   <p>Configures Blitz to make operations durable after a certain period of
   time such that if an operation was submitted more than that time period ago,
   it is guarenteed to be durable.</p>

   <p>This kind of persistence provides the developer/administrator with the
   ability to trade persistence guarentee for speed.  Certain applications can
   tolerate this sort of behaviour because they incorporate their own
   consistency checks which allow them to recover a certain amount of recent 
   state that the space hadn't yet persisted.</p>
 */
public class TimeBarrierPersistent extends PersistentBase {

    private long theFlushTime;

    /**
       @param shouldReset specifies whether to reset the ObjectOutputStream
       used for logging. This is a performance vs memory tradeoff

       @param shouldClean specifies whether old log files and snapshots should
       be cleaned up or left for archiving.

       @param aMaxLogsBeforeSync is the maximum number of log entries before
       a checkpoint is forced.

       @param aLogBufferSize especially useful when doing flushing
       All commands are rendered to the buffer before going to disk in one
       large block. Without the buffer, each command will trickle to disk as a
       small update which isn't good for throughput!

       @param aFlushTime the period in millis after which a logged command
       should be made durable.
     */
    public TimeBarrierPersistent(boolean shouldReset, boolean shouldClean,
                                 int aMaxLogsBeforeSync, int aLogBufferSize,
                                 long aFlushTime) {

        super(shouldReset, shouldClean, aLogBufferSize, aMaxLogsBeforeSync);
        theFlushTime = aFlushTime;
    }

    public long getFlushTime() {
        return theFlushTime;
    }
}
