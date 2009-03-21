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
import java.io.PrintWriter;

/******************************************************************************
 * CodeAssembler implementation that prints out instructions using a Java-like
 * syntax that matches the methods of CodeAssembler. When used in conjunction
 * with a {@link CodeDisassembler}, this class makes it easier to understand
 * how to use a CodeAssembler.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/11/27 <!-- $-->
 */
public class CodeAssemblerPrinter implements CodeAssembler {
    private TypeDescriptor[] mParamTypes;
    private boolean mIsStatic;
    private PrintWriter mWriter;
    private String mLinePrefix;
    private String mLineSuffix;

    private int mLocalCounter;
    private int mLabelCounter;

    private int mTypeDescriptorCounter;
    // Maps TypeDescriptor objects to String variable names.
    private Map mTypeDescriptorNames;

    private int mTypeDescriptorArrayCounter;
    // Maps TypeDescriptor arrays to String variable names.
    private Map mTypeDescriptorArrayNames;

    public CodeAssemblerPrinter(TypeDescriptor[] paramTypes, boolean isStatic,
                              PrintWriter writer)
    {
        this(paramTypes, isStatic, writer, null, null);
    }

    public CodeAssemblerPrinter(TypeDescriptor[] paramTypes, boolean isStatic,
                                PrintWriter writer,
                                String linePrefix, String lineSuffix)
    {
        mParamTypes = paramTypes;
        mIsStatic = isStatic;
        mWriter = writer;
        mLinePrefix = linePrefix;
        mLineSuffix = lineSuffix;
        mTypeDescriptorNames = new HashMap();
        mTypeDescriptorArrayNames = new HashMap();
    }

    public LocalVariable[] getParameters() {
        LocalVariable[] vars = new LocalVariable[mParamTypes.length];

        int varNum = (mIsStatic) ? 0 : 1;
        for (int i = 0; i<mParamTypes.length; i++) {
            String varName = "var_" + (++mLocalCounter);
            println("LocalVariable " + varName + 
                    " = getParameters()[" + i + ']');
            LocalVariable localVar =
                new NamedLocal(varName, mParamTypes[i], varNum);
            varNum += (localVar.isDoubleWord() ? 2 : 1);
            vars[i] = localVar;
        }
        
        return vars;
    }

    public LocalVariable createLocalVariable(String name, 
                                             TypeDescriptor type) {
        String varName = "var_" + (++mLocalCounter);
        if (name != null) {
            name = '"' + name + '"';
        }
        println("LocalVariable " + varName +
                " = createLocalVariable(" + name +
                ", " + getTypeDescriptorName(type) + ')');
        return new NamedLocal(varName, type, -1);
    }

    public Label createLabel() {
        String name = "label_" + (++mLabelCounter);
        println("Label " + name + " = createLabel()");
        return new NamedLabel(name);
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        println("exceptionHandler(" +
                getLabelName(startLocation) + ", " +
                getLabelName(endLocation) + ", " +
                catchClassName + ')');
    }
    
    public void mapLineNumber(int lineNumber) {
        println("mapLineNumber(" + lineNumber + ')');
    }

    public void loadConstant(String value) {
        if (value == null) {
            println("loadConstant(null)");
        }
        else {
            println("loadConstant(\"" + escape(value) + "\")");
        }
    }

    public void loadConstant(boolean value) {
        println("loadConstant(" + value + ')');
    }

    public void loadConstant(int value) {
        println("loadConstant(" + value + ')');
    }

    public void loadConstant(long value) {
        println("loadConstant(" + value + "L)");
    }

    public void loadConstant(float value) {
        println("loadConstant(" + value + "f)");
    }

    public void loadConstant(double value) {
        println("loadConstant(" + value + "d)");
    }

    public void loadLocal(LocalVariable local) {
        println("loadLocal(" + local.getName() + ')');
    }

    public void loadThis() {
        println("loadThis()");
    }

    public void storeLocal(LocalVariable local) {
        println("storeLocal(" + local.getName() + ')');
    }

    public void loadFromArray(Class type) {
        println("loadFromArray(" + generateClassLiteral(type) + ')');
    }

    public void storeToArray(Class type) {
        println("storeToArray(" + generateClassLiteral(type) + ')');
    }

