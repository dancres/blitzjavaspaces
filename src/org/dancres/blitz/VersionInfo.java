package org.dancres.blitz;

import java.util.logging.Level;

import com.sleepycat.je.JEVersion;

public class VersionInfo {
    public static final String PRODUCT_NAME = "Blitz JavaSpaces (PureJavaEdition)";
    public static final String EMAIL_CONTACT = "blitz@dancres.org";
    public static final String SUPPLIER_NAME = "The Blitz Project";
    public static final String VERSION = "2.1.3";

    public static void dump() {
        SpaceImpl.theLogger.log(Level.INFO, "Version info: " +
                                PRODUCT_NAME + ", " + EMAIL_CONTACT + ", " +
                                SUPPLIER_NAME + ", " + VERSION + ", Db/Java " +
            JEVersion.CURRENT_VERSION.getVersionString());
    }

    public static void main(String anArgs[]) {
        System.out.println("Version info: " +
            PRODUCT_NAME + ", " + EMAIL_CONTACT + ", " +
            SUPPLIER_NAME + ", " + VERSION + ", Db/Java " +
            JEVersion.CURRENT_VERSION.getVersionString());
    }
}