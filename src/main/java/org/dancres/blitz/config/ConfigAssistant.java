package org.dancres.blitz.config;

/**
   <p> As Blitz can be run on many operating systems (Win32, Mac OS X and most
   UNIX varieties) and on top of several different versions of Db (4.1 and
   4.2), initial configuration has the potential to be time-consuming. To make
   the task simpler, the Blitz distribution includes a tool (ConfigAssistant)
   which will assist you in determining what your configuration is and what
   changes are required. Run ConfigAssistant as follows:</p>

   <pre>
   cd /home/dan/src/jini/space/

   java -Djava.security.policy=config/policy.all -cp lib/blitz.jar
                    org.dancres.blitz.config.ConfigAssistant

   </pre>

   <p>
   ConfigAssistant performs the following steps:
   </p>

   <ol>
   <li> Identify the operating system you are running on.</li>
   <li> Identify which version of Db you have installed.</li>
   <li> Display recommended .config file changes and any platform specific instructions for running Blitz.</li>
   </ol>

   <p>
   ConfigAssistant may fail to determine the version of Db you have installed.#
   When this happens, it will suggest the likely cause of the problem and exit
   Once the problem is identified and fixed, you can re-run
   ConfigAssistant.</p>

   <p>
   <b>Note:</b> Mac OS X users should read the platform notes which details
   additional requirements for running ConfigAssistant, ServiceStarter etc.
   </p>
 */
public class ConfigAssistant {
    private static final String[][] DB_VERSIONS = {{"4", "1"}, {"4", "2"}};

    private static final int[] DB_VERSION_CODE = {0, 1};

    private static final String[] DB_NAMES = {"db4.1", "db4.2"};

    private static final int OS_X = 1;
    private static final int WIN32 = 2;
    private static final int UNIX = 3;

    private static final String DB_PROP = "sleepycat.db.libfile";

    private static final String[] CONFIG_GENERAL = {
        "",
        "**********",
        "** Note **",
        "**********",
        "",
        "Remember to set the persistDir and logDir variables in your",
        "blitz.config to suitable directories which should be writable",
        "by the user you plan to run Blitz under.",
        "",
        ""
    };

    private static final String[] CONFIG_UNIX_DB41 = {
        "UNIX with Db4.1",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values"
    };

    private static final String[] CONFIG_UNIX_DB42 = {
        "UNIX with Db4.2",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"db42/\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values"
    };

    private static final String[][] CONFIG_UNIX = {CONFIG_UNIX_DB41,
                                                   CONFIG_UNIX_DB42};

    private static final String[] CONFIG_OS_X_DB41 = {
        "OSX with Db4.1",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values",
        "",
        "*** IMPORTANT NOTES ON USING SERVICESTARTER ***",
        "",
        "When using start-transient-blitz.config or other transient configurations",
        "Make sure you run the java command with -D" + DB_PROP + "=" +
        System.getProperty(DB_PROP),
        "",
        "When using start-activatable-blitz.config or other activatable configurations",
        "Make sure you set sharedVM_options (in the .config file) to",
        "\"-D" + DB_PROP + "=" + System.getProperty(DB_PROP) + "\""
    };

    private static final String[] CONFIG_OS_X_DB42 = {
        "OSX with Db4.2",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"db42/\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values",
        "",
        "*** IMPORTANT NOTES ON USING SERVICESTARTER ***",
        "",
        "When using start-transient-blitz.config or other transient configurations",
        "Make sure you run the java command with -D" + DB_PROP + "=" +
        System.getProperty(DB_PROP),
        "",
        "When using start-activatable-blitz.config or other activatable configurations",
        "Make sure you set sharedVM_options (in the .config file) to",
        "\"-D" + DB_PROP + "=" + System.getProperty(DB_PROP) + "\""
    };

    private static final String[][] CONFIG_OS_X = {CONFIG_OS_X_DB41,
                                                   CONFIG_OS_X_DB42};

    private static final String[] CONFIG_WIN32_DB41 = {
        "Win32 with Db4.1",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values"
    };

    private static final String[] CONFIG_WIN32_DB42 = {
        "Win32 with Db4.2",
        "===============",
        "",
        "Edit the start-*.config files as follows:",
        "",
        "Set dbVersion=\"db42/\"",
        "Edit jiniRoot, dbLib, codebasePort and blitzRoot to appropriate values"
    };

    private static final String[][] CONFIG_WIN32 = {CONFIG_WIN32_DB41,
                                                    CONFIG_WIN32_DB42};

