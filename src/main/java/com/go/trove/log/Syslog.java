/* ====================================================================
 * Trove - Copyright (c) 1997-2000 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */

package com.go.trove.log;

import java.io.*;

/******************************************************************************
 * Contains references to a static Log instance that can be used for general
 * system-wide logging. By default, log messages are written to System.out
 * or System.err. When "installed", all output provided to System.out
 * and System.err is redirected into the system log.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 * @see Log
 */
public class Syslog {
    private static final Log cLog;
    private static final LogListener cSystemLogEventPrinter;

    private static LogEventParsingOutputStream cSystemOut;
    private static LogEventParsingOutputStream cSystemErr;

    private static PrintStream cOriginalOut;
    private static PrintStream cOriginalErr;
    private static boolean cInstalled;

    static {
        cLog = new Log(null, null);
        cLog.setDescription("System Log");

        cSystemLogEventPrinter = new LogListener() {
            public void logMessage(LogEvent e) {
                PrintStream ps = getPrintStream(e);
                String message = e.getMessage();
                if (message == null) {
                    ps.println();
                }
                else {
                    ps.println(message);
                }
            }
            
            public void logException(LogEvent e) {
                Throwable t = e.getException();
                if (t == null) {
                    logMessage(e);
                }
                else {
                    t.printStackTrace(getPrintStream(e));
                }
            }
                
            private PrintStream getPrintStream(LogEvent e) {
                synchronized (log()) {
                    switch (e.getType()) {
                        case LogEvent.DEBUG_TYPE:
                        case LogEvent.INFO_TYPE:
                        return (cInstalled) ? cOriginalOut : System.out;
                    }
                    
                    return (cInstalled) ? cOriginalErr : System.err;
                }
            }
        };
        
        log().addLogListener(cSystemLogEventPrinter);
    }

    /** 
     * Returns the system Log instance that, by default, only has one
     * LogListener. It prints debug and info LogEvent messages to System.out
     * and other LogEvent messages to System.err.
     *
     * @see #getSystemLogEventPrinter()
     */
    public static Log log() {
        return cLog;
    }

    /** 
     * Returns a simple LogListener that prints debug and info LogEvent
     * messages to System.out and other LogEvent messages to System.err.
     * Remove this listener to disable printing to System.out and System.err.
     */
    public static LogListener getSystemLogEventPrinter() {
        return cSystemLogEventPrinter;
    }

    /**
     * When installed, System.out and System.err are redirected to Syslog.log.
     * System.out produces info events, and System.err produces error events.
     */ 
    public static void install() {
        synchronized (log()) {
            if (!cInstalled) {
                cInstalled = true;
                cOriginalOut = System.out;
                cOriginalErr = System.err;
                
                cSystemOut = new LogEventParsingOutputStream
                    (log(), LogEvent.INFO_TYPE) 
                {
                    public boolean isEnabled() {
                        return log().isInfoEnabled();
                    }
                };
                cSystemOut.addLogListener(log());
                System.setOut(new PrintStream(cSystemOut, true));

                cSystemErr = new LogEventParsingOutputStream
                    (log(), LogEvent.ERROR_TYPE) 
                {
                    public boolean isEnabled() {
                        return log().isErrorEnabled();
                    }
                };
                cSystemErr.addLogListener(log());
                System.setErr(new PrintStream(cSystemErr, true));
            }
        }
    }

    /**
     * Uninstalls by restoring System.out and System.err.
     */
    public static void uninstall() {
        synchronized (log()) {
            if (cInstalled) {
                cInstalled = false;
                System.setOut(cOriginalOut);
                System.setErr(cOriginalErr);
                cOriginalOut = null;
                cOriginalErr = null;
                cSystemOut = null;
                cSystemErr = null;
            }
        }
    }

    /**
     * Shortcut to {@link Log#debug(String) Syslog.log().debug(String)}.
     */
    public static void debug(String s) {
        log().debug(s);
    }

    /**
     * Shortcut to {@link Log#debug(Throwable) Syslog.log().debug(Throwable)}.
     */
    public static void debug(Throwable t) {
        log().debug(t);
    }

    /**
     * Shortcut to {@link Log#info(String) Syslog.log().info(String)}.
     */
    public static void info(String s) {
        log().info(s);
    }

    /**
     * Shortcut to {@link Log#info(Throwable) Syslog.log().info(Throwable)}.
     */
    public static void info(Throwable t) {
        log().info(t);
    }

    /**
     * Shortcut to {@link Log#warn(String) Syslog.log().warn(String)}.
     */
    public static void warn(String s) {
        log().warn(s);
    }

    /**
     * Shortcut to {@link Log#warn(Throwable) Syslog.log().warn(Throwable)}.
     */
    public static void warn(Throwable t) {
        log().warn(t);
    }

    /**
     * Shortcut to {@link Log#error(String) Syslog.log().error(String)}.
     */
    public static void error(String s) {
        log().error(s);
    }

    /**
     * Shortcut to {@link Log#error(Throwable) Syslog.log().error(Throwable)}.
     */
    public static void error(Throwable t) {
        log().error(t);
    }

    private Syslog() {
    }
}
