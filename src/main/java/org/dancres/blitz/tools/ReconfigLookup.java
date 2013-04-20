package org.dancres.blitz.tools;

import org.dancres.blitz.remote.LookupStorage;
import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.disk.Disk;

/**
 * Reimport specified lookup data from specified configuration
 */
public class ReconfigLookup {
    private static String[] OPTIONS =
        new String[] {"-attrs", "-locators", "-groups"};

    private static byte[] FLAGS =
        new byte[] {LookupStorage.INIT_ATTRS, LookupStorage.INIT_LOCATORS,
            LookupStorage.INIT_GROUPS};

    public static void main(String anArgs[]) throws Exception {
        if (anArgs.length < 2) {
            System.err.println(
                "Usage: blitz_config_URL [-attrs | -locators | -groups]+ ");
        }

        String[] myURL = new String[] {anArgs[0]};
        byte myFlags = 0;

        for (int i = 1; i < anArgs.length; i++) {
            myFlags = (byte) (myFlags | convert(anArgs[i]));
        }

        ConfigurationFactory.setup(myURL);

        Disk.init();

        LookupStorage myStorage = new LookupStorage();
        myStorage.reinit(ConfigurationFactory.getConfig(), myFlags);
        myStorage.saveState();

        Disk.sync();
        Disk.stop();
    }

    private static byte convert(String anOption)
        throws IllegalArgumentException {

        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i].equals(anOption))
                return FLAGS[i];
        }

        throw new IllegalArgumentException("Invalid option: " + anOption);
    }
}
