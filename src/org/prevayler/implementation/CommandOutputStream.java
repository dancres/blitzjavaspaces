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

import org.prevayler.*;
import java.io.*;

/** Provides a simple API for writing commands and snapshots.
 */
class CommandOutputStream {

    /** This number determines the size of the log files produces by the system.
     */
    public static final long LOG_FILE_SIZE = 100L * 1024L * 1024L;
    private final NumberFileCreator fileCreator;
	private ObjectOutputStream logStream;
    private DelegatingByteCountStream2 fileStream;
    private boolean doReset;
    private boolean shouldClean;

    private int theBufferSize = 0;

    public CommandOutputStream(NumberFileCreator fileCreator,
                               boolean shouldReset, boolean shouldClean) {
        this(fileCreator, shouldReset, shouldClean, 0);
    }

    public CommandOutputStream(NumberFileCreator fileCreator,
                               boolean shouldReset, boolean doClean,
                               int bufferSize) {
        this.fileCreator = fileCreator;
        doReset = shouldReset;
        theBufferSize = bufferSize;
        shouldClean = doClean;
    }

    public void writeCommand(Command command) throws IOException{
        ObjectOutputStream oos = logStream();
        try {
            // long myStart = System.currentTimeMillis();

            oos.writeObject(command);

            if (doReset)
                oos.reset();    //You can comment this line if your free RAM is large compared to the size of each commandLog file. If you comment this line, commands will occupy much less space in the log file because their class descriptions will only be written once. Your application will therefore produce much fewer log files. If you comment this line, you must make sure that no command INSTANCE is used more than once in your application with different internal values. "Reset will disregard the state of any objects already written to the stream. The state is reset to be the same as a new ObjectOutputStream. ... Objects previously written to the stream will not be refered to as already being in the stream. They will be written to the stream again." - JDK1.2.2 API documentation.
            oos.flush();

            // long myEnd = System.currentTimeMillis();

            // System.err.println("Log command time: " + (myEnd - myStart));
        } catch (IOException iox) {
            closeLogStream();
            throw iox;
        }
	}

    public void flush() throws IOException {
        ObjectOutputStream oos = logStream();
        oos.flush();
    }

    public void writeCommand(Command command, boolean doSync)
        throws IOException{

        ObjectOutputStream oos = logStream();
        try {
            // long myStart = System.currentTimeMillis();

            oos.writeObject(command);

            if (doReset)
                oos.reset();    //You can comment this line if your free RAM is large compared to the size of each commandLog file. If you comment this line, commands will occupy much less space in the log file because their class descriptions will only be written once. Your application will therefore produce much fewer log files. If you comment this line, you must make sure that no command INSTANCE is used more than once in your application with different internal values. "Reset will disregard the state of any objects already written to the stream. The state is reset to be the same as a new ObjectOutputStream. ... Objects previously written to the stream will not be refered to as already being in the stream. They will be written to the stream again." - JDK1.2.2 API documentation.

            if (doSync)
                oos.flush();

            // long myEnd = System.currentTimeMillis();

            // System.err.println("Log command time: " + (myEnd - myStart));

        } catch (IOException iox) {
            closeLogStream();
            throw iox;
        }
    }

    public synchronized Snapshotter writeSnapshot(PrevalentSystem system)
        throws IOException{

        closeLogStream();    //After every snapshot, a new commandLog file must be started.

        SnapshotterImpl mySnapper = new SnapshotterImpl(fileCreator,
                                                        shouldClean);
        mySnapper.cacheSnapshot(system);

        return mySnapper;
    }

	private ObjectOutputStream logStream() throws IOException{
        if(logStream == null) {
            fileStream =
                new DelegatingByteCountStream2(fileCreator.newLog(),
                                               theBufferSize);
            logStream = new ObjectOutputStream(fileStream);
		}

        if(fileStream.bytesWritten() >= LOG_FILE_SIZE) {
            closeLogStream();
            return logStream();  //Recursive call.
		}

        return logStream;
	}

	private void closeLogStream() throws IOException {
        if (logStream == null) return;

        logStream.close();
        logStream = null;
    }

}
