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

package com.go.trove.io;

import java.io.*;

/******************************************************************************
 * The PushbackPositionReader is a kind of pushback reader that tracks the 
 * postion in the stream of the next character to be read.
 * The java.io.PushbackReader allows arbitrary characters
 * to be pushed back. Since this Reader may need to keep track of how many
 * characters were scanned from the underlying Reader to actually produce
 * a character, the unread operation cannot accept any arbitrary character.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 12/11/00 <!-- $-->
 * @see java.io.PushbackReader
 */
public class PushbackPositionReader extends PositionReader {
    /** Maximum pushback allowed. */
    private int mMaxPushback;

    /** Most recently read characters with escapes already processed. */
    private int[] mCharacters;

    /** The scan lengths of the most recently read characters. */
    private int[] mPositions;

    /** The cursor marks the position in the arrays of the last character. */
    private int mCursor;

    /** Amount of characters currently pushed back. */
    private int mPushback;

    public PushbackPositionReader(Reader reader) {
        this(reader, 0);
    }

    public PushbackPositionReader(Reader reader, int pushback) {
        super(reader);

        // Two more are required for correct operation
        pushback += 2;

        mMaxPushback = pushback;
        mCharacters = new int[pushback];
        mPositions = new int[pushback];
        mCursor = 0;
        mPushback = 0;
    }

    /**
     * @return the start position of the last read character.
     */
    public int getStartPosition() {
        int back = mCursor - 2;
        if (back < 0) back += mMaxPushback;

        return mPositions[back];
    }

    public int read() throws IOException {
        int c;

        if (mPushback > 0) {
            mPushback--;

            mPosition = mPositions[mCursor];
            c = mCharacters[mCursor++];
            if (mCursor >= mMaxPushback) mCursor = 0;
            
            return c;
        }

        c = super.read();
        
        mPositions[mCursor] = mPosition;
        mCharacters[mCursor++] = c;
        if (mCursor >= mMaxPushback) mCursor = 0;

        return c;
    }

    public int peek() throws IOException {
        int c = read();
        unread();
        return c;
    }

    /**
     * Unread the last several characters read.
     *
     * <p>Unlike PushbackReader, unread does not allow arbitrary characters to
     * to be unread. Rather, it functions like an undo operation.
     *
     * @param amount Amount of characters to unread.
     * @see java.io.PushbackReader#unread(int)
     */
    public void unread(int amount) throws IOException {
        for (int i=0; i<amount; i++) {
            unread();
        }
    }
    
    /**
     * Unread the last character read.
     * 
     * <p>Unlike PushbackReader, unread does not allow arbitrary characters to
     * to be unread. Rather, it functions like an undo operation.
     *
     * @see java.io.PushbackReader#unread(int)
     */
    public void unread() throws IOException {
        mPushback++;

        if (mPushback > mMaxPushback - 2) {
            throw new IOException(this.getClass().getName() + 
                                  ": pushback exceeded " + (mMaxPushback - 2));
        }

        if ((--mCursor) < 0) mCursor += mMaxPushback;

        if (mCursor > 0) {
            mPosition = mPositions[mCursor - 1];
        }
        else {
            mPosition = mPositions[mMaxPushback - 1];
        }

        unreadHook(mCharacters[mCursor]);
    }

    /**
     * A hook call from the unread method(s). Every unread character is
     * passed to this method.
     */
    protected void unreadHook(int c) {
    }
}
