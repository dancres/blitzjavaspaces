package org.dancres.blitz.stats;

/**
   <p>Tracks WriteDaemon statistics.  This stat is permanently enabled and
   cannot be switched on or off.  It's maintenance costs are entirely absorbed
   by the caller recovering stats from StatsBoard.</p>

   @see org.dancres.blitz.disk.WriteDaemon
 */
public class IOStat implements Stat {
    private long theId;

    private double theTimePerIn;
    private double theTimePerOut;
    private double theInOutRatio;
    private int theQueueSize;
    private int theThrottleCount;

    public IOStat(long anId, double aTimePerIn, double aTimePerOut, 
                  double anInOutRatio, int aQueueSize, int aThrottleCount) {
        theId = anId;
        theTimePerIn = aTimePerIn;
        theTimePerOut = aTimePerOut;
        theInOutRatio = anInOutRatio;
        theQueueSize = aQueueSize;
        theThrottleCount = aThrottleCount;
    }

    public long getId() {
        return theId;
    }

    public double getTimePerIn() {
        return theTimePerIn;
    }

    public double getTimePerOut() {
        return theTimePerOut;
    }

    public double getInOutRatio() {
        return theInOutRatio;
    }

    public int getQueueSize() {
        return theQueueSize;
    }

    public int getThrottleCount() {
        return theThrottleCount;
    }

    public String toString() {
        return "IO TPI: " + theTimePerIn + " TPO: " + theTimePerOut + " IOR: " +
            theInOutRatio + " QSZ: " + theQueueSize + " THROTTLE: " +
            theThrottleCount;
    }
}
