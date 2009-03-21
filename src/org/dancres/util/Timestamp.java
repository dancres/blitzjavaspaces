package org.dancres.util;

import java.util.HashMap;

/**
   Generates timestamps values which can be used to uniquely order events.
 */
public class Timestamp {
    private static HashMap theTimestamps = new HashMap();

    public static Timestamp newTimestamp(String aName) {
        synchronized(theTimestamps) {
            if (theTimestamps.get(aName) != null)
                throw new IllegalArgumentException("Timestamp already exists");

            Timestamp myTimestamp = new Timestamp();
            theTimestamps.put(aName, myTimestamp);

            return myTimestamp;
        }
    }

    public static Timestamp getTimestamp(String aName) {
        synchronized(theTimestamps) {
            return (Timestamp) theTimestamps.get(aName);
        }
    }

    long theStamp = 0;

    private Timestamp() {
    }

    /**
       Returns the last timestamp allocated or zero if none have been
       allocated yet.
     */
    public long get() {
        synchronized(this) {
            return theStamp;
        }
    }

    /**
       Return the next timestamp
     */
    public long next() {
        synchronized(this) {
            return ++theStamp;
        }
    }
}
