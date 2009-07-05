package org.dancres.blitz.txn;

import java.io.IOException;
import java.io.Serializable;
import org.prevayler.Command;
import org.prevayler.PrevalentSystem;
import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.PrevaylerCore;
import org.prevayler.implementation.Snapshotter;

public class NullBatcher implements SnapshotPrevayler {
    private PrevaylerCore thePrevayler;

    NullBatcher(PrevaylerCore aPrevayler) {
        thePrevayler = aPrevayler;
    }
    public Snapshotter takeSnapshot() throws IOException {
        return thePrevayler.takeSnapshot();
    }

    public PrevalentSystem system() {
        return thePrevayler.system();
    }

    public Serializable executeCommand(Command aCommand) throws Exception {
        thePrevayler.logCommand(aCommand);
        return aCommand.execute(system());
    }

    public Serializable executeCommand(Command aCommand, boolean sync) throws Exception {
        thePrevayler.logCommand(aCommand, sync);
        return aCommand.execute(system());
    }

}
