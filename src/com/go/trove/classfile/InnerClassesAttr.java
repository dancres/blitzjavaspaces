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
 * This class corresponds to the InnerClasses_attribute structure introduced in
 * JDK1.1. It is not defined in the first edition of 
 * <i>The Java Virual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 */
class InnerClassesAttr extends Attribute {
    private List mInnerClasses = new ArrayList();

    public InnerClassesAttr(ConstantPool cp) {
        super(cp, INNER_CLASSES);
    }

    /**
     * @param inner The full inner class name
     * @param outer The full outer class name
     * @param name The simple name of the inner class, or null if anonymous
     * @param accessFlags Access flags for the inner class
     */
    public void addInnerClass(String inner,
                              String outer,
                              String name,
                              AccessFlags accessFlags) {
        
        ConstantClassInfo innerInfo = ConstantClassInfo.make(mCp, inner);
        ConstantClassInfo outerInfo; 
        if (outer == null) {
            outerInfo = null;
        }
        else {
            outerInfo = ConstantClassInfo.make(mCp, outer);
        }

        ConstantUTFInfo nameInfo;
        if (name == null) {
            nameInfo = null;
        }
        else {
            nameInfo = ConstantUTFInfo.make(mCp, name);
        }

        mInnerClasses.add(new Info(innerInfo, outerInfo, nameInfo, 
                                   accessFlags.getModifier()));
    }

    public Info[] getInnerClassesInfo() {
        Info[] infos = new Info[mInnerClasses.size()];
        return (Info[])mInnerClasses.toArray(infos);
    }

    public int getLength() {
        return 2 + 8 * mInnerClasses.size();
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mInnerClasses.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            ((Info)mInnerClasses.get(i)).writeTo(dout);
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        InnerClassesAttr innerClasses = new InnerClassesAttr(cp);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int inner_index = din.readUnsignedShort();
            int outer_index = din.readUnsignedShort();
            int name_index = din.readUnsignedShort();
            int af = din.readUnsignedShort();
            
            ConstantClassInfo inner;
            if (inner_index == 0) {
                inner = null;
            }
            else {
                inner = (ConstantClassInfo)cp.getConstant(inner_index);
            }

            ConstantClassInfo outer;
            if (outer_index == 0) {
                outer = null;
            }
            else {
                outer = (ConstantClassInfo)cp.getConstant(outer_index);
            }
            
            ConstantUTFInfo innerName;
            if (name_index == 0) {
                innerName = null;
            }
            else {
                innerName = (ConstantUTFInfo)cp.getConstant(name_index);
            }

            Info info = new Info(inner, outer, innerName, af);
            innerClasses.mInnerClasses.add(info);
        }

        return innerClasses;
    }

    public static class Info {
        private ConstantClassInfo mInner;
        private ConstantClassInfo mOuter;
        private ConstantUTFInfo mName;
        private int mAccessFlags;

        Info(ConstantClassInfo inner,
             ConstantClassInfo outer,
             ConstantUTFInfo name,
             int accessFlags) {

            mInner = inner;
            mOuter = outer;
            mName = name;
            mAccessFlags = accessFlags;
        }

        /**
         * Returns null if no inner class specified.
         */
        public ConstantClassInfo getInnerClass() {
            return mInner;
        }

        /**
         * Returns null if no outer class specified.
         */
        public ConstantClassInfo getOuterClass() {
            return mOuter;
        }

        /**
         * Returns null if no inner class specified or is anonymous.
         */
        public String getInnerClassName() {
            if (mName == null) {
                return null;
            }
            else {
                return mName.getValue();
            }
        }

        /**
         * Returns a copy of the access flags.
         */
        public AccessFlags getAccessFlags() {
            return new AccessFlags(mAccessFlags);
        }

        public void writeTo(DataOutput dout) throws IOException {
            if (mInner == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mInner.getIndex());
            }

            if (mOuter == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mOuter.getIndex());
            }

            if (mName == null) {
                dout.writeShort(0);
            }
            else {
                dout.writeShort(mName.getIndex());
            }
            
            dout.writeShort(mAccessFlags);
        }
    }
}
