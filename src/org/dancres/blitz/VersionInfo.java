package org.dancres.blitz;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.je.JEVersion;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.VersionStat;

public class VersionInfo {
    private static final Logger theLogger = getLogger();

    private static Logger getLogger() {
        Logger myLogger = Logger.getLogger("org.dancres.blitz.VersionInfo");
        myLogger.setLevel(Level.INFO);

        return myLogger;
    }

    public static final String PRODUCT_NAME = "Blitz JavaSpaces (PureJavaEdition)";
    public static final String EMAIL_CONTACT = "blitz@dancres.org";
    public static final String SUPPLIER_NAME = "The Blitz Project";
    public static final String VERSION = "2.1.5";

    static {
        StatsBoard.get().add(new Generator());
    }

    private static class Generator implements StatGenerator {

        private long _id = StatGenerator.UNSET_ID;

        public long getId() {
            return _id;
        }

        public void setId(long anId) {
            _id = anId;
        }

        public Stat generate() {
            return new VersionStat(_id, versionString());
        }
    }

    private static String versionString() {
        return PRODUCT_NAME + ", " + EMAIL_CONTACT + ", " +
                SUPPLIER_NAME + ", " + VERSION + ", Db/Java " +
                JEVersion.CURRENT_VERSION.getVersionString();
    }

    public static void dump() {
        theLogger.log(Level.INFO, new VersionStat(versionString()).toString());
    }

    public static void main(String anArgs[]) {
        System.out.println(new VersionStat(versionString()).toString());
    }
}