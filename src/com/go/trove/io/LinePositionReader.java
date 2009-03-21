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
 * LinePositionReader aids in printing line numbers for error reporting. 
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 12/11/00 <!-- $-->
 */
public class LinePositionReader extends PositionReader {
    private int mLineNumber = 1;
    private int mPushback = -1;

    public LinePositionReader(Reader reader) {
        super(reader);
    }

    public int read() throws IOException {
        int c;

        if (mPushback >= 0) {
            c = mPushback;
            mPushback = -1;
        }
        else {
            c = super.read();
        }

        if (c == '\n') {
            mLineNumber++;
        }
        else if (c == '\r') {
            int peek = super.read();
            if (peek != '\n') {
                mLineNumber++;
            }
            mPushback = peek;
        }

        return c;
    }

    /**
     * After calling readLine, calling getLineNumber returns the next line
     * number.
     */
    public String readLine() throws IOException {
        StringBuffer buf = new StringBuffer(80);
        int line = mLineNumber;
        int c;
        while (line == mLineNumber && (c = read()) >= 0) {
            buf.append((char)c);
        }
        return buf.toString();
    }

    /**
     * Skips forward into the stream to the line number specified. The line
     * can then be read by calling readLine. Calling getPosition
     * returns the position that the line begins.
     *
     * @return the line number reached
     */
    public int skipForwardToLine(int line) throws IOException {
        while (mLineNumber < line && read() >= 0) {}
        return mLineNumber;
    }

    /**
     * @return the number of the line currently being read or the next one
     * available.
     */
    public int getLineNumber() {
        return mLineNumber;
    }

    /**
     * Converts all whitespace characters in a String to space characters
     * (\u0020).
     */
    public static String cleanWhitespace(String str) {
        int length = str.length();

        StringBuffer buf = new StringBuffer(length);
        for (int i=0; i<length; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                buf.append(' ');
            }
            else {
                buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * Creates and returns a String containing a sequence of the specified
     * length, repeating the given character.
     */
    public static String createSequence(char c, int length) {
        if (length < 0) length = 1;

        StringBuffer buf = new StringBuffer(length);
        for (; length > 0; length--) {
            buf.append(c);
        }
        return buf.toString();
    }
}
