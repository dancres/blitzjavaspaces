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
 * This class corresponds to the Exceptions_attribute structure as defined in
 * section 4.7.5 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 */
class ExceptionsAttr extends Attribute {
    private List mExceptions = new ArrayList(2);
    
    public ExceptionsAttr(ConstantPool cp) {
        super(cp, EXCEPTIONS);
    }
    
    public String[] getExceptions() {
        int size = mExceptions.size();
        String[] names = new String[size];

        for (int i=0; i<size; i++) {
            names[i] = ((ConstantClassInfo)mExceptions.get(i)).getClassName();
        }

        return names;
    }

    public void addException(ConstantClassInfo type) {
        mExceptions.add(type);
    }
    
    public int getLength() {
        return 2 + 2 * mExceptions.size();
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mExceptions.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            ConstantClassInfo info = (ConstantClassInfo)mExceptions.get(i);
            dout.writeShort(info.getIndex());
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        ExceptionsAttr attr = new ExceptionsAttr(cp);
        
        int size = din.readUnsignedShort();
        length -= 2;

        for (int i=0; i<size; i++) {
            int index = din.readUnsignedShort();
            length -= 2;
            ConstantClassInfo info = (ConstantClassInfo)cp.getConstant(index);
            attr.addException(info);
        }

        if (length > 0) {
            din.skipBytes(length);
        }
        
        return attr;
    }
}
