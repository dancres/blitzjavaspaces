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

import java.io.*;

/******************************************************************************
 * Just like {@link java.util.zip.DeflaterOutputStream}, except uses a
 * different {@link Deflater} implementation.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/06/13 <!-- $-->
 */
public class DeflaterOutputStream extends FilterOutputStream {
    protected Deflater mDeflater;
    protected byte[] mBuffer;

    public DeflaterOutputStream(OutputStream out, Deflater def, byte[] buf) {
        super(out);
        mDeflater = def;
        mBuffer = buf;
    }
   
    public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
        this(out, def, new byte[size]);
    }

    public DeflaterOutputStream(OutputStream out, Deflater def) {
        this(out, def, 512);
    }

    public DeflaterOutputStream(OutputStream out) {
        this(out, new Deflater());
    }

    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (mDeflater.finished()) {
            throw new IOException("write beyond end of stream");
        }
        mDeflater.setInput(b, off, len);
        while (!mDeflater.needsInput()) {
            deflate();
        }
    }

    public void flush() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.flush();
            int len;
            do {
                len = deflate();
            } while (len == mBuffer.length);
        }
    }

    public void fullFlush() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.fullFlush();
            int len;
            do {
                len = deflate();
            } while (len == mBuffer.length);
        }
    }

    public void finish() throws IOException {
        if (!mDeflater.finished()) {
            mDeflater.finish();
            do {
                deflate();
            } while (!mDeflater.finished());
        }
    }

    public void close() throws IOException {
        finish();
        super.close();
    }
    
    private int deflate() throws IOException {
        int len = mDeflater.deflate(mBuffer, 0, mBuffer.length);
        if (len > 0) {
            out.write(mBuffer, 0, len);
        }
        return len;
    }
}
