package org.dancres.blitz.stats;

/**
   Registered with the StatsBoard and responsible for generating an instance
   of an appropriate Stat for passing to a client asking for stats dump.

   @see org.dancres.blitz.stats.Stat
 */
public interface StatGenerator {
    public static final long UNSET_ID = -1;

    /**
       @return the id of the StatGenerator that produced the stat
       AdministrableStat.UNSET_ID if the id has never been set
     */
    public long getId();

    public void setId(long anId);

    public Stat generate();
}
