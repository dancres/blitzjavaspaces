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

package org.prevayler;

import java.io.Serializable;


/**
   Provides transparent persistence for all business objects in a
   PrevalentSystem. All commands to the system must be represented as objects
   implementing the Command interface and must be executed using
   Prevayler.executeCommand(Command).
 */
public interface Prevayler {

    /**
     * Returns the underlying PrevalentSystem.
     */
    public PrevalentSystem system();


    /**
     Logs the received command for crash or shutdown recovery and executes it
     on the underlying PrevalentSystem.

     @return The serializable object that was returned by the execution of command.
     @throws IOException if there is trouble writing the command to one of the
     log files.
     @throws Exception if the execution of command throws an Exception.
    */
    public Serializable executeCommand(Command command) throws Exception;

    /**
       @param sync if <code>true</code>indicates that the log should be forced
       after this command has been written otherwise the don't force.
     */
    public Serializable executeCommand(Command aCommand, boolean sync) throws Exception;
}
