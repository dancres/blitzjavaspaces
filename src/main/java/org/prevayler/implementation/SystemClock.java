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
import java.util.Date;

/**
 * An AlarmClock that uses the local system clock as its current-time source.
 * This class can be extended so that other time sources can be used.
 * @see #currentTimeMillis()
 */
public class SystemClock implements AlarmClock {

  private Date time;
  private long millis;
  private boolean isPaused = true;


  /** 
   * A newly created SystemClock starts off paused with time() equal to new Date(Long.MIN_VALUE).
   */
  public SystemClock() {
    set(new Date(Long.MIN_VALUE));
  }


  public synchronized Date time() {
    if (isPaused) return time;

    long currentMillis = currentTimeMillis();
    if (currentMillis != millis) {
      set(new Date(currentMillis));
    }

    return time;
  }


  /**
   * Causes time() to return always the same value as if the clock had stopped.
   * The clock does NOT STOP internally though. This method is called by Prevayler before each Command is executed so that it can be executed in a known moment in time.
   * @see #resume()
   */
  synchronized void pause() {
    if (isPaused) throw new IllegalStateException("AlarmClock was already paused.");

    time();           //Guarantees the time is up-to-date.
    isPaused = true;
  }


  /**
   * Causes time() to return the current time again. This method is called by Prevayler after each Command is executed so that the clock can start running again.
   * @see #pause()
   */
  synchronized void resume() {
    if (!isPaused) throw new IllegalStateException("AlarmClock was not paused.");

    isPaused = false;
  }


  /**
   * Sets the time forward, recovering some of the time that was "lost" since the clock was paused. The clock must be paused. This method is called by Prevayler when recovering commands from the commandLog file so that they can be re-executed in the "same" time as they had been originally.
   * @param newMillis the new time in milliseconds. Cannot be earlier than time().getTime() and cannot be later than currentTimeMillis().
   */
  synchronized void recover(long newMillis) {
    if (!isPaused) throw new IllegalStateException("AlarmClock must be paused for recovering.");

    if (newMillis == millis) return;
    if (newMillis < millis) throw new IllegalArgumentException("AlarmClock's time cannot be set backwards.");
    if (newMillis > currentTimeMillis()) throw new IllegalArgumentException("AlarmClock's time cannot be set after the current time.");

    set(new Date(newMillis));
  }


  /**
   * Returns System.currentTimeMillis(). Override this method if you want to use a different time source for your system.
   */
  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }


  private void set(Date time) {
    this.time = time;
    this.millis = time.getTime();

    // System.out.println("Time set to: " + millis);
  }

}
