package org.dancres.blitz.stats;

/**
 */
public class EventQueueStat implements Stat {
    private long _id;

    private int _persistentCount;
    private int _transientCount;

    public EventQueueStat(long anId, int aPersistent, int aTransient) {
        _id = anId;
        _persistentCount = aPersistent;
        _transientCount = aTransient;
    }

    public long getId() {
        return _id;
    }

    public int getPersistentCount() {
        return _persistentCount;
    }

    public int getTransientCount() {
        return _transientCount;
    }

    public String toString() {
        return "EventQueue Listeners: transient: " + _transientCount +
            " persistent: " + _persistentCount;
    }
}
