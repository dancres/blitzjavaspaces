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
import org.prevayler.*;

/** Provides an easy API for reading commands and snapshots.
*/
class CommandInputStream {

    public CommandInputStream(String directory) throws IOException {
	fileFinder = new NumberFileFinder(directory);
    }

    public PrevalentSystem readLastSnapshot() throws IOException, ClassNotFoundException {
	File snapshotFile = fileFinder.lastSnapshot();
	if (snapshotFile == null) return null;
	out(snapshotFile);

	ObjectInputStream ois = new ObjectInputStream(new FileInputStream(snapshotFile));
	try {
	    return (PrevalentSystem)ois.readObject();
	} finally {
	    ois.close();
	}
    }

    public Command readCommand() throws IOException, ClassNotFoundException {
	if (currentLogStream == null) currentLogStream = newLogStream();  //Throws EOFException if there are no more log streams.

	try {
	    return (Command)currentLogStream.readObject();
	} catch (EOFException eof) {
	    //No more commands in this file.
	} catch (ObjectStreamException osx) {
	    logStreamExceptionMessage(osx);
	} catch (RuntimeException rx) {
	    logStreamExceptionMessage(rx);    //Some stream corruptions cause runtime exceptions!
	}

	currentLogStream.close();
	currentLogStream = null;
	return readCommand();
    }

    public CommandOutputStream commandOutputStream(boolean shouldReset,
                                                   boolean shouldClean,
                                                   int aBufferSize) {
	return new CommandOutputStream(fileFinder.fileCreator(), shouldReset,
                                   shouldClean, aBufferSize);
    }

    private ObjectInputStream newLogStream() throws IOException {
	File logFile = fileFinder.nextPendingLog();  //Throws EOFException if there are no more pending log files.
	out(logFile);
	return new ObjectInputStream(new BufferedInputStream(new FileInputStream(logFile)));
    }

    private void logStreamExceptionMessage(Exception exception) {
	out("   " + exception);
	out("   Some commands might have been lost. Looking for the next file..." );
    }

    private static void out(File file) {
	out("Reading: " + file + "...");
    }

    private static void out(Object obj) {
	System.out.println(obj);
    }

    private NumberFileFinder fileFinder;
    private ObjectInputStream currentLogStream;
}
