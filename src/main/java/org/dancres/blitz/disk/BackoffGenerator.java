package org.dancres.blitz.disk;

import java.util.Random;

import java.util.logging.Level;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.config.ConfigurationFactory;

public class BackoffGenerator {
    private static final String LOAD_BACKOFF = "loadBackoff";

    private static int BACKOFF_BASE;
    private static int BACKOFF_JITTER;

    static {
        try {
            int[] myBackoff = 
                (int[]) ConfigurationFactory.getEntry(LOAD_BACKOFF,
                                                      int[].class,
                                                      new int[]{50, 50});

            BACKOFF_BASE = myBackoff[0];
            BACKOFF_JITTER = myBackoff[1];
        } catch (ConfigurationException aCE) {
            RetryingUpdate.theLogger.log(Level.SEVERE,
                                         "Failed to load backoff config", aCE);
        }
    }

    private static Random theBackoffGenerator = new Random();

    public static void pause() {
        try {
            Thread.sleep((long) (BACKOFF_BASE +
                                 theBackoffGenerator.nextInt(BACKOFF_JITTER)));
        } catch (InterruptedException anIE) {
        }
    }
}