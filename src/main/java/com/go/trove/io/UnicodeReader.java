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
 * This reader handles unicode escapes in a character stream as defined by
 * <i>The Java Language Specification</i>. 
 * 
 * <p>A unicode escape consists of six characters: '\' and 'u' followed by 
 * four hexadecimal digits. If the format of the escape is not correct, then 
 * the escape is unprocessed. To prevent a correctly formatted unicode escape 
 * from being processed, preceed it with another '\'.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 12/11/00 <!-- $-->
 */
public class UnicodeReader extends EscapeReader {
    /** Just a temporary buffer for holding the four hexadecimal digits. */
    private char[] mMinibuf = new char[4];

    private boolean mEscaped;

    /**
     * A UnicodeReader needs an underlying source Reader.
     *
     * @param source the source PositionReader
     */
    public UnicodeReader(Reader source) {
        super(source, 6);
    }

    public int read() throws IOException {
        int c = mSource.read();

        if (c != '\\' || !mEscapesEnabled) {
            mEscaped = false;
            return c;
        }

        c = mSource.read();

        // Have scanned "\\"? (two backslashes)
        if (c == '\\') {
            mEscaped = !mEscaped;
            mSource.unread();
            return '\\';
        }

        // Have not scanned '\', 'u'?
        if (c != 'u') {
            mSource.unread();
            return '\\';
        }

        // At this point, have scanned '\', 'u'.

        // If previously escaped, then don't process unicode escape.
        if (mEscaped) {
            mEscaped = false;
            mSource.unread();
            return '\\';
        }

        int len = mSource.read(mMinibuf, 0, 4);
        
        if (len == 4) {
            try {
                int val = 
                    Integer.valueOf(new String(mMinibuf, 0, 4), 16).intValue();

                return val;
            }
            catch (NumberFormatException e) {
                // If the number is not a parseable as hexadecimal, then
                // treat this as a bad format and do not process the
                // unicode escape.
            }
        }

        // Unread the four hexadecimal characters and the leading 'u'.
        if (len >= 0) {
            mSource.unread(len + 1);
        }

        return '\\';
    }

    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    private static class Tester {
        public static void test(String[] arg) throws Exception {
            String str = 
                "This is \\" + "u0061 test.\n" +
                "This is \\" + "u00612 test.\n" +
                "This is \\" + "u0061" + "\\" + "u0061" + " test.\n" +
                "This is \\" + "u061 test.\n" +
                "This is \\\\" + "u0061 test.\n" +
                "This is \\" + "a test.\n";

            System.out.println("\nOriginal:\n");
            
            Reader reader = new StringReader(str);

            int c;
            while ( (c = reader.read()) >= 0 ) {
                System.out.print((char)c);
            }

            System.out.println("\nConverted:\n");
            
            reader = new StringReader(str);
            reader = new UnicodeReader(reader);

            while ( (c = reader.read()) != -1 ) {
                System.out.print((char)c);
            }

            System.out.println("\nUnread test 1:\n");
            
            reader = new StringReader(str);
            PushbackPositionReader pr = 
                new PushbackPositionReader(new UnicodeReader(reader), 1);

            while ( (c = pr.read()) != -1 ) {
                pr.unread();
                c = pr.read();
                System.out.print((char)c);
            }

            System.out.println("\nUnread test 2:\n");
            
            reader = new StringReader(str);
            pr = new PushbackPositionReader(new UnicodeReader(reader), 2);

            int i = 0;
            while ( (c = pr.read()) != -1 ) {
                if ( (i++ % 5) == 0 ) {
                    c = pr.read();
                    pr.unread();
                    pr.unread();
                    c = pr.read();
                }

                System.out.print((char)c);
            }

            System.out.println("\nUnread position test:\n");

            reader = new StringReader(str);
            pr = new PushbackPositionReader(new UnicodeReader(reader), 2);

            System.out.print(pr.getNextPosition() + "\t");
            i = 0;
            while ( (c = pr.read()) != -1 ) {
                if ( (i++ % 5) == 0 ) {
                    c = pr.read();
                    pr.unread();
                    pr.unread();
                    c = pr.read();
                }

                System.out.println((char)c);
                System.out.print(pr.getNextPosition() + "\t");
            }
        }
    }
}
