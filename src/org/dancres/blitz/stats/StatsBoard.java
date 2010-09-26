package org.dancres.blitz.stats;

import java.util.LinkedList;
import java.util.Iterator;

import java.util.logging.Logger;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.Logging;

/**
   All available performance statistics are held here.
 */
public class StatsBoard {
    static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.stats.StatsBoard");

    private static StatsBoard theBoard = new StatsBoard();

    private LinkedList theStatGenerators = new LinkedList();

    private long theNextId = 1;

    private StatsBoard() {
        add(new MemoryStat());
        add(new HostStat());
        add(new ThreadStat());
    }

    private static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            theBoard.theStatGenerators.clear();
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }
    
    public static StatsBoard get() {
        return theBoard;
    }

    public synchronized void add(StatGenerator aStat) {
        if (! theStatGenerators.contains(aStat)) {
            if (aStat.getId() == StatGenerator.UNSET_ID)
                aStat.setId(theNextId++);

            theStatGenerators.add(aStat);
        }
    }

    public synchronized void remove(StatGenerator aStat) {
        theStatGenerators.remove(aStat);
    }

    public Stat[] getStats() {
        StatGenerator[] myGenerators;

        synchronized(this) {
            myGenerators = new StatGenerator[theStatGenerators.size()];
            myGenerators =
                    (StatGenerator[]) theStatGenerators.toArray(myGenerators);
        }

        Stat[] myStats = new Stat[myGenerators.length];

        for (int i = 0; i < myGenerators.length; i++) {
            myStats[i] = myGenerators[i].generate();
        }

        return myStats;
    }
}
