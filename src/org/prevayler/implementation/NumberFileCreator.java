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
import java.text.*;

/** Creates .log and .snapshot files using a number sequence for the file name.
*/
class NumberFileCreator {

    public static final String SNAPSHOT_SUFFIX = "snapshot";
    public static final String LOGFILE_SUFFIX = "commandLog";
    public static final DecimalFormat SNAPSHOT_FORMAT = new DecimalFormat("000000000000000000000'.'" + SNAPSHOT_SUFFIX); //21 zeros (enough for a long number).
    public static final DecimalFormat LOG_FORMAT = new DecimalFormat("000000000000000000000'.'" + LOGFILE_SUFFIX);  //21 zeros (enough for a long number).

    private File directory;
    private long nextFileNumber;

    public NumberFileCreator(File directory, long firstFileNumber) {
	this.directory = directory;
	this.nextFileNumber = firstFileNumber;
	}

    File newLog() throws IOException {
	File log = new File(directory, LOG_FORMAT.format(nextFileNumber));
	if(!log.createNewFile()) throw new IOException("Attempt to create command log file that already existed: " + log);;

	++nextFileNumber;
	return log;
    }

    File newSnapshot() throws IOException {
	File snapshot = new File(directory, SNAPSHOT_FORMAT.format(nextFileNumber - 1));
	snapshot.delete();   //If no commands are logged, the same snapshot file will be created over and over.
	return snapshot;
    }

    File newTempSnapshot() throws IOException {
	return File.createTempFile("temp","generatingSnapshot",directory);
    }

    NumberFileCleaner newCleaner() {
        return new NumberFileCleaner(directory);
    }
}
