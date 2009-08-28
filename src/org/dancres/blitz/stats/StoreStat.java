package org.dancres.blitz.stats;

/**
 */
public class StoreStat implements Stat {
    private static final long _minimalFree;

    private long _id;
    private String _type;
    private String[] _titles;
    private long[] _missed;
    private long[] _deleted;
    private boolean _safe;

    private int _activeCache;
    private int _totalCache;
    private long _free;
    private long _max;

    static {
        _minimalFree = Runtime.getRuntime().maxMemory() / 5;
    }

    public StoreStat(long anId, String aType,
                      String[] aTitles,
                      long[] aMisses, long[] aDeld, int anActiveCacheSize, int aCacheSize) {
        _id = anId;
        _type = aType;
        _titles = aTitles;
        _missed = aMisses;
        _deleted = aDeld;

        _activeCache = anActiveCacheSize;
        _totalCache = aCacheSize;
        _free = Runtime.getRuntime().freeMemory();
        _max = Runtime.getRuntime().maxMemory();

        _safe = (anActiveCacheSize == aCacheSize) &&
                (Runtime.getRuntime().freeMemory() >= _minimalFree);
    }
    
    public long getId() {
        return _id;
    }

    public String toString() {
        StringBuffer myBuffer = new StringBuffer("Store: " + _type);

        for (int i = 0; i < _titles.length; i++) {
            myBuffer.append(" ");
            myBuffer.append(_titles[i]);
            myBuffer.append(" miss: ");
            myBuffer.append(_missed[i]);
            myBuffer.append(" deld: ");
            myBuffer.append(_deleted[i]);
        }

        myBuffer.append(" sustainable: ");
        myBuffer.append(_safe);
        myBuffer.append(" (" + _activeCache + ", " + _totalCache + ", " + _free + ", " + _max + ")");

        return myBuffer.toString();
    }
}
