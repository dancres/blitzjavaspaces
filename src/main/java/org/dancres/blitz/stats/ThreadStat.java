package org.dancres.blitz.stats;

public class ThreadStat implements Stat, StatGenerator {
    private long theId = StatGenerator.UNSET_ID;
    private int theThreadCount = 0;

    ThreadStat() {
    }

    ThreadStat(long anId, int aThreadCount) {
        theId = anId;
        theThreadCount = aThreadCount;
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public synchronized Stat generate() {
        ThreadGroup myGroup = Thread.currentThread().getThreadGroup();

        int myActiveCount = myGroup.activeCount();

        while ((!myGroup.getName().equals("main")) &&
            (myGroup = myGroup.getParent()) != null) {
            myActiveCount += myGroup.activeCount();
        }

        return new ThreadStat(theId, myActiveCount);
    }

    public int getThreadCount() {
        return theThreadCount;
    }

    public String toString() {
        return "Thread count: " + theThreadCount;
    }
}
