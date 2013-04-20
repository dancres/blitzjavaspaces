package org.dancres.blitz.notify;

import java.io.IOException;

import java.util.logging.*;

import net.jini.space.JavaSpace;

import org.dancres.blitz.task.Task;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.mangler.MangledEntry;

/**
   <p> RemoteEvent dispatch occurs here.  The generator is locked during
   generation of the RemoteEvent and any logging that may be required. </p>

   <p> If we fail to deliver the event to the client we take some special
   action.  First we "taint" the generator to prevent further events being
   processed/dispatched, then we schedule a cleanup task to remove the
   registration associated with the generator. </p>
 */
class SendTask implements Task {
    private EventGenerator theGenerator;
    private JavaSpace theSource;
    private DispatchTask theTask;

    SendTask(JavaSpace aSource, DispatchTask aTask,
             EventGenerator aGenerator) {

        theSource = aSource;
        theGenerator = aGenerator;
        theTask = aTask;
    }

    public void run() {
        theGenerator.ping(theTask.getEvent(), theSource);
        theTask.dispatched();
    }
}
