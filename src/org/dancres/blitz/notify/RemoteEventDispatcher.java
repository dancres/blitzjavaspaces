package org.dancres.blitz.notify;

import java.util.logging.*;

import net.jini.space.JavaSpace;

import org.dancres.blitz.task.Tasks;

import org.dancres.blitz.Logging;

import org.dancres.blitz.remote.NullJavaSpace;

/**
   Converts internal events into external RemoteEvents and arranges for
   their dispatch.
*/
public class RemoteEventDispatcher {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.notify.RemoteEventDispatcher");


    private static JavaSpace theSource = new NullJavaSpace();

    /**
       Used to configure the EventGenerator with an appropriate remote
       source reference for passing out with each event
     */
    public static synchronized void setSource(JavaSpace aSource) {
        theSource = aSource;
    }

    RemoteEventDispatcher() {
    }

    void sendEvent(DispatchTask aTask, EventGenerator aGenerator) {
        try {
            aTask.newDispatch();
            Tasks.queue("RemoteEvent",
                    new SendTask(theSource, aTask, aGenerator));
        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to add event to queue",
                          anIE);
        }
    }
}
