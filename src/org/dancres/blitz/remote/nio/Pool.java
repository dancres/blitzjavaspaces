package org.dancres.blitz.remote.nio;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 */
public class Pool {
    private static Executor _executor =
                // new PooledExecutor(new LinkedQueue(), 50);
                new PooledExecutor(50);

    static void execute(Runnable aTask) throws InterruptedException {        
        _executor.execute(aTask);
    }
}
