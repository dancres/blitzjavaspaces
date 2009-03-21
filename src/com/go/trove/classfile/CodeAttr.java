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
 * This class corresponds to the Code_attribute structure as defined in
 * section 4.7.4 of <i>The Java Virtual Machine Specification</i>.
 * To make it easier to create bytecode for the CodeAttr, use the 
 * CodeBuilder.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/01/02 <!-- $-->
 * @see Opcode
 * @see CodeBuilder
 */
public class CodeAttr extends Attribute {
    private CodeBuffer mCodeBuffer;
    private List mAttributes = new ArrayList(2);
    
    private LineNumberTableAttr mLineNumberTable;
    private LocalVariableTableAttr mLocalVariableTable;

    CodeAttr(ConstantPool cp) {
        super(cp, CODE);
    }
    
    /**
     * Returns null if no CodeBuffer is defined for this CodeAttr.
     */
    public CodeBuffer getCodeBuffer() {
        return mCodeBuffer;
    }

    public void setCodeBuffer(CodeBuffer code) {
        mCodeBuffer = code;
    }
    
    /**
     * Returns the line number in the source code from the given bytecode
     * address (start_pc).
     *
     * @return -1 if no line number is mapped for the start_pc.
     */
    public int getLineNumber(Location start) {
        if (mLineNumberTable == null || start.getLocation() < 0) {
            return -1;
        }
        else {
            return mLineNumberTable.getLineNumber(start);
        }
    }

    /**
     * Map a bytecode address (start_pc) to a line number in the source code
     * as a debugging aid.
     */
    public void mapLineNumber(Location start, int line_number) {
        if (mLineNumberTable == null) {
            addAttribute(new LineNumberTableAttr(mCp));
        }

        mLineNumberTable.addEntry(start, line_number);
    }

    /**
     * Indicate a local variable's use information be recorded in the
     * ClassFile as a debugging aid. If the LocalVariable doesn't provide
     * both a start and end location, then its information is not recorded.
     * This method should be called at most once per LocalVariable instance.
     */
    public void localVariableUse(LocalVariable localVar) {
        if (mLocalVariableTable == null) {
            addAttribute(new LocalVariableTableAttr(mCp));
        }

        mLocalVariableTable.addEntry(localVar);
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof LineNumberTableAttr) {
            if (mLineNumberTable != null) {
                mAttributes.remove(mLineNumberTable);
            }
            mLineNumberTable = (LineNumberTableAttr)attr;
        }
        else if (attr instanceof LocalVariableTableAttr) {
            if (mLocalVariableTable != null) {
                mAttributes.remove(mLocalVariableTable);
            }
            mLocalVariableTable = (LocalVariableTableAttr)attr;
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
        int length = 12;

        if (mCodeBuffer != null) {
            length += mCodeBuffer.getByteCodes().length;
            ExceptionHandler[] handlers = mCodeBuffer.getExceptionHandlers();
            if (handlers != null) {
                length += 8 * handlers.length;
            }
        }
        
        int size = mAttributes.size();
        for (int i=0; i<size; i++) {
            length += ((Attribute)mAttributes.get(i)).getLength();
            length += 6; // attributes have an intial 6 byte length
        }
        
        return length;
    }

    public void writeDataTo(DataOutput dout) throws IOException {
        if (mCodeBuffer == null) {
            throw new NullPointerException("CodeAttr has no CodeBuffer set");
        }

        ExceptionHandler[] handlers = mCodeBuffer.getExceptionHandlers();
        
        dout.writeShort(mCodeBuffer.getMaxStackDepth());
        dout.writeShort(mCodeBuffer.getMaxLocals());
        
        byte[] byteCodes = mCodeBuffer.getByteCodes();
        dout.writeInt(byteCodes.length);
        dout.write(byteCodes);

        if (handlers != null) {
            int exceptionHandlerCount = handlers.length;
            dout.writeShort(exceptionHandlerCount);

            for (int i=0; i<exceptionHandlerCount; i++) {
                handlers[i].writeTo(dout);
            }
        }
        else {
            dout.writeShort(0);
        }
        
        int size = mAttributes.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = (Attribute)mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din,
                            AttributeFactory attrFactory)
        throws IOException
    {
        CodeAttr code = new CodeAttr(cp);

        final int maxStackDepth = din.readUnsignedShort();
        final int maxLocals = din.readUnsignedShort();

        final byte[] byteCodes = new byte[din.readInt()];
        din.readFully(byteCodes);

        int exceptionHandlerCount = din.readUnsignedShort();
        final ExceptionHandler[] handlers = 
            new ExceptionHandler[exceptionHandlerCount];

        for (int i=0; i<exceptionHandlerCount; i++) {
            handlers[i] = ExceptionHandler.readFrom(cp, din);
        }
        
        code.mCodeBuffer = new CodeBuffer() {
            public int getMaxStackDepth() {
                return maxStackDepth;
            }
            
            public int getMaxLocals() {
                return maxLocals;
            }
            
            public byte[] getByteCodes() {
                return (byte[])byteCodes.clone();
            }
            
            public ExceptionHandler[] getExceptionHandlers() {
                return (ExceptionHandler[])handlers.clone();
            }
        };

        int attributeCount = din.readUnsignedShort();
        for (int i=0; i<attributeCount; i++) {
            code.addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }

        return code;
    }
}
