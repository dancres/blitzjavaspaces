package org.dancres.blitz.stats;

/**
 */
public class WriteEventStat implements Stat {
    private long _id;
    private long _average;
    private long _total;

    public WriteEventStat(long anId, long anAverage, long aTotal) {
        _id = anId;
        _average = anAverage;
        _total = aTotal;
    }

    public long getId() {
        return _id;
    }

    public String toString() {
        return "Avg Write Event lifetime: " + _average + " for: " + _total;
    }
}
