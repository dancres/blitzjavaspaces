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
import java.lang.reflect.Modifier;

/******************************************************************************
 * This class corresponds to the field_info structure as defined in
 * section 4.5 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 * @see ClassFile
 */
public class FieldInfo {
    private ClassFile mParent;
    private ConstantPool mCp;

    private String mName;
    private TypeDescriptor mType;

    private int mAccessFlags;

    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    private List mAttributes = new ArrayList(2);

    private ConstantValueAttr mConstant;
    
    FieldInfo(ClassFile parent,
              AccessFlags flags,
              String name,
              TypeDescriptor type) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = name;
        mType = type;

        mAccessFlags = flags.getModifier();
        mNameConstant = ConstantUTFInfo.make(mCp, name);
        mDescriptorConstant = ConstantUTFInfo.make(mCp, type.toString());
    }
    
    private FieldInfo(ClassFile parent,
                      int flags,
                      ConstantUTFInfo nameConstant,
                      ConstantUTFInfo descConstant) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = nameConstant.getValue();
        mType = TypeDescriptor.parseTypeDesc(descConstant.getValue());

        mAccessFlags = flags;
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;
    }

    /**
     * Returns the parent ClassFile for this FieldInfo.
     */
    public ClassFile getClassFile() {
        return mParent;
    }

    /**
     * Returns the name of this field.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the type of this field.
     */
    public TypeDescriptor getType() {
        return mType;
    }
    
    /**
     * Returns a copy of this field's access flags.
     */
    public AccessFlags getAccessFlags() {
        return new AccessFlags(mAccessFlags);
    }

    /**
     * Returns a constant from the constant pool with this field's name.
     */
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Returns a constant from the constant pool with this field's type 
     * descriptor string.
     * @see TypeDescriptor
     */
    public ConstantUTFInfo getDescriptorConstant() {
        return mDescriptorConstant;
    }

    /**
     * Returns the constant value for this field or null if no constant set.
     */
    public ConstantInfo getConstantValue() {
        if (mConstant == null) {
            return null;
        }
        else {
            return mConstant.getConstant();
        }
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the constant value for this field as an int.
     */
    public void setConstantValue(int value) {
        addAttribute(new ConstantValueAttr
                     (mCp, ConstantIntegerInfo.make(mCp, value)));
    }

    /**
     * Set the constant value for this field as a float.
     */
    public void setConstantValue(float value) {
        addAttribute(new ConstantValueAttr
                     (mCp, ConstantFloatInfo.make(mCp, value)));
    }

    /**
     * Set the constant value for this field as a long.
     */
    public void setConstantValue(long value) {
        addAttribute(new ConstantValueAttr
                     (mCp, ConstantLongInfo.make(mCp, value)));
    }

    /**
     * Set the constant value for this field as a double.
     */
    public void setConstantValue(double value) {
        addAttribute(new ConstantValueAttr
                     (mCp, ConstantDoubleInfo.make(mCp, value)));
    }

    /**
     * Set the constant value for this field as a string.
     */
    public void setConstantValue(String value) {
        addAttribute(new ConstantValueAttr
                     (mCp, ConstantStringInfo.make(mCp, value)));
    }
    
    /**
     * Mark this field as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this field as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof ConstantValueAttr) {
            if (mConstant != null) {
                mAttributes.remove(mConstant);
            }
            mConstant = (ConstantValueAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        Attribute[] attrs = new Attribute[mAttributes.size()];
        return (Attribute[])mAttributes.toArray(attrs);
    }
    
    /**
     * Returns the length (in bytes) of this object in the class file.
     */
    public int getLength() {
        int length = 8;
        
        int size = mAttributes.size();
        for (int i=0; i<size; i++) {
            length += ((Attribute)mAttributes.get(i)).getLength();
        }
        
        return length;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mAccessFlags);
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
        
        int size = mAttributes.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = (Attribute)mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    public String toString() {
        String modStr = Modifier.toString(mAccessFlags);
        if (modStr.length() == 0) {
            return String.valueOf(mType) + ' ' + getName();
        }
        else {
            return modStr + ' ' + mType + ' ' + getName();
        }
    }

    static FieldInfo readFrom(ClassFile parent, 
                              DataInput din,
                              AttributeFactory attrFactory)
        throws IOException
    {
        ConstantPool cp = parent.getConstantPool();

        int flags = din.readUnsignedShort();
        int index = din.readUnsignedShort();
        ConstantUTFInfo nameConstant = (ConstantUTFInfo)cp.getConstant(index);
        index = din.readUnsignedShort();
        ConstantUTFInfo descConstant = (ConstantUTFInfo)cp.getConstant(index);

        FieldInfo info = new FieldInfo(parent, flags,
                                       nameConstant, descConstant);

        // Read attributes.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            info.addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }

        return info;
    }
}
