package org.dancres.blitz.stats;

public class VersionStat implements Stat {
    private long _id;
    private String _string;

    public VersionStat(String aVersion) {
        _id = StatGenerator.UNSET_ID;
        _string = aVersion;
    }

    public VersionStat(long anId, String aVersion) {
        _id = anId;
        _string = aVersion;
    }

    public long getId() {
        return _id;
    }

    public String getVersion() {
        return _string;
    }

    public String toString() {
        return "Version info: " + getVersion();
    }
}
