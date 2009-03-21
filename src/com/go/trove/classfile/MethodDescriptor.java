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

import java.util.List;
import java.util.ArrayList;

/******************************************************************************
 * This class is used to build method descriptor strings as 
 * defined in <i>The Java Virtual Machine Specification</i>, section 4.3.3.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class MethodDescriptor extends Descriptor {
    private static TypeDescriptor[] cEmptyParams = new TypeDescriptor[0];

    private String mStr;
    private TypeDescriptor mRetType;
    private TypeDescriptor[] mParams;
    
    public MethodDescriptor() {
        this(null, null);
    }
    
    /** 
     * Used to construct a method descriptor for a method with a return
     * type and parameters.
     */
    public MethodDescriptor(TypeDescriptor ret, TypeDescriptor[] params) {
        if (params == null) {
            params = cEmptyParams;
        }

        mStr = generate(ret, params);
        mRetType = ret;
        mParams = params;
    }

    public TypeDescriptor getReturnType() {
        return mRetType;
    }
    
    public int getParameterCount() {
        return mParams.length;
    }

    public TypeDescriptor[] getParameterTypes() {
        return mParams;
    }

    public String toString() {
        return mStr;
    }
    
    public static String generate() {
        return generate(null, null);
    }
    
    /** 
     * Used to generate a method descriptor for a method with a return
     * type and parameters.
     */
    public static String generate(TypeDescriptor ret, 
                                  TypeDescriptor[] params) {

        StringBuffer desc = new StringBuffer(20);
        
        desc.append('(');
        
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                desc.append(params[i]);
            }
        }
        
        desc.append(')');
        
        if (ret == null) ret = new TypeDescriptor(Void.TYPE);
        desc.append(ret);
        
        return desc.toString();
    }

    public static MethodDescriptor parseMethodDesc(String desc) 
        throws IllegalArgumentException {

        MethodDescriptor md;
        int cursor = 0;
        try {
            char c;

            if ((c = desc.charAt(cursor++)) != '(') {
                throw new IllegalArgumentException
                    ("Invalid descriptor: " + desc);
            }

            StringBuffer buf = new StringBuffer();
            List list = new ArrayList();

            while ((c = desc.charAt(cursor++)) != ')') {
                switch (c) {
                case 'V':
                case 'I':
                case 'C':
                case 'Z':
                case 'D':
                case 'F':
                case 'J':
                case 'B':
                case 'S':
                    buf.append(c);
                    break;
                case '[':
                    buf.append(c);
                    continue;
                case 'L':
                    while (true) {
                        buf.append(c);
                        if (c == ';') {
                            break;
                        }
                        c = desc.charAt(cursor++);
                    }
                    break;
                default:
                    throw new IllegalArgumentException
                        ("Invalid descriptor: " + desc);
                }

                list.add(TypeDescriptor.parseTypeDesc(buf.toString()));
                buf.setLength(0);
            }

            TypeDescriptor ret = 
                TypeDescriptor.parseTypeDesc(desc.substring(cursor));

            TypeDescriptor[] tds = new TypeDescriptor[list.size()];
            tds = (TypeDescriptor[])list.toArray(tds);

            md = new MethodDescriptor(ret, tds);
        }
        catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }

        return md;
    }
}
