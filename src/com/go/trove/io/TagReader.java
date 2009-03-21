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
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 12/11/00 <!-- $-->
 */
public class TagReader extends EscapeReader {
    private int mTagCount;
    private int[] mTagStarts;
    private String[] mTagEnds;
    private int[] mCodes;

    private char[] mMinibuf;

    private static int maxLength(String[] tags) {
        int max = 0;

        for (int i=0; i<tags.length; i++) {
            if (tags[i].length() > max) {
                max = tags[i].length();
            }
        }

        return max;
    }

    public TagReader(Reader source, String[] tags, int[] codes) {
        super(source, maxLength(tags));

        mTagCount = tags.length;
        mTagStarts = new int[mTagCount];
        mTagEnds = new String[mTagCount];
        mCodes = new int[mTagCount];

        for (int i=0; i<mTagCount; i++) {
            mTagStarts[i] = tags[i].charAt(0);
            mTagEnds[i] = tags[i].substring(1);
            mCodes[i] = codes[i];
        }
        
        mMinibuf = new char[maxLength(tags)];
    }

    public int read() throws IOException {
        int c = mSource.read();

        if (c == -1 || !mEscapesEnabled) {
            return c;
        }

        for (int i=0; i<mTagCount; i++) {
            if (mTagStarts[i] == c) {
                int length = mTagEnds[i].length();

                mMinibuf[0] = (char)c;
                int len = mSource.read(mMinibuf, 0, length);

                if (len == length) {
                    if (new String(mMinibuf, 0, length).equals(mTagEnds[i])) {
                        return mCodes[i];
                    }
                }
     
                if (len > 0) {
                    mSource.unread(len);
                }
            }
        }

        return c;
    }

    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    private static class Tester {
        public static void test(String[] arg) throws Exception {
            String str = "This <%is a %> % > > % %% >> < % test.\n";

            System.out.println("\nOriginal: " + str);

            System.out.println("\nConverted:\n");
            
            Reader reader = new StringReader(str);
            
            TagReader tr = new TagReader
                (reader, new String[] {"<%", "%>"}, new int[] {-2, -3});

            PositionReader pr = new PositionReader(tr);

            int c;
            System.out.print(pr.getNextPosition() + "\t");
            while ( (c = pr.read()) != -1 ) {
                System.out.println((char)c + "\t" + c);
                System.out.print(pr.getNextPosition() + "\t");
            }
        }
    }
}
