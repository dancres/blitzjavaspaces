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
 * This class corresponds to the attribute_info structure defined in section
 * 4.7 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 * @see ClassFile
 */
public abstract class Attribute {
    final static Attribute[] NO_ATTRIBUTES = new Attribute[0];

    final static String CODE = "Code";
    final static String CONSTANT_VALUE = "ConstantValue";
    final static String DEPRECATED = "Deprecated";
    final static String EXCEPTIONS = "Exceptions";
    final static String INNER_CLASSES = "InnerClasses";
    final static String LINE_NUMBER_TABLE = "LineNumberTable";
    final static String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
    final static String SOURCE_FILE = "SourceFile";
    final static String SYNTHETIC = "Synthetic";

    /** The ConstantPool that this attribute is defined against. */
    protected final ConstantPool mCp;

    private String mName;
    private ConstantUTFInfo mNameConstant;
    
    protected Attribute(ConstantPool cp, String name) {
        mCp = cp;
        mName = name;
        mNameConstant = ConstantUTFInfo.make(cp, name);
    }

    /**
     * Returns the ConstantPool that this attribute is defined against.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }
 
    /**
     * Returns the name of this attribute.
     */
    public String getName() {
        return mName;
    }
    
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Some attributes have sub-attributes. Default implementation returns an
     * empty array.
     */
    public Attribute[] getAttributes() {
        return NO_ATTRIBUTES;
    }

    /**
     * Returns the length (in bytes) of this attribute in the class file.
     */
    public abstract int getLength();
    
    /**
     * This method writes the 16 bit name constant index followed by the
     * 32 bit attribute length, followed by the attribute specific data.
     */
    public final void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mNameConstant.getIndex());
        dout.writeInt(getLength());
        writeDataTo(dout);
    }

    /**
     * Write just the attribute specific data. The default implementation
     * writes nothing.
     */
    public void writeDataTo(DataOutput dout) throws IOException {
    }

    /**
     * @param attrFactory optional factory for reading custom attributes
     */
    public static Attribute readFrom(ConstantPool cp,
                                     DataInput din,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        int index = din.readUnsignedShort();
        String name = ((ConstantUTFInfo)cp.getConstant(index)).getValue();
        int length = din.readInt();

        attrFactory = new Factory(attrFactory);
        return attrFactory.createAttribute(cp, name, length, din);
    }

    private static class Factory implements AttributeFactory {
        private final AttributeFactory mAttrFactory;

        public Factory(AttributeFactory attrFactory) {
            mAttrFactory = attrFactory;
        }

        public Attribute createAttribute(ConstantPool cp, 
                                         String name,
                                         final int length,
                                         DataInput din) throws IOException {
            if (name.equals(CODE)) {
                return CodeAttr.define(cp, name, length, din, mAttrFactory);
            }
            else if (name.equals(CONSTANT_VALUE)) {
                return ConstantValueAttr.define(cp, name, length, din);
            }
            else if (name.equals(DEPRECATED)) {
                return DeprecatedAttr.define(cp, name, length, din);
            }
            else if (name.equals(EXCEPTIONS)) {
                return ExceptionsAttr.define(cp, name, length, din);
            }
            else if (name.equals(INNER_CLASSES)) {
                return InnerClassesAttr.define(cp, name, length, din);
            }
            else if (name.equals(LINE_NUMBER_TABLE)) {
                return LineNumberTableAttr.define(cp, name, length, din);
            }
            else if (name.equals(LOCAL_VARIABLE_TABLE)) {
                return LocalVariableTableAttr.define
                    (cp, name, length, din);
            }
            else if (name.equals(SOURCE_FILE)) {
                return SourceFileAttr.define(cp, name, length, din);
            }
            else if (name.equals(SYNTHETIC)) {
                return SyntheticAttr.define(cp, name, length, din);
            }

            if (mAttrFactory != null) {
                Attribute attr =
                    mAttrFactory.createAttribute(cp, name, length, din);
                if (attr != null) {
                    return attr;
                }
            }

            // Default case, return attribute that captures the data, but
            // doesn't decode it.

            final byte[] data = new byte[length];
            din.readFully(data);
            
            return new Attribute(cp, name) {
                public int getLength() {
                    return length;
                }
                
                public void writeDataTo(DataOutput dout) throws IOException {
                    dout.write(data);
                }
            };
        }
    }
}
