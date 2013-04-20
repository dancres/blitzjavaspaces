package org.dancres.blitz.stats;

/**
 */
public class TaskQueueStat implements Stat {
    private long _id;
    private String _queueName;
    private int _size;

    public TaskQueueStat(long anId, String aQueueName, int aSize) {
        _id = anId;
        _queueName = aQueueName;
        _size = aSize;
    }

    public long getId() {
        return _id;
    }

    public String getQueueName() {
        return _queueName;
    }

    public int getQueueSize() {
        return _size;
    }

    public String toString() {
        return "Queue: " + _queueName + " size: " + _size; 
    }
}
