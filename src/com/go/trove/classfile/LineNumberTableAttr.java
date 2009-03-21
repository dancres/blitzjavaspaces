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

package com.go.trove.classfile;

import java.util.*;
import java.io.*;

/******************************************************************************
 * This class corresponds to the LineNumberTable_attribute structure as
 * defined  in section 4.7.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 */
class LineNumberTableAttr extends Attribute {
    private List mEntries = new ArrayList();
    private boolean mClean = false;
    
    public LineNumberTableAttr(ConstantPool cp) {
        super(cp, LINE_NUMBER_TABLE);
    }
    
    public int getLineNumber(Location start) {
        clean();
        int index = Collections.binarySearch(mEntries, new Entry(start, 0));
        if (index < 0) {
            if ((index = -index - 2) < 0) {
                return -1;
            }
        }
        return ((Entry)mEntries.get(index)).mLineNumber;
    }

    public void addEntry(Location start, int line_number) {
        check("line number", line_number);
        mEntries.add(new Entry(start, line_number));
        mClean = false;
    }
    
    public int getLength() {
        clean();
        return 2 + 4 * mEntries.size();
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mEntries.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Entry entry = (Entry)mEntries.get(i);
            
            int start_pc = entry.mStart.getLocation();

            check("line number table entry start PC", start_pc);

            dout.writeShort(start_pc);
            dout.writeShort(entry.mLineNumber);
        }
    }

    private void check(String type, int addr) throws RuntimeException {
        if (addr < 0 || addr > 65535) {
            throw new RuntimeException("Value for " + type + " out of " +
                                       "valid range: " + addr);

        }
    }

    private void clean() {
        if (!mClean) {
            mClean = true;

            // Clean things up by removing multiple mappings of the same
            // start_pc to line numbers. Only keep the last one.
            // This has to be performed now because the Labels should have
            // a pc location, but before they did not. Since entries must be
            // sorted ascending by start_pc, use a sorted set.

            Set reduced = new TreeSet();
            for (int i = mEntries.size(); --i >= 0; ) {
                reduced.add(mEntries.get(i));
            }

            mEntries = new ArrayList(reduced);
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        LineNumberTableAttr lineNumbers = new LineNumberTableAttr(cp);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int start_pc = din.readUnsignedShort();
            int line_number = din.readUnsignedShort();

            lineNumbers.addEntry(new FixedLocation(start_pc), line_number);
        }

        return lineNumbers;
    }

    private static class Entry implements Comparable {
        public final Location mStart;
        public final int mLineNumber;
        
        public Entry(Location start, int line_number) {
            mStart = start;
            mLineNumber = line_number;
        }

        public int compareTo(Object other) {
            int thisLoc = mStart.getLocation();
            int thatLoc = ((Entry)other).mStart.getLocation();
            
            if (thisLoc < thatLoc) {
                return -1;
            }
            else if (thisLoc > thatLoc) {
                return 1;
            }
            else {
                return 0;
            }
        }

        public boolean equals(Object other) {
            if (other instanceof Entry) {
                return mStart.getLocation() == 
                    ((Entry)other).mStart.getLocation();
            }
            return false;
        }

        public String toString() {
            return "start_pc=" + mStart.getLocation() + " => " +
                "line_number=" + mLineNumber;
        }
    }
}
