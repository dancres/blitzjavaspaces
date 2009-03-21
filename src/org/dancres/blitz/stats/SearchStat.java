package org.dancres.blitz.stats;

/**
 */
public class SearchStat implements Stat {
    private long _id;
    private String _type;
    private String[] _titles;
    private long[] _missed;
    private long[] _deleted;

    public SearchStat(long anId, String aType,
                      String[] aTitles,
                      long[] aMisses, long[] aDeld) {
        _id = anId;
        _type = aType;
        _titles = aTitles;
        _missed = aMisses;
        _deleted = aDeld;
    }
    
    public long getId() {
        return _id;
    }

    public String toString() {
        StringBuffer myBuffer = new StringBuffer("Search: " + _type);

        for (int i = 0; i < _titles.length; i++) {
            myBuffer.append(" ");
            myBuffer.append(_titles[i]);
            myBuffer.append(" miss: ");
            myBuffer.append(_missed[i]);
            myBuffer.append(" deld: ");
            myBuffer.append(_deleted[i]);
        }

        return myBuffer.toString();
    }
}
