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
 * This class corresponds to the LocalVariableTable_attribute structure as 
 * defined in section 4.7.7 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 */
class LocalVariableTableAttr extends Attribute {
    private List mEntries = new ArrayList(10);
    private List mCleanEntries;
    private int mRangeCount;
    
    public LocalVariableTableAttr(ConstantPool cp) {
        super(cp, LOCAL_VARIABLE_TABLE);
    }
    
    /**
     * Add an entry into the LocalVariableTableAttr.
     */
    public void addEntry(LocalVariable localVar) {
        String varName = localVar.getName();
        if (varName != null) {
            ConstantUTFInfo name = ConstantUTFInfo.make(mCp, varName);
            ConstantUTFInfo descriptor = 
                ConstantUTFInfo.make(mCp, localVar.getType().toString());

            mEntries.add(new Entry(localVar, name, descriptor));
        }

        mCleanEntries = null;
    }
    
    public int getLength() {
        clean();
        return 2 + 10 * mRangeCount;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mRangeCount);

        int size = mCleanEntries.size();
        for (int i=0; i<size; i++) {
            Entry entry = (Entry)mEntries.get(i);
            LocalVariable localVar = entry.mLocalVar;

            SortedSet ranges = localVar.getLocationRangeSet();

            int name_index = entry.mName.getIndex();
            int descriptor_index = entry.mDescriptor.getIndex();
            int index = localVar.getNumber();

            check("local variable table entry name index", name_index);
            check("local variable table entry descriptor index", 
                  descriptor_index);
            check("local variable table entry index", index);

            Iterator it = ranges.iterator();
            while (it.hasNext()) {
                LocationRange range = (LocationRange)it.next();

                Location startLocation = range.getStartLocation();
                Location endLocation = range.getEndLocation();

                int start_pc = startLocation.getLocation();
                int length = endLocation.getLocation() - start_pc - 1;

                check("local variable table entry start PC", start_pc);

                dout.writeShort(start_pc);
                dout.writeShort(length);
                dout.writeShort(name_index);
                dout.writeShort(descriptor_index);
                dout.writeShort(index);
            }
        }
    }

    private void check(String type, int addr) throws RuntimeException {
        if (addr < 0 || addr > 65535) {
            throw new RuntimeException("Value for " + type + " out of " +
                                       "valid range: " + addr);
        }
    }

    private void clean() {
        if (mCleanEntries != null) {
            return;
        }

        // Clean out entries that are incomplete or bogus.

        int size = mEntries.size();
        mCleanEntries = new ArrayList(size);
        mRangeCount = 0;

    outer:
        for (int i=0; i<size; i++) {
            Entry entry = (Entry)mEntries.get(i);
            LocalVariable localVar = entry.mLocalVar;

            SortedSet ranges = localVar.getLocationRangeSet();
            if (ranges == null || ranges.size() == 0) {
                continue;
            }

            Iterator it = ranges.iterator();
            while (it.hasNext()) {
                LocationRange range = (LocationRange)it.next();

                Location startLocation = range.getStartLocation();
                Location endLocation = range.getEndLocation();

                if (startLocation == null || endLocation == null) {
                    continue outer;
                }

                int start_pc = startLocation.getLocation();
                int length = endLocation.getLocation() - start_pc - 1;

                if (length < 0) {
                    continue outer;
                }
            }
            
            mCleanEntries.add(entry);
            mRangeCount += entry.getRangeCount();
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        LocalVariableTableAttr locals = new LocalVariableTableAttr(cp);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int start_pc = din.readUnsignedShort();
            int end_pc = start_pc + din.readUnsignedShort() + 1;
            int name_index = din.readUnsignedShort();
            int descriptor_index = din.readUnsignedShort();
            final int index = din.readUnsignedShort();

            final ConstantUTFInfo varName = 
                (ConstantUTFInfo)cp.getConstant(name_index);
            final ConstantUTFInfo varDesc = 
                (ConstantUTFInfo)cp.getConstant(descriptor_index);

            final Location startLocation = new FixedLocation(start_pc);
            final Location endLocation = new FixedLocation(end_pc);

            SortedSet ranges = new TreeSet();
            ranges.add(new LocationRangeImpl(startLocation, endLocation));
            final SortedSet fRanges =
                Collections.unmodifiableSortedSet(ranges);

            LocalVariable localVar = new LocalVariable() {
                private String mName;
                private TypeDescriptor mType;

                {
                    mName = varName.getValue();
                    mType = TypeDescriptor.parseTypeDesc(varDesc.getValue());
                }

                public String getName() {
                    return mName;
                }

                public void setName(String name) {
                    mName = name;
                }

                public TypeDescriptor getType() {
                    return mType;
                }
                
                public boolean isDoubleWord() {
                    Class clazz = mType.getClassArg();
                    return (clazz == double.class || clazz == long.class);
                }
                
                public int getNumber() {
                    return index;
                }

                public SortedSet getLocationRangeSet() {
                    return fRanges;
                }
            };

            Entry entry = new Entry(localVar, varName, varDesc);
            locals.mEntries.add(entry);
        }

        return locals;
    }

    private static class Entry {
        public LocalVariable mLocalVar;
        public ConstantUTFInfo mName;
        public ConstantUTFInfo mDescriptor;

        public Entry(LocalVariable localVar,
                     ConstantUTFInfo name, ConstantUTFInfo descriptor) {

            mLocalVar = localVar;
            mName = name;
            mDescriptor = descriptor;
        }

        public int getRangeCount() {
            return mLocalVar.getLocationRangeSet().size();
        }
    }
}
