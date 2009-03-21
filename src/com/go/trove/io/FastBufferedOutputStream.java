/*
 * @(#)BufferedOutputStream.java    1.27 00/02/02
 *
 * Copyright 1994-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package com.go.trove.io;

import java.io.*;

/**
 * FastBufferedOutputStream is just a slightly modified version of
 * {@link java.io.BufferedOutputStream}. The synchronization is gone, and so
 * writes are faster. Refer to the original BufferedOutputStream for
 * documentation.
 */
/*
 * @author  Arthur van Hoff
 * @version 1.27, 02/02/00
 * @since   JDK1.0
 */
public class FastBufferedOutputStream extends FilterOutputStream {
    // These fields have been renamed and made private. In the original, they
    // are protected.
    private byte[] mBuffer;
    private int mCount;

    public FastBufferedOutputStream(OutputStream out) {
        this(out, 512);
    }

    public FastBufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        mBuffer = new byte[size];
    }

    private void flushBuffer() throws IOException {
        if (mCount > 0) {
            out.write(mBuffer, 0, mCount);
            mCount = 0;
        }
    }

    public void write(int b) throws IOException {
        if (mCount >= mBuffer.length) {
            flushBuffer();
        }
        mBuffer[mCount++] = (byte)b;
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (len >= mBuffer.length) {
            flushBuffer();
            out.write(b, off, len);
            return;
        }
        if (len > mBuffer.length - mCount) {
            flushBuffer();
        }
        System.arraycopy(b, off, mBuffer, mCount, len);
        mCount += len;
    }

    public synchronized void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}
