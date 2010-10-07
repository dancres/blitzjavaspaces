package org.dancres.blitz.util;

import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.TaskQueueStat;

import java.util.concurrent.LinkedBlockingQueue;

/**
 */
public class QueueStatGenerator implements StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    private String _queueName;
    private LinkedBlockingQueue _channel;
    
    public QueueStatGenerator(String aName, LinkedBlockingQueue aQueue) {
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
