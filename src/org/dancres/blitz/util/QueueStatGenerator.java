package org.dancres.blitz.util;

import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.TaskQueueStat;
import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

/**
 */
public class QueueStatGenerator implements StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private String _queueName;
    private BoundedLinkedQueue _channel;
    
    public QueueStatGenerator(String aName, BoundedLinkedQueue aQueue) {
        _queueName = aName;
        _channel = aQueue;
    }

    public long getId() {
        return theId;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public Stat generate() {
        return new TaskQueueStat(theId, _queueName, _channel.size());
    }
}
