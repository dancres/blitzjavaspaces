package org.dancres.blitz.stats;

import java.io.Serializable;

/**
   Client side representation of a stat.  Other fields of stat are only
   available by casting to the appropriate class.
 */
public interface Stat extends Serializable {
    /**
       @return the id of the StatGenerator that produced the stat
     */
    public long getId();
}
