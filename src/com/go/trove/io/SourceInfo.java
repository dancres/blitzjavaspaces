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

/******************************************************************************
 * Provides information on where an object (like a token) appeared in the
 * the source file.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 12/11/00 <!-- $-->
 */
public class SourceInfo implements Cloneable, java.io.Serializable {
    private int mLine;
    private int mStartPosition;
    private int mEndPosition;

    public SourceInfo(int line, int startPos, int endPos) {
        mLine = line;
        mStartPosition = startPos;
        mEndPosition = endPos;
    }

    /**
     * @return The line in the source file. The first line is one.
     */
    public int getLine() {
        return mLine;
    }
    
    /**
     * @return The character position in the source file where this object 
     * started. The first position of the source file is zero.
     */
    public int getStartPosition() {
        return mStartPosition;
    }

    /**
     * @return The character position in the source file where this object 
     * ended. The first position of the source file is zero.
     */
    public int getEndPosition() {
        return mEndPosition;
    }

    /**
     * @return A character position detailing this object. Usually is the same 
     * as the start position.
     */
    public int getDetailPosition() {
        return mStartPosition;
    }

    private SourceInfo copy() {
        try {
            return (SourceInfo)super.clone();
        }
        catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * @return A clone of this SourceInfo, but with a different end position
     */
    public SourceInfo setEndPosition(int endPos) {
        SourceInfo infoCopy = copy();
        infoCopy.mEndPosition = endPos;
        return infoCopy;
    }

    /**
     * @return A clone of this SourceInfo, but with a different end position
     */
    public SourceInfo setEndPosition(SourceInfo info) {
        return setEndPosition(info.getEndPosition());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(60);

        buf.append("line=");
        buf.append(getLine());
        buf.append(',');
        buf.append("start=");
        buf.append(getStartPosition());
        buf.append(',');
        buf.append("end=");
        buf.append(getEndPosition());
        buf.append(',');
        buf.append("detail=");
        buf.append(getDetailPosition());

        return buf.toString();
    }
}
