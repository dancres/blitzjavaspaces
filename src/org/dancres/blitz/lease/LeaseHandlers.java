package org.dancres.blitz.lease;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.logging.*;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.Lease;

import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationFile;
import net.jini.config.Configuration;

import org.dancres.blitz.Logging;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.util.Time;

/**
   The access point for managing leases associated with SpaceUIDs.  Lease
   management is achieved through a collection of LeaseHandlers loaded from
   <code>handlers.properties</code>.  Each LeaseHandler "knows" about one
   (or possibly more) types of SpaceUID and handles renewal or cancelling of
   the lease associated with the SpaceUID.

   @see org.dancres.blitz.lease.LeaseHandler
 */
public class LeaseHandlers {
    private static final String MODULE_NAME =
        ConfigurationFactory.BLITZ_MODULE;

    private static final String HANDLER_PROPERTY = "handlers";

    private static LeaseHandler[] theHandlers;

    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.lease.LeaseHandlers");

    static {
        try {
            InputStream myStream = 
                LeaseHandlers.class.getResourceAsStream("handlers.properties");

            if (myStream == null)
                throw new IOException();
            else {
                Configuration myConfig =
                    new ConfigurationFile(new InputStreamReader(myStream),
                                          null);

                LeaseHandler[] myFilters =
                    (LeaseHandler[]) myConfig.getEntry(MODULE_NAME,
                                                       HANDLER_PROPERTY,
                                                       LeaseHandler[].class);

                theHandlers = myFilters;
            }

        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to load lease handlers", anE);
        }
    }

    public static long renew(SpaceUID aUID, long aLeaseDuration)
        throws UnknownLeaseException, LeaseDeniedException, IOException {

        for (int i = 0; i < theHandlers.length; i++) {
            if (theHandlers[i].recognizes(aUID)) {
                return theHandlers[i].renew(aUID, aLeaseDuration);
            }
        }

        throw new UnknownLeaseException();
    }

    public static void cancel(SpaceUID aUID)
        throws UnknownLeaseException, IOException {

        for (int i = 0; i < theHandlers.length; i++) {
            if (theHandlers[i].recognizes(aUID)) {
                theHandlers[i].cancel(aUID);
                return;
            }
        }

        throw new UnknownLeaseException();
    }
}
