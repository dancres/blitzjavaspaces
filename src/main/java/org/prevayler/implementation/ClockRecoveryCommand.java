package org.prevayler.implementation;

import org.prevayler.*;

import java.io.Serializable;

import java.util.Date;

/**
   A command for executing another command at a specific moment in time.
*/
class ClockRecoveryCommand implements Command {
    static final long serialVersionUID = 4156866783673937422L;

	private Command command;
	private long millis;

	public ClockRecoveryCommand(Command command, Date date) {
		this.command = command;
		this.millis = date.getTime();
	}

	public Serializable execute(PrevalentSystem system) throws Exception {
		((SystemClock)system.clock()).recover(millis);
		return command.execute(system);
	}

    public String toString() {
        return millis + ":" + command;
    }

    public long getMillis() {
        return millis;
    }
}
