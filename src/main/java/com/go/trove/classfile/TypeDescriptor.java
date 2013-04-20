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

/******************************************************************************
 * This class is used to build field and return type descriptor strings as 
 * defined in <i>The Java Virtual Machine Specification</i>, section 4.3.2.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 * @see ClassFile
 */
public class TypeDescriptor extends Descriptor {
    private String mStr;
    private String mClassName;
    private Class mClass;
    private int mDim;
    private TypeDescriptor mComponentType;
    private int mSpecifiedDim;
    
    public TypeDescriptor(String className) {
        mClassName = className;
        mStr = generate(className);
    }

    /** 
     * Used to construct any kind of type descriptor including objects and
     * primitive types.
     */
    public TypeDescriptor(Class clazz) {
        int dim = 0;

        // Adjust dimension count.
        while (clazz.isArray()) {
            dim++;
            clazz = clazz.getComponentType();
        }

        mClassName = clazz.getName();
        mClass = clazz;
        mDim = dim;

        mStr = generate(clazz, dim);
    }

    /**
     * Used to construct an array TypeDescriptor.
     *
     * @param componentType the component type
     * @param dim the number of dimensions, which must be greater than zero.
     *
     * @exception IllegalArgumentException when the dimensions is not greater
     * than zero.
     */
    public TypeDescriptor(TypeDescriptor componentType, int dim) 
        throws IllegalArgumentException {

        if (dim <= 0) {
            throw new IllegalArgumentException
                ("Array dimensions must be greater than zero: " + dim);
        }

        mClassName = componentType.getClassName();
        mClass = componentType.getClassArg();
        mDim = componentType.getDimensions() + dim;

        if (mClass != null) {
            mStr = generate(mClass, mDim);
        }
        else {
            mStr = generate(mClassName, mDim);
        }

        mComponentType = componentType;
        mSpecifiedDim = dim;
    }

    public String getClassName() {
        return mClassName;
    }

    /**
     * If this TypeDescriptor was constructed with a Class argument, then
     * this method retrieves that argument. If the Class argument represented
     * an array, the component type is returned. i.e. for
     * <tt>TypeDescriptor(Object[][].class)</tt>, Object is returned.
     *
     * <p>If this TypeDescriptor was constructed with a String argument,
     * null is returned. Call getClassName instead in this case.
     *
     * @return A Class that never represents an array.
     */
    public Class getClassArg() {
        return mClass;
    }

    /**
     * If this TypeDescriptor represents an array, the component type is
     * returned. Otherwise, null is returned. i.e. for
     * <tt>TypeDescriptor(Object[].class, 1)</tt>, Object[] is returned,
     * and for <tt>TypeDescriptor(Object[].class)</tt>, Object is returned.
     */
    public TypeDescriptor getComponentType() {
        if (mComponentType == null && mDim > 0) {
            if (mClass != null) {
                mComponentType = new TypeDescriptor(mClass);
            }
            else {
                mComponentType = new TypeDescriptor(mClassName);
            }
        }

        return mComponentType;
    }

    /**
     * Returns the dimensions after being adjusted. i.e. 
     * <tt>TypeDescriptor(Object[][].class)</tt> has two dimensions and
     * <tt>TypeDescriptor(TypeDescriptor(Object[].class), 2)</tt> has three
     * dimensions.
     */
    public int getDimensions() {
        return mDim;
    }

    /**
     * Returns the dimensions as originally specified in the constructor. i.e.
     * <tt>new TypeDescriptor(new TypeDescriptor(Object[].class), 2)</tt>
     * has two specified dimensions. If no dimensions were specified, the 
     * results of getDimensions are returned.
     */
    public int getSpecifiedDimensions() {
        return (mSpecifiedDim != 0) ? mSpecifiedDim : mDim;
    }

    public int hashCode() {
        return mStr.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof TypeDescriptor) {
            return ((TypeDescriptor)other).mStr.equals(mStr);
        }
        else {
            return false;
        }
    }

    /**
     * Returns the TypeDescriptor code.
     */
    public String toString() {
        return mStr;
    }
    
    static String generate(String className) {
        return generate(className, 0);
    }
    
    /** 
     * Used to generate an array type descriptor. 
     */
    static String generate(String className, int dim) {
        StringBuffer desc = new StringBuffer(className.length() + dim + 2);
        
        while (dim-- > 0) {
            desc.append('[');
        }
        
        desc.append('L');
        desc.append(className.replace('.', '/'));
        desc.append(';');
        
        return desc.toString();
    }
    
    /** 
     * Used to generate any kind of type descriptor including arrays and
     * primitive types.
     */
    static String generate(Class clazz) {
        return generate(clazz, 0);
    }

    static String generate(Class clazz, int dim) {
        // Get component type and dimensions if an array.
        while (clazz.isArray()) {
            dim++;
            clazz = clazz.getComponentType();
        }

        if (!clazz.isPrimitive()) {
            return generate(clazz.getName(), dim);
        }
        
        StringBuffer desc = new StringBuffer(dim + 1);
        
        while (dim-- > 0) {
            desc.append('[');
        }
        
        char type = 'V';
        
        if (clazz == int.class)
            type = 'I';
        else if (clazz == char.class) 
            type = 'C';
        else if (clazz == boolean.class)
            type = 'Z';
        else if (clazz == double.class)
            type = 'D';
        else if (clazz == float.class)
            type = 'F';
        else if (clazz == long.class)
            type = 'J';
        else if (clazz == byte.class)
            type = 'B';
        else if (clazz == short.class)
            type = 'S';
        
        desc.append(type);
        
        return desc.toString();
    }

    public static TypeDescriptor parseTypeDesc(String desc) 
        throws IllegalArgumentException {

        TypeDescriptor td = null;
        int cursor = 0;
        try {
            int dim = 0;
            char c;
            while ((c = desc.charAt(cursor++)) == '[') {
                dim++;
            }

            Class primitiveClass = null;

            switch (c) {
            case 'V':
                primitiveClass = void.class;
                break;
            case 'I':
                primitiveClass = int.class;
                break;
            case 'C':
                primitiveClass = char.class;
                break;
            case 'Z':
                primitiveClass = boolean.class;
                break;
            case 'D':
                primitiveClass = double.class;
                break;
            case 'F':
                primitiveClass = float.class;
                break;
            case 'J':
                primitiveClass = long.class;
                break;
            case 'B':
                primitiveClass = byte.class;
                break;
            case 'S':
                primitiveClass = short.class;
                break;
            case 'L':
                StringBuffer name = new StringBuffer(desc.length());
                while ((c = desc.charAt(cursor++)) != ';') {
                    if (c == '/') {
                        c = '.';
                    }
                    name.append(c);
                }

                td = new TypeDescriptor(name.toString());

                if (dim > 0) {
                    td = new TypeDescriptor(td, dim);
                }

                break;
            }

            if (primitiveClass != null) {
                td = new TypeDescriptor(primitiveClass);

                if (dim > 0) {
                    td = new TypeDescriptor(td, dim);
                }
            }
        }
        catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }

        if (td == null || cursor != desc.length()) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }

        return td;
    }
}
