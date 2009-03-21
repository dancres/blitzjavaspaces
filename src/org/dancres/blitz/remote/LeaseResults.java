package org.dancres.blitz.remote;

/**
   <p>Used to convey the results of a renew/cancelAll method invocation.</p>

   <p>Should consist of two arrays - one contains longs - for renew, if -1
   failure else it's a new duration which should be added to local system
   time millis and put in the lease.  The other array contains exceptions
   for those with a -1.  In the case of cancel, only the exception array
   is populated and the entries are null indicating successful cancel or
   an exception indicating a problem.</p>
 */
public class LeaseResults implements java.io.Serializable {
    long[] theNewDurations;
    Exception[] theFailures;

    LeaseResults(long[] aNewDurations, Exception[] aFails) {
        theNewDurations = aNewDurations;
        theFailures = aFails;
    }
}