    public void loadField(String fieldName,
                          TypeDescriptor type) {
        println("loadField(\"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDescriptor type) {
        println("loadField(\"" + className + "\", \"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void loadStaticField(String fieldName,
                                TypeDescriptor type) {
        println("loadStaticField(\"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDescriptor type) {
        println("loadStaticField(\"" + className + "\", \"" +
                fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void storeField(String fieldName,
                           TypeDescriptor type) {
        println("storeField(\"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDescriptor type) {
        println("storeField(\"" + className + "\", \"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void storeStaticField(String fieldName,
                                 TypeDescriptor type) {
        println("storeStaticField(\"" + fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDescriptor type) {
        println("storeStaticField(\"" + className + "\", \"" +
                fieldName + "\", " +
                getTypeDescriptorName(type) + ')');
    }

    public void returnVoid() {
        println("returnVoid()");
    }

    public void returnValue(Class type) {
        println("returnValue(" + generateClassLiteral(type) + ')');
    }

    public void convert(Class fromType, Class toType) {
        println("convert(" +
                generateClassLiteral(fromType) + ", " +
                generateClassLiteral(toType) + ')');
    }

    public void invokeVirtual(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        println("invokeVirtual(\"" + methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        println("invokeVirtual(\"" + className + "\", \"" +
                methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeStatic(String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        println("invokeStatic(\"" + methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        println("invokeStatic(\"" + className + ", " +
                methodName + ", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDescriptor ret,
                                TypeDescriptor[] params) {
        println("invokeInterface(\"" + className + "\", \"" +
                methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokePrivate(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        println("invokePrivate(\"" + methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDescriptor ret,
                            TypeDescriptor[] params) {
        println("invokeSuper(\"" + superClassName + "\", \"" +
                methodName + "\", " +
                getTypeDescriptorName(ret) + ", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeConstructor(TypeDescriptor[] params) {
        println("invokeConstructor(" +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeConstructor(String className, TypeDescriptor[] params) {
        println("invokeConstructor(\"" + className + "\", " +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void invokeSuperConstructor(TypeDescriptor[] params) {
        println("invokeSuperConstructor(" +
                getTypeDescriptorArrayName(params) + ')');
    }

    public void newObject(TypeDescriptor type) {
        println("newObject(" + getTypeDescriptorName(type) + ')');
    }

    public void dup() {
        println("dup()");
    }

    public void dupX1() {
        println("dupX1()");
    }

    public void dupX2() {
        println("dupX2()");
    }

    public void dup2() {
        println("dup2()");
    }

    public void dup2X1() {
        println("dup2X1()");
    }

    public void dup2X2() {
        println("dup2X2()");
    }

    public void pop() {
        println("pop()");
    }

    public void pop2() {
        println("pop2()");
    }

    public void swap() {
        println("swap()");
    }

    public void swap2() {
        println("swap2()");
    }

    public void branch(Location location) {
        println("branch(" + getLabelName(location) + ')');
    }

    public void ifNullBranch(Location location, boolean choice) {
        println("ifNullBranch(" +
                getLabelName(location) + ", " + choice + ')');
    }

    public void ifEqualBranch(Location location, boolean choice) {
        println("ifEqualBranch(" +
                getLabelName(location) + ", " + choice + ')');
    }

    public void ifZeroComparisonBranch(Location location, String choice) {
        println("ifZeroComparisonBranch(" +
                getLabelName(location) + ", \"" + choice + "\")");
    }

    public void ifComparisonBranch(Location location, String choice) {
        println("ifComparisonBranch(" +
                getLabelName(location) + ", \"" + choice + "\")");
    }

    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {

        StringBuffer buf = new StringBuffer(cases.length * 15);

        buf.append("switchBranch(");

        buf.append("new int[] {");
        for (int i=0; i<cases.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(cases[i]);
        }
        buf.append("}");

        buf.append(", ");

        buf.append("new Location[] {");
        for (int i=0; i<locations.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(getLabelName(locations[i]));
        }
        buf.append("}");

        buf.append(", ");
        buf.append(getLabelName(defaultLocation));
        buf.append(')');

        println(buf.toString());
    }

    public void jsr(Location location) {
        println("jsr(" + getLabelName(location) + ')');
    }

    public void ret(LocalVariable local) {
        println("ret(" + local.getName() + ')');
    }

    public void math(byte opcode) {
        println
            ("math(Opcode." + Opcode.getMnemonic(opcode).toUpperCase() + ')');
    }

    public void arrayLength() {
        println("arrayLength()");
    }

    public void throwObject() {
        println("throwObject()");
    }

    public void checkCast(TypeDescriptor type) {
        println("checkCast(" + getTypeDescriptorName(type) + ')');
    }

    public void instanceOf(TypeDescriptor type) {
        println("instanceOf(" + getTypeDescriptorName(type) + ')');
    }

    public void integerIncrement(LocalVariable local, int amount) {
        println("integerIncrement(" + local.getName() + ", " + amount + ')');
    }

    public void monitorEnter() {
        println("monitorEnter()");
    }

    public void monitorExit() {
        println("monitorExit()");
    }

    public void nop() {
        println("nop()");
    }

    public void breakpoint() {
        println("breakpoint()");
    }

    private void println(String str) {
        if (mLinePrefix != null) {
            mWriter.print(mLinePrefix);
        }
        if (mLineSuffix == null) {
            mWriter.println(str);
        }
        else {
            mWriter.print(str);
            mWriter.println(mLineSuffix);
        }
    }

    private String generateClassLiteral(Class type) {
        StringBuffer buf = new StringBuffer();

        int dim = 0;
        while (type.isArray()) {
            dim++;
            type = type.getComponentType();
        }
        
        if (!type.isPrimitive()) {
            buf.append(type.getName());
        }
        else {
            if (type == int.class) {
                buf.append("int");
            }
            else if (type == char.class) {
                buf.append("char");
            }
            else if (type == boolean.class) {
                buf.append("boolean");
            }
            else if (type == double.class) {
                buf.append("double");
            }
            else if (type == float.class) {
                buf.append("float");
            }
            else if (type == long.class) {
                buf.append("long");
            }
            else if (type == byte.class) {
                buf.append("byte");
            }
            else if (type == short.class) {
                buf.append("short");
            }
            else {
                buf.append("void");
            }
        }
        
        while (dim-- > 0) {
            buf.append('[');
            buf.append(']');
        }

        buf.append(".class");

        return buf.toString();
    }

    private String getLabelName(Location location) {
        if (location instanceof NamedLabel) {
            return ((NamedLabel)location).mName;
        }
        else {
            return ((NamedLabel)createLabel()).mName;
        }
    }

    private String getTypeDescriptorName(TypeDescriptor type) {
        if (type == null) {
            return "null";
        }

        String name = (String)mTypeDescriptorNames.get(type);

        if (name == null) {
            name = "type_" + (++mTypeDescriptorCounter);
            mTypeDescriptorNames.put(type, name);

            StringBuffer buf = new StringBuffer("TypeDescriptor ");
            buf.append(name);
            buf.append(" = new TypeDescriptor(");

            TypeDescriptor componentType = type.getComponentType();
            if (componentType != null) {
                buf.append(getTypeDescriptorName(componentType));
                buf.append(", ");
                buf.append(type.getSpecifiedDimensions());
            }
            else {
                Class clazz = type.getClassArg();
                if (clazz != null) {
                    buf.append(generateClassLiteral(clazz));
                }
                else {
                    buf.append('"');
                    buf.append(type.getClassName());
                    buf.append('"');
                }
            }

            buf.append(')');

            println(buf.toString());
        }

        return name;
    }

    private String getTypeDescriptorArrayName(TypeDescriptor[] types) {
        if (types == null) {
            return "null";
        }

        Object key = Arrays.asList(types);
        String name = (String)mTypeDescriptorArrayNames.get(key);

        if (name == null) {
            name = "params_" + (++mTypeDescriptorArrayCounter);
            mTypeDescriptorArrayNames.put(key, name);

            StringBuffer buf = new StringBuffer("TypeDescriptor[] ");
            buf.append(name);
            buf.append(" = new TypeDescriptor[] {");

            for (int i=0; i<types.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(getTypeDescriptorName(types[i]));
            }

            buf.append('}');

            println(buf.toString());
        }

        return name;
    }

    private String escape(String value) {
        int length = value.length();
        int i = 0;
        for (; i < length; i++) {
            char c = value.charAt(i);
            if (c < 32 || c > 126 || c == '"' || c == '\\') {
                break;
            }
        }

        if (i >= length) {
            return value;
        }

        StringBuffer buf = new StringBuffer(length + 16);
        for (i=0; i<length; i++) {
            char c = value.charAt(i);
            if (c >= 32 && c <= 126 && c != '"' && c != '\\') {
                buf.append(c);
                continue;
            }

            switch (c) {
            case '\0':
                buf.append("\\0");
                break;
            case '"':
                buf.append("\\\"");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            case '\b':
                buf.append("\\b");
                break;
            case '\f':
                buf.append("\\f");
                break;
            case '\n':
                buf.append("\\n");
                break;
            case '\r':
                buf.append("\\r");
                break;
            case '\t':
                buf.append("\\t");
                break;
            default:
                String u = Integer.toHexString(c).toLowerCase();
                buf.append("\\u");
                for (int len = u.length(); len < 4; len++) {
                    buf.append('0');
                }
                buf.append(u);
                break;
            }
        }

        return buf.toString();
    }

    private class NamedLocal implements LocalVariable {
        private String mName;
        private TypeDescriptor mType;
        private boolean mIsDoubleWord;
        private int mNumber;

        public NamedLocal(String name, TypeDescriptor type, int number) {
            mName = name;
            mType = type;
            Class clazz = type.getClassArg();
            mIsDoubleWord = clazz == long.class || clazz == double.class;
            mNumber = number;
        }

        public String getName() {
            return mName;
        }
        
        public void setName(String name) {
            println(mName + ".setName(" + name + ')');
        }
        
        public TypeDescriptor getType() {
            return mType;
        }
        
        public boolean isDoubleWord() {
            return mIsDoubleWord;
        }
        
        public int getNumber() {
            return mNumber;
        }
        
        public Location getStartLocation() {
            return null;
        }
        
        public Location getEndLocation() {
            return null;
        }

        public SortedSet getLocationRangeSet() {
            return null;
        }
    }

    private class NamedLabel implements Label {
        public final String mName;

        public NamedLabel(String name) {
            mName = name;
        }

        public Label setLocation() {
            println(mName + ".setLocation()");
            return this;
        }
        
        public int getLocation() {
            return -1;
        }

        public int compareTo(Object obj) {
            return 0;
        }
    }
}
