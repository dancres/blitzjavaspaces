package org.prevayler.implementation;

import java.io.IOException;
import java.io.Serializable;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Prevayler which logs nothing but dispatches all commands.
 */
public class NullPrevayler implements SnapshotPrevayler {

    private PrevalentSystem theSystem;

    private Snapshotter theNullSnapshotter = new NullSnapshotter();

    public NullPrevayler(PrevalentSystem aSystem) {
        theSystem = aSystem;
        theSystem.clock(new SystemClock());
    }

    public PrevalentSystem system() {
        return theSystem;
    }

    public Serializable executeCommand(Command command) throws Exception {
        return command.execute(theSystem);
    }

    public Serializable executeCommand(Command command, boolean sync)
        throws Exception {
        return command.execute(theSystem);
    }

    public synchronized Snapshotter takeSnapshot() throws IOException {
        return theNullSnapshotter;
    }

    private static class NullSnapshotter implements Snapshotter {
        NullSnapshotter() {
        }

        public void save() throws IOException {
            // Do nothing
        }
    }
}
