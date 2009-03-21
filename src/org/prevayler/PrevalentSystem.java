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
 * A system that implements this interface can be made transparently persistent with Prevayler. All business objects in this system must be deterministic. i.e.: Any two PrevalentSystems of the same class must produce the same results given the same commands.
 * @see org.prevayler.Prevayler
 */
public interface PrevalentSystem extends Serializable {

  /**
   * Sets the clock to be used by this system for ALL its date/time related functions. This method is called only once by Prevayler during the first system initialization.
   */
  public void clock(AlarmClock clock);

  /**
   * Returns the clock used by this system for ALL its date/time related functions.
   */
  public AlarmClock clock();

    public void add(SnapshotContributor aContributor);

    /**
       This method would typically be used, for example, when something is
       about to be deleted or freed (perhaps as part of delete all Entry's)
       and thus doesn't wish to provide contributions any longer
     */
    public void remove(SnapshotContributor aContributor);

    public Serializable[] getSnapshotContributions();
}
