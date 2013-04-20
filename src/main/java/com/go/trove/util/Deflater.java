/* ====================================================================
 * Trove - Copyright (c) 1997-2001 Walt Disney Internet Group
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

package com.go.trove.util;

/******************************************************************************
 * A zlib deflater interface that matches {@link java.util.zip.Deflater},
 * except additional flush operations are supported. This class requires native
 * code support and looks for a library named "TroveZip".
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/06/13 <!-- $-->
 */
public class Deflater {
    public static final int
        DEFLATED = 8,
        NO_COMPRESSION = 0,
        BEST_SPEED = 1,
        BEST_COMPRESSION = 9,
        DEFAULT_COMPRESSION = -1,
        FILTERED = 1,
        HUFFMAN_ONLY = 2,
        DEFAULT_STRATEGY = 0;

    private static final int
        NO_FLUSH = 0,
        SYNC_FLUSH = 2,
        FULL_FLUSH = 3,
        FINISH = 4;

    static {
        System.loadLibrary("TroveZip");
        initIDs();
    }

    // Pointer to strm used by native deflate functions.
    private long mStream;
    private boolean mNoWrap;

    private int mStrategy;
    private int mLevel;
    private boolean mSetParams;

    private int mFlushOption = NO_FLUSH;
    private boolean mFinished;

    private byte[] mInputBuf;
    private int mInputOffset;
    private int mInputLength;

    public Deflater(int level, boolean nowrap) {
        mStream = init(DEFAULT_STRATEGY, level, nowrap);
        mStrategy = DEFAULT_STRATEGY;
        mLevel = level;
        mNoWrap = nowrap;
    }

    public Deflater(int level) {
        this(level, false);
    }

    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    public boolean isNoWrap() {
        return mNoWrap;
    }

    public synchronized void setInput(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        mInputLength = len;
        mInputOffset = off;
        mInputBuf = b;
    }
    
    public synchronized void setInput(byte[] b) {
        mInputLength = b.length;
        mInputOffset = 0;
        mInputBuf = b;
    }
    
    public synchronized void setDictionary(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        setDictionary(mStream, b, off, len);
    }
    
    public synchronized void setDictionary(byte[] b) {
        setDictionary(mStream, b, 0, b.length);
    }

    public synchronized void setStrategy(int strategy) {
        mStrategy = strategy;
        mSetParams = true;
    }
    
    public synchronized void setLevel(int level) {
        mLevel = level;
        mSetParams = true;
    }

    public boolean needsInput() {
        return mInputLength <= 0;
    }

    /**
     * When called, indicates that the current input buffer contents should be
     * flushed out when deflate is next called.
     */
    public void flush() {
        mFlushOption = SYNC_FLUSH;
    }

    /**
     * When called, indicates that the current input buffer contents should be
     * flushed out when deflate is next called, but all compression information
     * up to this point is cleared.
     */
    public void fullFlush() {
        mFlushOption = FULL_FLUSH;
    }

    /**
     * When called, indicates that compression should end with the current
     * contents of the input buffer. Deflate must be called to get the final
     * compressed bytes.
     */
    public void finish() {
        mFlushOption = FINISH;
    }

    public synchronized boolean finished() {
        return mFinished;
    }

    public int deflate(byte[] b, int off, int len) {
        boundsCheck(b, off, len);
        return deflate0(b, off, len);
    }

    public int deflate(byte[] b) {
        return deflate0(b, 0, b.length);
    }

    private synchronized int deflate0(byte[] b, int off, int len) {
        int amt = deflate(mStream, mFlushOption, mSetParams,
                          mInputBuf, mInputOffset, mInputLength,
                          b, off, len);
        if (amt < len) {
            if (mFlushOption == SYNC_FLUSH || mFlushOption == FULL_FLUSH) {
                mFlushOption = NO_FLUSH;
            }
        }
        return amt;
    }

    public synchronized int getAdler() {
        return getAdler(mStream);
    }

    public synchronized int getTotalIn() {
        return getTotalIn(mStream);
    }

    public synchronized int getTotalOut() {
        return getTotalOut(mStream);
    }

    private int mResetCount;

    public synchronized void reset() {
        mFinished = false;
        mFlushOption = NO_FLUSH;
        mInputBuf = null;
        mInputLength = 0;
        reset(mStream);
    }

    public synchronized void end() {
        end(mStream);
    }

    protected void finalize() {
        end();
    }

    private void boundsCheck(byte[] b, int off, int len) {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private static native void initIDs();

    private native long init(int strategy, int level, boolean nowrap);

    private native void setDictionary(long strm, byte[] b, int off, int len);

    private native int deflate(long strm, int flushOpt, boolean setParams,
                               byte[] inBuf, int inOff, int inLen,
                               byte[] outBuf, int outOff, int outLen);

    private native int getAdler(long strm);
    private native int getTotalIn(long strm);
    private native int getTotalOut(long strm);
    private native void reset(long strm);
    private native void end(long strm);
}
