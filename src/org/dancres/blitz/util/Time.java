package org.dancres.blitz.util;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;

/**
   Various methods/classes must cope with the JINI time model.  This class
   provides useful methods for dealing with that model.
 */
public class Time {
    /**
       Computes an expiry time based on the passed in wait time which might
       be a suitable value or it might be Lease.FOREVER
     */
    public static long getAbsoluteTime(long aWaitTime) {
        if (aWaitTime == Lease.FOREVER)
            return Long.MAX_VALUE;

        long myTime = System.currentTimeMillis() + aWaitTime;

        if (myTime < 0)
            return Long.MAX_VALUE;
        else
            return myTime;
    }

    /**
       Compute the actual time to relative wait time based on the passed
       argument.  This will typically used by read/take variants.
     */
    public static long getWaitTime(long aWaitTime) {
        if (aWaitTime == Lease.FOREVER)
            return Long.MAX_VALUE;
        else
            return aWaitTime;
    }

    /**
       Computes a lease duration given a lease duration which
       could be a legitimate duration, Lease.FOREVER or Lease.ANY

       @param aTime the desired lease length
       @param aBound the maximum lease length allowed.  If aBound is zero,
       then the max time is assumed to be Lease.FOREVER
     */
    public static long getLeaseDuration(long aTime, long aBound) {

        long myBound;
        long myDuration = aTime;

        // Convert the bound to something sane
        if (aBound == 0)
            myBound = Lease.FOREVER;
        else
            myBound = aBound;

        // Allow FOREVER or not, based on bound
        if (aTime == Lease.FOREVER) {
            if (aBound == 0)
                return Long.MAX_VALUE;
            else
                return myBound;
        }

        // If ANY, use the sanitized bound
        if (aTime == Lease.ANY) {
            myDuration = myBound;
        }

        if (myDuration > myBound)
            return myBound;
        else
            return myDuration;
    }
}
