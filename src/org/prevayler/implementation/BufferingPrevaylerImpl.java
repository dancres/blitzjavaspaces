package org.prevayler.implementation;

import java.util.LinkedList;
import java.util.Date;
import java.io.*;

import org.prevayler.*;

import org.dancres.blitz.ActiveObject;
import org.dancres.blitz.ActiveObjectRegistry;

/**
   <p>Prevayler which guarentees that all logged changes will be made
   persistent within a certain fixed amount of time - e.g. after 5 seconds.
   When a snapshot is requested, any logged changes will be flushed to disk and
   then a snapshot will be prepared.  This provides correct recovery should
   we crash before we save the snapshot (which could be a while) but after
   we've flushed the log.</p>

   <p>This Prevayler is designed to work in co-operation with Blitz's
   OpCountingCheckpointTrigger and the ActiveObjectRegistry.</p>

   @see org.dancres.blitz.txn.OpCountingCheckpointTrigger
   @see org.dancres.blitz.ActiveObjectRegistry
 */
public class BufferingPrevaylerImpl implements SnapshotPrevayler {

	private final PrevalentSystem system;
	private final SystemClock clock;
	private final CommandOutputStream output;

    private LinkedList theBuffer = new LinkedList();

	/** Returns a new Prevayler for the given PrevalentSystem.
	* "PrevalenceBase" shall be the directory where the snapshot and log files shall be created and read.
	* @param newSystem The newly started, "empty" PrevalentSystem that will be used as a starting point for every system startup, until the first snapshot is taken.
    * @param shouldReset Whether to issue resets in the underlying OOS
	*/
	public BufferingPrevaylerImpl(PrevalentSystem newSystem,
                                  boolean shouldReset, boolean shouldClean,
                                  int aBufferSize)
        throws IOException, ClassNotFoundException {

		this(newSystem, "PrevalenceBase", shouldReset, shouldClean,
             aBufferSize);
	}


	/** Returns a new Prevayler for the given PrevalentSystem.
	* @param newSystem The newly started, "empty" PrevalentSystem that will be used as a starting point for every system startup, until the first snapshot is taken.
	* @param directory The full path of the directory where the snapshot and log files shall be created and read.
    * @param shouldReset Whether to issue resets in the underlying OOS
	*/
	public BufferingPrevaylerImpl(PrevalentSystem newSystem, String directory,
                                  boolean shouldReset, boolean shouldClean,
                                  int aBufferSize)
        throws IOException, ClassNotFoundException {

		newSystem.clock(new SystemClock());
		CommandInputStream input = new CommandInputStream(directory);

		PrevalentSystem savedSystem = input.readLastSnapshot();
		system = (savedSystem == null)
		    ? newSystem
		    : savedSystem;

		recoverCommands(input);

		output = input.commandOutputStream(shouldReset, shouldClean,
                                           aBufferSize);
		clock = (SystemClock)system.clock();
		clock.resume();
	}


	/** Returns the underlying PrevalentSystem.
	*/
	public PrevalentSystem system() {
		return system;
	}

	/** Logs the received command for crash or shutdown recovery and executes it on the underlying PrevalentSystem.
	* @see system()
	* @return The serializable object that was returned by the execution of command.
	* @throws IOException if there is trouble writing the command to the log.
	* @throws Exception if command.execute() throws an exception.
	*/
	public synchronized Serializable executeCommand(Command command) throws Exception {
		clock.pause();  //To be deterministic, the system must know exactly at what time the command is being executed.
		try {
            theBuffer.add(new ClockRecoveryCommand(command, clock.time()));

			return command.execute(system);

		} finally {
			clock.resume();
		}
	}

	public synchronized Serializable executeCommand(Command command,
                                                    boolean doSync)
        throws Exception {

		clock.pause();
		try {
            theBuffer.add(new ClockRecoveryCommand(command, clock.time()));

			return command.execute(system);

		} finally {
			clock.resume();
		}
	}

    /**
     * This method prepares a snapshot of the system and returns it in a
     * Snapshotter instance which can be used to save the snapshot to disk
     * once dirty state has been sync'd to disk.  If your application has no
     * additional state, you can simply invoke on the Snapshotter immediately.
    * @return Snapshotter to be used to save an appropriate snapshot post
    * sync'ing of dirty state to disk.
	* @see system()
	* @throws IOException if there is trouble preparing the snapshot file.
	*/
	public synchronized Snapshotter takeSnapshot() throws IOException {
		clock.pause();
		try {
            flush();

			return output.writeSnapshot(system);
		} finally {
			clock.resume();
		}
	}


    private synchronized void flush() throws IOException {
        while (! theBuffer.isEmpty()) {
            Command myCommand = (Command) theBuffer.remove(0);

            output.writeCommand(myCommand, theBuffer.isEmpty());
        }
    }

	private void recoverCommands(CommandInputStream input) throws IOException, ClassNotFoundException {
		Command command;
		while(true) {
			try {
				command = input.readCommand();
			} catch (EOFException eof) {
				break;
			}

			try {
				command.execute(system);
			} catch (Exception e) {
				//Don't do anything at all. Commands may throw exceptions normally.
                System.err.println("Command threw exception");
                e.printStackTrace(System.err);
			}
		}
	}
}

