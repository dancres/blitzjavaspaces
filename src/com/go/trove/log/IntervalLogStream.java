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

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;

/******************************************************************************
 * IntervalLogStream writes to an underlying OutputStream that is opened once
 * per a specific time interval. This class forms the basis of a dated file 
 * logging mechanism.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 * @see DailyFileLogStream
 */
public abstract class IntervalLogStream extends OutputStream {
    private static int cCounter;

    private static synchronized String nextName() {
        return "IntervalLogStream Auto Rollover " + cCounter++;
    }

    private Factory mFactory;
    private OutputStream mOut;
    private boolean mIsClosed;

    private Calendar mIntervalStart;
    private Calendar mNextIntervalStart;

    private Thread mRolloverThread;

    public IntervalLogStream(Factory factory) {
        mFactory = factory;
    }

    /**
     * Starts up a thread that automatically rolls the underlying OutputStream
     * at the beginning of the interval, even if no output is written.
     */
    public synchronized void startAutoRollover() {
        // TODO: If java.util.Timer class is available, use it instead of
        // creating a thread each time.
        if (mRolloverThread == null) {
            mRolloverThread = new Thread(new AutoRollover(this), nextName());
            mRolloverThread.setDaemon(true);
            mRolloverThread.start();
        }
    }

    /**
     * If the auto-rollover thread was started, calling this method will 
     * stop it.
     */
    public synchronized void stopAutoRollover() {
        if (mRolloverThread != null) {
            mRolloverThread.interrupt();
            mRolloverThread = null;
        }
    }

    /**
     * Moves calendar to beginning of log interval.
     */
    protected abstract void moveToIntervalStart(Calendar cal);

    /**
     * Moves calendar to beginning of next log interval.
     */
    protected abstract void moveToNextIntervalStart(Calendar cal);

    public synchronized void write(int b) throws IOException {
        getOutputStream().write(b);
    }

    public synchronized void write(byte[] array) throws IOException {
        getOutputStream().write(array, 0, array.length);
    }

    public synchronized void write(byte[] array, int off, int len) 
        throws IOException {

        getOutputStream().write(array, off, len);
    }

    public synchronized void flush() throws IOException {
        getOutputStream().flush();
    }

    /**
     * Closes any underlying OutputStreams and stops the auto-rollover thread
     * if it is running.
     */
    public synchronized void close() throws IOException {
        mIsClosed = true;
        stopAutoRollover();

        if (mOut != null) {
            mOut.close();
        }
    }

    protected synchronized void finalize() throws IOException {
        close();
    }

    private synchronized OutputStream getOutputStream() throws IOException {
        if (mIsClosed) {
            throw new IOException("LogStream is closed");
        }

        Calendar cal = Calendar.getInstance();

        if (mOut == null || 
            cal.before(mIntervalStart) || !cal.before(mNextIntervalStart)) {

            if (mOut != null) {
                mOut.close();
            }

            mOut = new BufferedOutputStream
                (mFactory.openOutputStream(cal.getTime()));

            setIntervalEndpoints(cal);
        }

        return mOut;
    }

    private void setIntervalEndpoints(Calendar cal) {
        mIntervalStart = (Calendar)cal.clone();
        moveToIntervalStart(mIntervalStart);

        mNextIntervalStart = cal;
        moveToNextIntervalStart(mNextIntervalStart);
    }

    public static interface Factory {
        public OutputStream openOutputStream(Date date) throws IOException;
    }

    /**
     * Thread that just wakes up at the proper time so that log stream
     * rolls over even when there is no output.
     */
    private static class AutoRollover implements Runnable {
        // Refer to the log stream via a weak reference so that this thread
        // doesn't prevent it from being garbage collected.
        private WeakReference mLogStream;

        public AutoRollover(IntervalLogStream stream) {
            mLogStream = new WeakReference(stream);
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    IntervalLogStream stream = 
                        (IntervalLogStream)mLogStream.get();

                    if (stream == null || stream.mIsClosed) {
                        break;
                    }

                    try {
                        // Just requesting the stream forces a rollover.
                        stream.getOutputStream();
                    }
                    catch (IOException e) {
                    }

                    Calendar cal = Calendar.getInstance();
                    stream.moveToNextIntervalStart(cal);
                    
                    // Clear reference to stream so that it isn't strongly
                    // reachable from this thread.
                    stream = null;

                    long calTime = cal.getTime().getTime();
                    long timeLeft = calTime - System.currentTimeMillis();

                    while (timeLeft > 0) {
                        // Sleep until next start interval. ZZZ...
                        Thread.sleep(timeLeft);
                        timeLeft = calTime - System.currentTimeMillis();
                    }
                }
            }
            catch (InterruptedException e) {
                // Exit thread.
            }
        }
    }
}
