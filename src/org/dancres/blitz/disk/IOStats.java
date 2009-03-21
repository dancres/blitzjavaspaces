package org.dancres.blitz.disk;

import java.util.logging.Level;

class IOStats {
    private int theAsyncInCount;
    private int theAsyncOutCount;

    private double theSampleStartTime;

    private double theTimePerOut;
    private double theTimePerIn;

    private double theInOutLag;
    private double theInOutRatio;

    private int theQueueSize;

    double getTimePerIn() {
        return theTimePerIn;
    }

    double getTimePerOut() {
        return theTimePerOut;
    }

    double getInOutRatio() {
        return theInOutRatio;
    }

    void incAsyncInCount() {
        synchronized(this) {
            ++theAsyncInCount;
            ++theQueueSize;
        }
    }

    int getQueueSize() {
        synchronized(this) {
            return theQueueSize;
        }
    }

    /**
       @return <code>true</code> if the io statistics were updated, <code>
       false</code> otherwise.
     */
    boolean incAsyncOutCount() {
        synchronized(this) {
            if (theAsyncOutCount == 0)
                theSampleStartTime = (double) System.currentTimeMillis();

            ++theAsyncOutCount;
            --theQueueSize;

            /*
              Probably this should be based on low watermark so the
              calculation is available before the high watermark is hit.
              Probably ought to remove desired pending count as well at some
              point.
            */
            if (theAsyncOutCount == 1000) {
                double myDuration = (double) System.currentTimeMillis() -
                    theSampleStartTime;

                theTimePerOut = myDuration / 1000.0;
                theTimePerIn = myDuration / (double) theAsyncInCount;

                theInOutLag = theTimePerOut - theTimePerIn;
                theInOutRatio = theTimePerOut / theTimePerIn;

                theAsyncOutCount = theAsyncInCount = 0;

                return true;
            }

            return false;
        }
    }

    void dumpStats() {
        synchronized(this) {
            WriteDaemon.theLogger.log(Level.INFO, "TPI: " + theTimePerIn +
                " TPO: " + theTimePerOut + " IOL: " + theInOutLag +
                " IOR: " + theInOutRatio + " QSZ: " + theQueueSize);
        }
    }
}
