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
 * The SourceReader provides several services for reading source input.
 * It calculates line numbers, position in the source file, supports two
 * character pushback, extracts code from text that allows mixed code and
 * plain text, and it processes unicode escape sequences that appear in
 * source code.
 *
 * <p>Readers return -1 when the end of the stream, has been reached, and so
 * does SourceReader. SourceReader will also return other special negative
 * values to indicate a tag substitution. ENTER_CODE is returned to indicate
 * that characters read are in source code, and ENTER_TEXT is returned to
 * indicate that characters read are in plain text. The first character read
 * from a SourceReader is either ENTER_CODE or ENTER_TEXT;
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  4/26/01 <!-- $-->
 */
public class SourceReader extends PushbackPositionReader {
    public static final int ENTER_CODE = -2;
    public static final int ENTER_TEXT = -3;

    private static Reader createReader(Reader source, 
                                       String beginTag, String endTag) {
        String[] tags = new String[4];
        int[] codes = new int[4];

        int i = 0;
        // Convert different kinds of line breaks into the newline character.
        tags[i] = "\r\n"; codes[i++] = '\n';
        tags[i] = "\r"; codes[i++] = '\n';

        if (beginTag != null && beginTag.length() > 0) {
            tags[i] = beginTag; codes[i++] = ENTER_CODE;
        }

        if (endTag != null && endTag.length() > 0) {
            tags[i] = endTag; codes[i++] = ENTER_TEXT;
        }

        if (i < 4) {
            String[] newTags = new String[i];
            System.arraycopy(tags, 0, newTags, 0, i);
            tags = newTags;

            int[] newCodes = new int[i];
            System.arraycopy(codes, 0, newCodes, 0, i);
            codes = newCodes;
        }

        return new UnicodeReader(new TagReader(source, tags, codes));
    }

    private UnicodeReader mUnicodeReader;
    private TagReader mTagReader;
    private boolean mClosed = false;

    // The current line in the source. (1..)
    private int mLine = 1;

    private int mFirst;

    private String mBeginTag;
    private String mEndTag;

    /**
     * The begin and end tags for a SourceReader are optional. If the begin
     * tag is null or has zero length, then the SourceReader starts reading
     * characters as if they were source code.
     *
     * <p>If the end tag is null or has zero length, then a source code
     * region continues to the end of the input Reader's characters.
     *
     * @param source the source reader
     * @param beginTag tag that marks the beginning of a source code region
     * @param endTag tag that marks the end of a source code region
     */ 
    public SourceReader(Reader source, String beginTag, String endTag) {
        this(source, beginTag, endTag, false);
    }

    /**
     * The begin and end tags for a SourceReader are optional. If the begin
     * tag is null or has zero length, then the SourceReader starts reading
     * characters as if they were source code.
     *
     * <p>If the end tag is null or has zero length, then a source code
     * region continues to the end of the input Reader's characters.
     *
     * @param source the source reader
     * @param beginTag tag that marks the beginning of a source code region
     * @param endTag tag that marks the end of a source code region
     * @param inCode flag that indicates if the stream is starting in code
     */ 
    public SourceReader(Reader source, String beginTag, String endTag, 
                        boolean inCode) {
        super(createReader(source, beginTag, endTag), 2);
        mUnicodeReader = (UnicodeReader)in;
        mTagReader = (TagReader)mUnicodeReader.getOriginalSource();

        boolean codeMode = ((beginTag == null || beginTag.length() == 0) || 
                            inCode);
        mFirst = (codeMode) ? ENTER_CODE : ENTER_TEXT;

        mBeginTag = beginTag;
        mEndTag = endTag;
    }

    public String getBeginTag() {
        return mBeginTag;
    }

    public String getEndTag() {
        return mEndTag;
    }

    /** 
     * All newline character patterns are are converted to \n. 
     */
    public int read() throws IOException {
        int c;

        if (mFirst != 0) {
            c = mFirst;
            mFirst = 0;
        }
        else {
            c = super.read();
        }

        if (c == '\n') {
            mLine++;
        }
        else if (c == ENTER_CODE) {
            mUnicodeReader.setEscapesEnabled(true);
        }
        else if (c == ENTER_TEXT) {
            mUnicodeReader.setEscapesEnabled(false);
        }
        
        return c;
    }

    public int getLineNumber() {
        return mLine;
    }

    /**
     * The position in the reader where the last read character ended. The
     * position of the first character read from a Reader is zero.
     *
     * <p>The end position is usually the same as the start position, but
     * sometimes a SourceReader may combine multiple characters into a
     * single one.
     *
     * @return the end position where the last character was read
     */
    public int getEndPosition() {
        int e = getNextPosition() - 1;
        return (e < getStartPosition()) ? getStartPosition() : e;
    }

    public void ignoreTags(boolean ignore) {
        mTagReader.setEscapesEnabled(!ignore);
    }

    public boolean isClosed() {
        return mClosed;
    }

    public void close() throws IOException {
        mClosed = true;
        super.close();
    }

    protected void unreadHook(int c) {
        if (c == '\n') {
            mLine--;
        }
        else if (c == ENTER_CODE) {
            mUnicodeReader.setEscapesEnabled(false);
        }
        else if (c == ENTER_TEXT) {
            mUnicodeReader.setEscapesEnabled(true);
        }
    }

    /** 
     * Simple test program 
     */
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
                "This is \\" + "a test.\n" +
                "Plain text <%put code here%> plain text.\n" +
                "Plain text <\\" + "u0025put code here%> plain text.\n" +
                "Plain text <%put code here\\" + "u0025> plain text.\n";

            Reader reader;

            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                System.out.println("\nOriginal:\n");
                
                reader = new StringReader(str);

                int c;
                while ( (c = reader.read()) != -1 ) {
                    System.out.print((char)c);
                }
            }

            System.out.println("\nTest 1:\n");

            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                reader = new StringReader(str);
            }

            SourceReader sr = new SourceReader(reader, "<%", "%>");

            int c;
            while ( (c = sr.read()) != -1 ) {
                System.out.print((char)c);
                System.out.print("\t" + c);
                System.out.print("\t" + sr.getLineNumber());
                System.out.print("\t" + sr.getStartPosition());
                System.out.println("\t" + sr.getEndPosition());
            }

            System.out.println("\nTest 2:\n");
            if (arg.length > 0) {
                reader = new java.io.FileReader(arg[0]);
            }
            else {
                reader = new StringReader(str);
            }

            sr = new SourceReader(reader, null, null);

            while ( (c = sr.read()) != -1 ) {
                System.out.print((char)c);
                System.out.print("\t" + c);
                System.out.print("\t" + sr.getLineNumber());
                System.out.print("\t" + sr.getStartPosition());
                System.out.println("\t" + sr.getEndPosition());
            }
        }
    }
}
