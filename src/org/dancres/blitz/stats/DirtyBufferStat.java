package org.dancres.blitz.stats;

/**
 */
public class DirtyBufferStat implements Stat {
    private long _id;
    private String _type;
    private int _size;

    public DirtyBufferStat(long anId, String aType, int aBufferSize) {
        _id = anId;
        _type = aType;
        _size = aBufferSize;
    }

    public long getId() {
        return _id;
    }

    public String toString() {
        return "Dirty Buffer: " + _type + " size: " + _size;
    }
}
