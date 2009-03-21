package org.dancres.blitz.remote.nio;

import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

/**
 */
public class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable aCommand) {
        Thread myThread = new Thread(aCommand);
        myThread.setDaemon(true);

        return myThread;
    }
}
