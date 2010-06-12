/*
  The copyright of all source code included in this Prevayler distribution is
  held by Klaus Wuestefeld, except the files that specifically state otherwise.
  All rights are reserved. "PREVAYLER" is a trademark of Klaus Wuestefeld.


  BSD License:

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
  - Neither the name of Prevayler nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.


  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
*/

package org.prevayler.implementation;

import java.io.*;

import java.rmi.RMISecurityManager;

import org.prevayler.*;

/** Provides transparent persistence for business objects.
 * This applies to any deterministic system implementing the PrevalentSystem interface.
 * All commands to the system must be represented as objects implementing the Command interface and must be executed
 * using Prevayler.executeCommand(Command).
 * Take a look at the demo application included with the Prevayler distribution for examples.
 */
public class PrevaylerCore {

    private final PrevalentSystem system;
    private final SystemClock clock;
    private final CommandOutputStream output;

    /** Returns a new Prevayler for the given PrevalentSystem.
     * "PrevalenceBase" shall be the directory where the snapshot and log files shall be created and read.
     * @param newSystem The newly started, "empty" PrevalentSystem that will be used as a starting point for every
     * system startup, until the first snapshot is taken.
     * @param shouldReset Whether to issue resets in the underlying OOS
     */
    public PrevaylerCore(PrevalentSystem newSystem,
            boolean shouldReset, boolean shouldClean,
            int aBufferSize)
            throws IOException, ClassNotFoundException {

        this(newSystem, "PrevalenceBase", shouldReset, shouldClean,
                aBufferSize);
    }

    /** Returns a new Prevayler for the given PrevalentSystem.
     * @param newSystem The newly started, "empty" PrevalentSystem that will be used as a starting point for every
     * system startup, until the first snapshot is taken.
     * @param directory The full path of the directory where the snapshot and log files shall be created and read.
     * @param shouldReset Whether to issue resets in the underlying OOS
     */
    public PrevaylerCore(PrevalentSystem newSystem, String directory,
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
        clock = (SystemClock) system.clock();
        clock.resume();
    }

    public PrevaylerCore(PrevalentSystem newSystem, String directory)
            throws IOException, ClassNotFoundException {

        clock = null;
        output = null;

        newSystem.clock(new SystemClock());
        CommandInputStream input = new CommandInputStream(directory);

        PrevalentSystem savedSystem = input.readLastSnapshot();

        if (savedSystem != null) {
            system = savedSystem;
        } else {
            system = newSystem;
        }

        System.out.println("System base clock: " + system.clock().time().getTime());
        Serializable[] myUserData = system.getSnapshotContributions();

        for (int i = 0; i < myUserData.length; i++) {
            System.out.println(myUserData[i].toString());
        }

        debugCommands(input);
    }

    /**
    Returns the underlying PrevalentSystem.
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
    public void logCommand(Command command) throws Exception {
        synchronized (this) {
            output.writeCommand(command);
        }
    }

    public void logCommand(Command command, boolean doSync) throws Exception {
        synchronized (this) {
            output.writeCommand(command, doSync);
        }
    }

    public void flush() throws IOException {
        synchronized(this) {
            output.flush();
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
            return output.writeSnapshot(system);
        } finally {
            clock.resume();
        }
    }

    private void debugCommands(CommandInputStream input) throws IOException, ClassNotFoundException {
        Command command;
        
        while (true) {
            try {
                command = input.readCommand();
            } catch (EOFException eof) {
                break;
            }

            try {
                System.out.println(command);
                System.out.println();
                // command.execute(system);
            } catch (Exception e) {
                //Don't do anything at all. Commands may throw exceptions normally.
                System.err.println("Command threw exception");
                e.printStackTrace(System.err);
            }
        }
    }

    private void recoverCommands(CommandInputStream input) throws IOException, ClassNotFoundException {
        Command command;
        while (true) {
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

    public static void main(String anArgs[]) {
        try {
            System.setSecurityManager(new RMISecurityManager());

            new PrevaylerCore(new DebugSystem(), anArgs[0]);
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }

    static class DebugSystem implements org.prevayler.PrevalentSystem {

        private org.prevayler.AlarmClock theClock;

        public void clock(org.prevayler.AlarmClock clock) {
            theClock = clock;
        }

        public org.prevayler.AlarmClock clock() {
            return theClock;
        }

        public void add(SnapshotContributor aContributor) {
        }

        public void remove(SnapshotContributor aContributor) {
        }

        public Serializable[] getSnapshotContributions() {
            return new Serializable[0];
        }
    }
}
