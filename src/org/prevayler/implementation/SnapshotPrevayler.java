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

import java.util.Date;
import java.io.*;

import org.prevayler.*;

/** Provides transparent persistence for business objects.
* This applies to any deterministic system implementing the PrevalentSystem interface.
* All commands to the system must be represented as objects implementing the Command interface and must be executed using Prevayler.executeCommand(Command).
* Take a look at the demo application included with the Prevayler distribution for examples.
* @see PrevaylerFactory
*/
public interface SnapshotPrevayler extends Prevayler {
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
	public Snapshotter takeSnapshot() throws IOException;
}


