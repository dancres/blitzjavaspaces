package org.dancres.blitz;

import org.dancres.blitz.stats.BlockingOpsStat;
import org.dancres.blitz.stats.MissedOpsStat;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.StatsBoard;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the number ouf outstanding blocked operations (take and read)
 */
class SearchTasks {
    private static class LifecycleImpl implements Lifecycle {
        public void init() {
            theTasks = new SearchTasks();
        }

        public void deinit() {
            theTasks = null;
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    private static SearchTasks theTasks;

    private AtomicInteger theTakeCount = new AtomicInteger();
    private AtomicInteger theReadCount = new AtomicInteger();
    private AtomicLong theMissedTakes = new AtomicLong();
    private AtomicLong theMissedReads = new AtomicLong();

    public static SearchTasks get() {
        return theTasks;
    }

    private SearchTasks() {
        StatsBoard.get().add(new BlockingStatGenerator());
        StatsBoard.get().add(new MissedStatGenerator());
    }

    public void add(MatchTask aTask) {
        if (aTask.getVisitor().isDeleter())
            theTakeCount.incrementAndGet();
        else
            theReadCount.incrementAndGet();
    }

    public void remove(MatchTask aTask, boolean didMiss) {
        if (aTask.getVisitor().isDeleter()) {
            theTakeCount.decrementAndGet();
            
            if (didMiss)
                theMissedTakes.incrementAndGet();
        } else {
            theReadCount.decrementAndGet();

            if (didMiss)
                theMissedReads.incrementAndGet();
        }
    }

    private class BlockingStatGenerator implements StatGenerator {
        private long theStatId = StatGenerator.UNSET_ID;

        public long getId() {
            return theStatId;
        }
        
        public void setId(long anId) {
            theStatId = anId;
        }
        
        public Stat generate() {
            return new BlockingOpsStat(
                    theStatId, theReadCount.intValue(), theTakeCount.intValue());
        }
    }

    private class MissedStatGenerator implements StatGenerator {
        private long theStatId = StatGenerator.UNSET_ID;

        public long getId() {
            return theStatId;
        }
        
        public void setId(long anId) {
            theStatId = anId;
        }
        
        public Stat generate() {
            return new MissedOpsStat(
                    theStatId, theMissedReads.longValue(), 
                    theMissedTakes.longValue());
        }
    }
}