    public static void main(String args[]) {

        if (Boolean.getBoolean("debug")) {
            System.err.println("OS name: " + System.getProperty("os.name"));

            System.err.println("MacOSX: " + System.getProperty(DB_PROP));

            for (int i = 0; i < DB_VERSIONS.length; i++) {
                String myLibName;

                myLibName = getLibName(DB_VERSIONS[i][0], DB_VERSIONS[i][1],
                                       WIN32);

                System.err.println("Win32: " + myLibName + ", " +
                                   libraryPresent(myLibName, WIN32));

                myLibName = getLibName(DB_VERSIONS[i][0], DB_VERSIONS[i][1],
                                       UNIX);

                System.err.println("Unix: " + myLibName + ", " +
                                   libraryPresent(myLibName, UNIX));
            }

            dumpInstructions(CONFIG_UNIX);
            dumpInstructions(CONFIG_OS_X);
            dumpInstructions(CONFIG_WIN32);

            System.exit(0);
        }

        if (System.getProperty("os.name").startsWith("Windows")) {
            configWindows();
            return;
        }

        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            if (System.getProperty(DB_PROP) == null) {
                System.err.println();
                System.err.println("Please read docs/mac_osx.html and rerun ConfigAssistant with an appropriate -D" + DB_PROP);
                System.exit(0);
            } else {
                configMacOSX();
                return;
            }
        }

        configUnix();
    }

    private static void dumpInstructions(String[][] anInsts) {
        for (int i = 0; i < anInsts.length; i++) {
            System.err.println();
            dumpSpecificInstructions(anInsts[i]);
            System.err.println();
        }
    }

    private static void dumpSpecificInstructions(String[] anInst) {
        for (int i = 0; i < anInst.length; i++) {
            System.err.println(anInst[i]);
        }
    }

    private static void configMacOSX() {
        System.err.println("I think you're on Mac OSX");

        int myVersion = probeLib(OS_X);

        if (myVersion == -1) {
            System.err.println("Couldn't locate Db :(");
            System.err.println("Are the Db .so's on  LD_LIBRARY_PATH?");
            System.exit(0);
        } else {
            System.err.println("I think you have: " + DB_NAMES[myVersion]);
        }

        System.err.println();
        dumpSpecificInstructions(CONFIG_OS_X[myVersion]);
        dumpSpecificInstructions(CONFIG_GENERAL);
    }

    private static void configUnix() {
        System.err.println("I think you're on UNIX");

        int myVersion = probeLib(UNIX);

        if (myVersion == -1) {
            System.err.println("Couldn't locate Db :(");
            System.err.println("Are the Db .so's on  LD_LIBRARY_PATH?");
            System.exit(0);
        } else {
            System.err.println("I think you have: " + DB_NAMES[myVersion]);
        }

        System.err.println();
        dumpSpecificInstructions(CONFIG_UNIX[myVersion]);
        dumpSpecificInstructions(CONFIG_GENERAL);
    }

    private static void configWindows() {
        System.err.println("I think you're on Win32");

        int myVersion = probeLib(WIN32);

        if (myVersion == -1) {
            System.err.println("Couldn't locate Db :(");
            System.err.println("Are the Db .dll's on PATH?");
            System.exit(0);
        } else {
            System.err.println("I think you have: " + DB_NAMES[myVersion]);
        }

        System.err.println();
        dumpSpecificInstructions(CONFIG_WIN32[myVersion]);
        dumpSpecificInstructions(CONFIG_GENERAL);
    }

    private static String getLibName(String aMajor, String aMinor,
                                     int aPlatform) {

        switch(aPlatform) {
            case OS_X : {
                return System.getProperty(DB_PROP);
            }

            case WIN32 : {
                return "libdb_java" + aMajor + aMinor;
            }

            case UNIX : {
                return "db_java-" + aMajor + "." + aMinor;
            }

            default : {
                throw new RuntimeException("Not a platform");
            }
        }
    }

    private static int probeLib(int aPlatform) {
        if (aPlatform == OS_X) {
            String myLib = System.getProperty(DB_PROP);

            if (libraryPresent(myLib, OS_X)) {
                if (myLib.indexOf("db_java-4.1") != -1)
                    return DB_VERSION_CODE[0];
                else
                    return DB_VERSION_CODE[1];
            }
        } else {
            for (int i = 0; i < DB_VERSIONS.length; i++) {
                String myLibName;

                myLibName = getLibName(DB_VERSIONS[i][0], DB_VERSIONS[i][1],
                                       aPlatform);

                if (libraryPresent(myLibName, aPlatform))
                    return DB_VERSION_CODE[i];
            }
        }

        return -1;
    }

    private static boolean libraryPresent(String aLibName, int aPlatform) {
        try {
            if (aPlatform == OS_X) {
                System.load(aLibName);
            } else {
                System.loadLibrary(aLibName);
            }
        } catch (UnsatisfiedLinkError aULE) {
            return false;
        }

        return true;
    }
}
