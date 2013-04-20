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

import java.io.*;

/******************************************************************************
 * This class corresponds to the CONSTANT_Class_info structure as defined in
 * section 4.4.1 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
 */
public class ConstantClassInfo extends ConstantInfo {
    private String mClassName;
    private int mDim;
    private TypeDescriptor mType;
    
    private ConstantUTFInfo mNameConstant;
    
    /** 
     * Will return either a new ConstantClassInfo object or one already in
     * the constant pool. If it is a new ConstantClassInfo, it will be inserted
     * into the pool.
     */
    static ConstantClassInfo make(ConstantPool cp, String className) {
        ConstantInfo ci = new ConstantClassInfo(cp, className);
        return (ConstantClassInfo)cp.addConstant(ci);
    }
    
    /** Used to describe an array class. */
    static ConstantClassInfo make(ConstantPool cp, 
                                  String className, int dim) {
        ConstantInfo ci = new ConstantClassInfo(cp, className, dim);
        return (ConstantClassInfo)cp.addConstant(ci);
    }
    
    static ConstantClassInfo make(ConstantPool cp,
                                  TypeDescriptor type) {
        ConstantInfo ci = new ConstantClassInfo(cp, type);
        return (ConstantClassInfo)cp.addConstant(ci);
    }

    ConstantClassInfo(ConstantUTFInfo nameConstant) {
        super(TAG_CLASS);
        mNameConstant = nameConstant;
        String name = nameConstant.getValue();
        if (!name.endsWith(";") && !name.startsWith("[")) {
            mClassName = name.replace('/', '.');
            mDim = 0;
        }
        else {
            TypeDescriptor desc = TypeDescriptor.parseTypeDesc(name);
            mClassName = desc.getClassName();
            mDim = desc.getDimensions();
        }
    }

    private ConstantClassInfo(ConstantPool cp, String className) {
        super(TAG_CLASS);
        
        mClassName = className;
        mDim = 0;
        
        String desc = className.replace('.', '/');
        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }
    
    /** Used to describe an array class. */
    private ConstantClassInfo(ConstantPool cp, String className, int dim) {
        super(TAG_CLASS);
        
        mClassName = className;
        mDim = dim;
        
        String desc;
        if (dim > 0) {
            desc = TypeDescriptor.generate(className, dim);
        }
        else {
            desc = className.replace('.', '/');
        }

        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }
    
    private ConstantClassInfo(ConstantPool cp, TypeDescriptor type) {
        super(TAG_CLASS);

        mClassName = type.getClassName();
        mDim = type.getDimensions();
        mType = type;

        String desc;
        if (mDim > 0) {
            desc = type.toString();
        }
        else {
            desc = type.getClassName().replace('.', '/');
        }

        mNameConstant = ConstantUTFInfo.make(cp, desc);
    }

    public String getClassName() {
        return mClassName;
    }

    public int getDimensions() {
        return mDim;
    }

    public TypeDescriptor getTypeDescriptor() {
        if (mType == null) {
            mType = new TypeDescriptor(getClassName());

            int dim = getDimensions();
            if (dim > 0) {
                mType = new TypeDescriptor(mType, dim);
            }
        }

        return mType;
    }

    public int hashCode() {
        return mClassName.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantClassInfo) {
            ConstantClassInfo other = (ConstantClassInfo)obj;
            return mClassName.equals(other.mClassName) && mDim == other.mDim;
        }
        
        return false;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mNameConstant.getIndex());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("CONSTANT_Class_info: ");
        buf.append(getClassName());
        for (int i = getDimensions(); i > 0; i--) {
            buf.append('[');
            buf.append(']');
        }
        return buf.toString();
    }
}
