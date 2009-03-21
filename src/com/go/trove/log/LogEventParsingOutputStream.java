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

/******************************************************************************
 * LogEventParsingOutputStream parses the data written to it and converts it
 * to LogEvent objects. Add a LogListener to intercept LogEvents. Events are
 * parsed based on newline characters (LF, CRLF or CR) or a switch to a
 * different thread.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class LogEventParsingOutputStream extends OutputStream {
    private Vector mListeners;
    private Log mSource;
    private int mType;

    private byte[] mOneByte = new byte[1];

    private ByteArrayOutputStream mMessageBuffer;
    private String mEncoding;
    private Thread mMessageThread;

    private Date mTimestamp;

    // Set to true upon reading a CR so that if the next character is a LF,
    // it is consumed. This is how CRLF patterns are discovered.
    private boolean mTrackLF;

    /**
     * @param source Source object to create events with.
     * @param type Type of events to create.
     */
    public LogEventParsingOutputStream(Log source, int type) {
        mListeners = new Vector(2);
        mSource = source;
        mType = type;

        mMessageBuffer = new ByteArrayOutputStream();
    }

    /**
     * @param source Source object to create events with.
     * @param type Type of events to create.
     * @param encoding Character encoding of bytes.
     */
    public LogEventParsingOutputStream(Log source, int type,
                                       String encoding) {
        mListeners = new Vector(2);
        mSource = source;
        mType = type;

        mMessageBuffer = new ByteArrayOutputStream();
        mEncoding = encoding;
    }

    public void addLogListener(LogListener listener) {
        mListeners.addElement(listener);
    }

    public void removeLogListener(LogListener listener) {
        mListeners.removeElement(listener);
    }

    private synchronized void flushLogEvent() 
        throws UnsupportedEncodingException {

        if (mMessageThread == null) {
            return;
        }

        String message;
        if (mEncoding == null) {
            message = mMessageBuffer.toString();
        }
        else {
            message = mMessageBuffer.toString(mEncoding);
        }

        if (mMessageBuffer.size() > 10000) {
            mMessageBuffer = new ByteArrayOutputStream();
        }
        else {
            mMessageBuffer.reset();
        }

        LogEvent e;

        if (mTimestamp == null) {
            e = new LogEvent(mSource, mType, message, mMessageThread);
        }
        else {
            e = new LogEvent(mSource, mType, message, mMessageThread, 
                             mTimestamp);
            mTimestamp = null;
        }

        synchronized (mListeners) {
            Enumeration group = mListeners.elements();
            while (group.hasMoreElements()) {
                ((LogListener)group.nextElement()).logMessage(e);
            }
        }
    }

    public synchronized void write(int b) throws IOException {
        mOneByte[0] = (byte)b;
        write(mOneByte, 0, 1);
    }

    public synchronized void write(byte[] array, int off, int len) 
        throws IOException {

        if (!isEnabled()) {
            if (mMessageBuffer.size() > 0) {
                flushLogEvent();
            }
            return;
        }

        Thread current = Thread.currentThread();
        if (current != mMessageThread) {
            if (mMessageBuffer.size() > 0) {
                flushLogEvent();
            }
            mMessageThread = current;
        }

        int writtenLength = 0;

        int i = 0;
        for (i=0; i<len; i++) {
            byte b = array[i + off];
            if (b == (byte)'\r') {
                mTrackLF = true;
                writeToBuffer(array, writtenLength + off, i - writtenLength);
                // Add one more than i to skip the CR.
                writtenLength = i + 1;
                flushLogEvent();
            }
            else if (b == (byte)'\n') {
                if (mTrackLF) {
                    // Consume the LF of CRLF.
                    mTrackLF = false;
                    writtenLength++;
                }
                else {
                    writeToBuffer(array, writtenLength + off, 
                                  i - writtenLength);
                    // Add one more than i to skip the LF.
                    writtenLength = i + 1;
                    flushLogEvent();
                }
            }
            else {
                mTrackLF = false;
            }
        }

        writeToBuffer(array, writtenLength + off, i - writtenLength);
    }

    public synchronized void close() throws IOException {
        mMessageBuffer.close();
    }

    /**
     * Returning false discards written data, and events are not generated. 
     * Default implementation always returns true. 
     */
    public boolean isEnabled() {
        return true;
    }

    private void writeToBuffer(byte[] array, int off, int len) {
        if (mMessageBuffer.size() == 0) {
            mTimestamp = new Date();
        }
        mMessageBuffer.write(array, off, len);
    }
}
