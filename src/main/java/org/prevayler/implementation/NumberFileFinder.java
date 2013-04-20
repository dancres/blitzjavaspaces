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
import java.util.Arrays;
import java.text.*;

/** Finds the last .snapshot file by number and finds all the subsequent pending .log files.
*/
class NumberFileFinder {

    private File directory;
	private File lastSnapshot;
    private long fileNumber;
    private NumberFileCreator fileCreator;

    /** @throws IOException if location does not exist and cannot be created as a directory.
    * @throws IllegalArgumentException If location exists and is not a directory.
    */
    public NumberFileFinder(String directoryName) throws IOException {
	this.directory = new File(directoryName);
	if (!directory.exists() && !directory.mkdirs()) throw new IOException("Directory doesn't exist and could not be created: " + directoryName);
	if (!directory.isDirectory()) throw new IllegalArgumentException("Path exists but is not a directory: " + directoryName);

	init();
	}

    /** Returns the last snapshot file. Returns null if there are no snapshots.
    */
    public File lastSnapshot() {
	return lastSnapshot;
	}

    /** @throws EOFException if there are no more pending .log files.
    */
    public File nextPendingLog() throws EOFException {
	File log = new File(directory, NumberFileCreator.LOG_FORMAT.format(fileNumber + 1));
	if (!log.exists()) {
	    fileCreator = new NumberFileCreator(directory, fileNumber + 1);
	    throw new EOFException();
	}
	++fileNumber;
	return log;
	}

    /** Returns a NumberFileCreator that will start off with the first number after the last .log file number.
    * @return null if there are still .log files pending.
    */
    public NumberFileCreator fileCreator() {
	return fileCreator;
    }

    private void init() throws IOException {
	findLastSnapshot();
	fileNumber = lastSnapshot == null
	    ? 0
	    : number(lastSnapshot);
	}

    private long number(File snapshot) throws NumberFormatException {  //NumberFomatException is a RuntimeException.
	String name = snapshot.getName();
	if (!name.endsWith("." + NumberFileCreator.SNAPSHOT_SUFFIX)) throw new NumberFormatException();
	return Long.parseLong(name.substring(0,name.indexOf('.')));    // "00000.snapshot" becomes "00000".
	//The following doesn't work! It throws ParseException (UnparseableNumber): return (NumberFileCreator.SNAPSHOT_FORMAT.parse(snapshot.getName())).longValue();
	}

    private void findLastSnapshot() throws IOException {
	File[] snapshots = directory.listFiles(new SnapshotFilter());
	if (snapshots == null) throw new IOException("Error reading file list from directory " + directory);

	Arrays.sort(snapshots);

	lastSnapshot = snapshots.length > 0
	    ? snapshots[snapshots.length - 1]
	    : null;
	}


    private class SnapshotFilter implements FileFilter {

	public boolean accept(File file) {
	    try {
		number(file);
	    } catch (NumberFormatException nfx) {
		return false;
	    }
	    return true;
		}
	}

}
