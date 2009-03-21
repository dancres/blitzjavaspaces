package org.dancres.blitz.remote.test;

import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

/**
 */
public class StressReap {

    // Logistics variables
    private int m_numberOfIterations;
    private int m_numberOfThreads;
    private long m_startTime;

    // Not the best barrier, but it works for this crude test
    private int m_runningThreads;

    private LocalSpace _space;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java StressReap [numThreads] [numIterations]");
            return;
        }

        // Do it...
        new StressReap(Integer.parseInt(args[0]),
            Integer.parseInt(args[1])).run();
    }

    public StressReap(int threads, int iterations) throws Exception {
        System.out.println("Threads: " + threads);
        System.out.println("Iterations: " + iterations);

        m_numberOfThreads = threads;
        m_numberOfIterations = iterations;
    }

    /**
     * Fires off all of the workers and then waits for all of them to finish.
     */
    public void run() throws Exception {
        // Start count at top and let workers decrement when finished
        m_runningThreads = m_numberOfThreads;

        // When we started the whole thing
        m_startTime = System.currentTimeMillis();

        for (int i = 0; i < m_numberOfThreads; i++) {
            Thread t = new Worker();
            t.setName("thread" + i);
            t.start();
        }

        // Wait for workers to finish
        while (true) {
            Thread.sleep(1000);
            if (m_runningThreads == 0)
                break;
        }
    }

    // The worker that actually does work...
    private class Worker extends Thread {
        private StressReapItem [] m_templates;

        public Worker() {
            // Init some basic templates of varying sizes of random strings
            m_templates = new StressReapItem[]
                {new StressReapItem("a", 1024),
                    new StressReapItem("b", 2048),
                    new StressReapItem("c", 4096)};
        }

        /**
         * Get the test running. The real work is done by runTest() but this ensures
         * that regardless of what happens, we decrease our barrier count
         */
        public void run() {
            try {
                System.out.println("Thread " + Thread.currentThread().getName() + " starting.");
                runTest();
            }
            finally {
                m_runningThreads--;
            }
        }

        /**
         * The true workhorse of the tester. This grabs one of our 1K, 2K or 4K entries at
         * random and writes it into the space.
         */
        public void runTest() {
            // keep a rough approximation of how much this thread added
            long totalBytesInSpace = 0;

            // Blast through all of the writes
            for (int i = 0; i < m_numberOfIterations; i++) {
                try {
                    StressReapItem template = m_templates[(int) (Math.random() * 3)];
                    totalBytesInSpace += template.m_value.length();

                    // Rename to a unique key (bad style, I know, but this is a hack of a test)
                    template.m_key = Thread.currentThread().getName() + i;
                    long writeTime = doWrite(template);
                    long totalTime = System.currentTimeMillis() - m_startTime;

                    // Do an immediate take on the most recently added entry  to simulate
                    // our messaging request/response behavior
                    //StressReapItem findMe = new StressReapItem();
                    //findMe.m_key = template.m_key;
                    //findMe.m_value = null;
                    //long readTime = doTake(findMe);


                    String log = "";
                    log += "Thread: " + Thread.currentThread().getName() + " ";
                    log += "Iteration: " + i + " ";
                    log += "Space consumed: " + (totalBytesInSpace / 1024 / 1024) + "MB ";
                    log += "Write time: " + String.valueOf(writeTime) + "ms ";
                    //log += "Read time: " + String.valueOf(readTime) + "ms ";
                    log += "Total time: " + String.valueOf(toMinutes(totalTime)) + " minutes";
                    System.out.println(log);

                    Thread.sleep(1000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Calculate the number of minutes to 2 decimal points
         *
         * @param ms A number of milliseconds
         * @return The number of minutes, rounded to 2 decimal places
         */
        private double toMinutes(long ms) {
            double minutes = ms / 60000.0;
            minutes = minutes * 100.0;
            minutes = Math.floor(minutes);
            minutes = minutes / 100.0;
            return minutes;
        }

        /**
         * Write the message for an hour and determine how long it took
         *
         * @param template What to write to the space
         * @return How long the write operation took (excluding service lookup time)
         */
        private long doWrite(StressReapItem template) throws Exception {
            JavaSpace space = getSpace();
            long startTime = System.currentTimeMillis();
            space.write(template, null, 1000 * 60);   // live for 1 minute
            long endTime = System.currentTimeMillis();
            return endTime - startTime;
        }

        /**
         * Take an entry from the space and see how long it took
         *
         * @param template The entry template to use for the space lookup
         * @return How long the write operation took (excluding service lookup time)
         */
        private long doTake(StressReapItem template)
            throws Exception {
            JavaSpace space = getSpace();
            long startTime = System.currentTimeMillis();
            template = (StressReapItem) space.take(template, null, 30000);
            long endTime = System.currentTimeMillis();
            return endTime - startTime;
        }
    }

    // ================================================================================
    // Below here is just code pulled from our ServiceLocator to locate
    // the space service
    // ================================================================================

    protected synchronized JavaSpace getSpace() throws Exception {
        if (_space == null) {
            _space = new LocalSpace();
        }
        return _space.getJavaSpaceProxy();
    }
}
