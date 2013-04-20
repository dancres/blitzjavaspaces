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
 * This class corresponds to the CONSTANT_NameAndType_info structure as defined
 * in section 4.4.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class ConstantNameAndTypeInfo extends ConstantInfo {
    private String mName;
    private Descriptor mType;
    
    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    /** 
     * Will return either a new ConstantNameAndTypeInfo object or one 
     * already in the constant pool. 
     * If it is a new ConstantNameAndTypeInfo, it will be inserted
     * into the pool.
     */
    static ConstantNameAndTypeInfo make(ConstantPool cp, 
                                        String name,
                                        Descriptor type) {
        ConstantInfo ci = new ConstantNameAndTypeInfo(cp, name, type);
        return (ConstantNameAndTypeInfo)cp.addConstant(ci);
    }
    
    ConstantNameAndTypeInfo(ConstantUTFInfo nameConstant,
                            ConstantUTFInfo descConstant) {
        super(TAG_NAME_AND_TYPE);
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;

        mName = nameConstant.getValue();
        mType = Descriptor.parse(descConstant.getValue());
    }

    private ConstantNameAndTypeInfo(ConstantPool cp, 
                                    String name, 
                                    Descriptor type) {
        super(TAG_NAME_AND_TYPE);
        mName = name;
        mType = type;
        
        mNameConstant = ConstantUTFInfo.make(cp, name);
        mDescriptorConstant = ConstantUTFInfo.make(cp, mType.toString());
    }
    
    public String getName() {
        return mName;
    }

    public Descriptor getType() {
        return mType;
    }

    public int hashCode() {
        return mName.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ConstantNameAndTypeInfo) {
            ConstantNameAndTypeInfo other = (ConstantNameAndTypeInfo)obj;
            return mName.equals(other.mName) && 
                mType.toString().equals(other.mType.toString());
        }
        
        return false;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        super.writeTo(dout);
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
    }

    public String toString() {
        return "CONSTANT_NameAndType_info: " + getName() + ", " + getType();
    }
}
