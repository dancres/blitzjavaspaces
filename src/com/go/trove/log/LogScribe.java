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
import java.text.*;
import java.util.TimeZone;
import com.go.trove.util.FastDateFormat;

/******************************************************************************
 * LogScribe is a LogListener that writes log messages to a PrintWriter. Each
 * message is printed to a line that is prepended with other LogEvent
 * information. The default prepend format is as follows:
 *
 * <pre>
 * [event type code],[date & time],[thread name],[log source name]>[one space]
 * </pre>
 *
 * The event type codes are <tt>" D", " I", "*W" and "*E"</tt> for debug,
 * info, warn and error, respectively. The default date format looks like
 * this: <tt>"1999/06/08 18:08:34.067 PDT"</tt>. If there is no log source, or
 * it has no name, it is omitted from the prepend. Here is a sample line that
 * is written out:
 *
 * <pre>
 *  I,1999/06/08 18:08:34.67 PDT,main> Started Transaction Manager
 * </pre>
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/07/02 <!-- $-->
 */
public class LogScribe implements LogListener {
    private PrintWriter mWriter;
    private DateFormat mSlowFormat;
    private FastDateFormat mFastFormat;

    private boolean mShowThread = true;
    private boolean mShowSourceName = true;

    public LogScribe(PrintWriter writer) {
        this(writer, (FastDateFormat)null);
    }

    public LogScribe(PrintWriter writer, DateFormat format) {
        mWriter = writer;
        mSlowFormat = format;
        if (format == null) {
            mFastFormat =
                FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS z");
        }
        else if (format instanceof SimpleDateFormat) {
            SimpleDateFormat simple = (SimpleDateFormat)format;
            String pattern = simple.toPattern();
            TimeZone timeZone = simple.getTimeZone();
            DateFormatSymbols symbols = simple.getDateFormatSymbols();
            mFastFormat =
                FastDateFormat.getInstance(pattern, timeZone, null, symbols);
        }
    }

    public LogScribe(PrintWriter writer, FastDateFormat format) {
        mWriter = writer;
        mSlowFormat = null;
        if (format == null) {
            format = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS z");
        }
        mFastFormat = format;
    }

    public void logMessage(LogEvent e) {
        String message = e.getMessage();
        if (message != null) {
            synchronized (mWriter) {
                mWriter.print(createPrepend(e));
                mWriter.println(message);
                mWriter.flush();
            }
        }
    }

    public void logException(LogEvent e) {
        Throwable t = e.getException();
        
        if (t == null) {
            logMessage(e);
        }
        else {
            synchronized (mWriter) {
                mWriter.print(createPrepend(e));
                t.printStackTrace(mWriter);
                mWriter.flush();
            }
        }
    }

    /**
     * The showing of the event thread name is on by default.
     */
    public boolean isShowThreadEnabled() {
        return mShowThread;
    }

    public void setShowThreadEnabled(boolean enabled) {
        mShowThread = enabled;
    }

    /**
     * The showing of the event source name is on by default.
     */
    public boolean isShowSourceEnabled() {
        return mShowSourceName;
    }

    public void setShowSourceEnabled(boolean enabled) {
        mShowSourceName = enabled;
    }

    /**
     * Creates the default line prepend for a message.
     */
    protected String createPrepend(LogEvent e) {
        StringBuffer pre = new StringBuffer(80);
                
        String code = "??";
        switch (e.getType()) {
        case LogEvent.DEBUG_TYPE:
            code = " D";
            break;
        case LogEvent.INFO_TYPE:
            code = " I";
            break;
        case LogEvent.WARN_TYPE:
            code = "*W";
            break;
        case LogEvent.ERROR_TYPE:
            code = "*E";
            break;
        }

        pre.append(code);
        pre.append(',');
        if (mFastFormat != null) {
            pre.append(mFastFormat.format(e.getTimestamp()));
        }
        else {
            synchronized (mSlowFormat) {
                pre.append(mSlowFormat.format(e.getTimestamp()));
            }
        }

        if (isShowThreadEnabled()) {
            pre.append(',');
            pre.append(e.getThreadName());
        }

        if (isShowSourceEnabled()) {
            Log source = e.getLogSource();
            if (source != null) {
                String sourceName = source.getName();
                if (sourceName != null) {
                    pre.append(',');
                    pre.append(sourceName);
                }
            }
        }

        pre.append('>');
        pre.append(' ');

        return pre.toString();
    }
}
