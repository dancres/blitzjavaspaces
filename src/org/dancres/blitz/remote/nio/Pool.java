package org.dancres.blitz.remote.nio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class Pool {
    private static ExecutorService _executor = Executors.newFixedThreadPool(50);

    static void execute(Runnable aTask) throws InterruptedException {        
        _executor.execute(aTask);
    }
}
