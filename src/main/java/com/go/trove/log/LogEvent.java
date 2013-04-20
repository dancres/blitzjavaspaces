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
import java.util.*;
import java.lang.ref.WeakReference;

/******************************************************************************
 * LogEvent captures information that should be logged. LogEvents are one
 * of four types: debug, info, warn or error. All LogEvents have a
 * timestamp for when the event occurred and a reference to the thread
 * that created it. Most have an embedded message, and some have an
 * embedded exception.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class LogEvent extends EventObject {
    /** Debug type of LogEvent */
    public static final int DEBUG_TYPE = 1;

    /** Info type of LogEvent */
    public static final int INFO_TYPE = 2;

    /** Warn type of LogEvent */
    public static final int WARN_TYPE = 3;

    /** Error type of LogEvent */
    public static final int ERROR_TYPE = 4;

    private int mType;
    private Date mTimestamp;
    private String mMessage;
    private Throwable mThrowable;
    private String mThreadName;
    // WeakReference to a Thread.
    private transient WeakReference mThread;
    
    public LogEvent(Log log, int type, 
                    String message, Throwable throwable,
                    Thread thread, Date timestamp) {
        super(log);
        
        if (type < DEBUG_TYPE || type > ERROR_TYPE) {
            throw new IllegalArgumentException
                ("Type out of range: " + type);
        }
        
        mType = type;
        
        if (message == null) {
            if (throwable != null) {
                mMessage = throwable.getMessage();
            }
        }
        else {
            mMessage = message;
        }
        
        mThrowable = throwable;
        
        if (thread == null) {
            mThread = new WeakReference(Thread.currentThread());
        }
        else {
            mThread = new WeakReference(thread);
        }
        
        if (timestamp == null) {
            mTimestamp = new Date();
        }
        else {
            mTimestamp = timestamp;
        }
    }
    
    public LogEvent(Log log, int type, 
                    String message, Thread thread, Date timestamp) {
        this(log, type, message, null, thread, timestamp);
    }
    
    public LogEvent(Log log, int type, 
                    Throwable throwable, Thread thread, Date timestamp) {
        this(log, type, null, throwable, thread, timestamp);
    }
    
    public LogEvent(Log log, int type, 
                    String message, Thread thread) {
        this(log, type, message, null, thread, null);
    }
    
    public LogEvent(Log log, int type, 
                    Throwable throwable, Thread thread) {
        this(log, type, null, throwable, thread, null);
    }
    
    public LogEvent(Log log, int type, 
                    String message, Throwable throwable) {
        this(log, type, message, throwable, null, null);
    }
    
    public LogEvent(Log log, int type, String message) {
        this(log, type, message, null, null, null);
    }
    
    public LogEvent(Log log, int type, Throwable throwable) {
        this(log, type, null, throwable, null, null);
    }
    
    public Log getLogSource() {
        return (Log)getSource();
    }
    
    /**
     * Returns the type of this LogEvent, which matches one of the defined
     * type constants.
     */
    public int getType() {
        return mType;
    }
    
    /**
     * Returns the date and time of this event.
     */
    public Date getTimestamp() {
        return mTimestamp;
    }
    
    /**
     * Message may be null.
     */
    public String getMessage() {
        return mMessage;
    }
    
    /**
     * Returns null if there is no exception logged.
     */
    public Throwable getException() {
        return mThrowable;
    }
    
    /**
     * Returns null if there is no exception logged.
     */
    public String getExceptionStackTrace() {
        Throwable t = getException();
        if (t == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Returns the name of the thread that created this event.
     */
    public String getThreadName() {
        if (mThreadName == null) {
            Thread t = getThread();
            if (t != null) {
                mThreadName = t.getName();
            }
        }
        return mThreadName;
    }
    
    /**
     * Returns the thread that created this event, which may be null if
     * this LogEvent was deserialized or the thread has been reclaimed.
     */
    public Thread getThread() {
        return (Thread)mThread.get();
    }
    
    public String toString() {
        String msg;
        if (getMessage() == null) {
            msg = "null";
        }
        else {
            msg = '"' + getMessage() + '"';
        }
        
        return 
            getClass().getName() + "[" + 
            getTimestamp() + ',' +
            getThreadName() + ',' + 
            msg +
            "] from " + getSource();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        getThreadName();
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) 
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
