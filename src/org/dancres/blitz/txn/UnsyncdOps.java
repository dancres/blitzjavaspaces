package org.dancres.blitz.txn;

import org.dancres.blitz.BootInfo;

/**
   <p>UnsyncdOps provides boot-time information concerning the maximum number
   of log operations which might have occurred since the last full
   checkpoint/sync was initiated.</p>

   <p>We deliberately return double the op count to allow for interrupted 
   checkpoints.  An interrupted checkpoint leads to two contiguous
   log files with no separating checkpoint image.  The maximum number
   of operations before the next checkpoint would, therefore be
   double the maximum number of ops per checkpoint.  A checkpoint can
   only be interrupted as the result of violent shutdown and, restart
   will immediately resolve state using the UnsyncdOps.  If restart
   fails to complete, there will be no further log entries or
   checkpoints and the next restart will, worst case, apply the
   state resolution twice which isn't harmful.</p>
 */
public class UnsyncdOps implements BootInfo, java.io.Serializable {
    private int theOpCount;

    UnsyncdOps(int anOpCount) {
        theOpCount = 2 * anOpCount;
    }

    public int getOpsSinceLastCheckpoint() {
        return theOpCount;
    }
}
