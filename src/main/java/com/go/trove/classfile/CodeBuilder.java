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

import java.lang.reflect.*;

/******************************************************************************
 * This class is used as an aid in generating code for a method.
 * It controls the max stack, local variable allocation, labels and bytecode.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/14 <!-- $-->
 */
public class CodeBuilder implements CodeBuffer, CodeAssembler {
    private CodeAttr mCodeAttr;
    private ClassFile mClassFile;
    private ConstantPool mCp;

    private InstructionList mInstructions = new InstructionList();

    private LocalVariable mThisReference;
    private LocalVariable[] mParameters;

    private boolean mSaveLineNumberInfo;
    private boolean mSaveLocalVariableInfo;

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     */
    public CodeBuilder(MethodInfo info) {
        this(info, true, false);
    }

    /**
     * Construct a CodeBuilder for the CodeAttr of the given MethodInfo. The
     * CodeBuffer for the CodeAttr is automatically set to this CodeBuilder.
     *
     * @param saveLineNumberInfo When set false, all calls to mapLineNumber
     * are ignored. By default, this value is true.
     *
     * @param saveLocalVariableInfo When set true, all local variable
     * usage information is saved in the ClassFile. By default, this value
     * is false.
     *
     * @see #mapLineNumber
     */
    public CodeBuilder(MethodInfo info, boolean saveLineNumberInfo,
                       boolean saveLocalVariableInfo) {
        mCodeAttr = info.getCodeAttr();
        mClassFile = info.getClassFile();
        mCp = mClassFile.getConstantPool();

        mCodeAttr.setCodeBuffer(this);

        mSaveLineNumberInfo = saveLineNumberInfo;
        mSaveLocalVariableInfo = saveLocalVariableInfo;

        // Create LocalVariable references for "this" reference and other
        // passed in parameters.

        LocalVariable localVar;
        int varNum = 0;

        if (!info.getAccessFlags().isStatic()) {
            localVar = mInstructions.createLocalParameter
                ("this", mClassFile.getType(), varNum++);
            mThisReference = localVar;

            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }

        TypeDescriptor[] paramTypes = 
            info.getMethodDescriptor().getParameterTypes();
        int paramSize = paramTypes.length;

        mParameters = new LocalVariable[paramSize];

        for (int i = 0; i<paramTypes.length; i++) {
            localVar = mInstructions.createLocalParameter
                (null, paramTypes[i], varNum);
            varNum += (localVar.isDoubleWord() ? 2 : 1);
            mParameters[i] = localVar;

            if (saveLocalVariableInfo) {
                mCodeAttr.localVariableUse(localVar);
            }
        }
    }

    public int getMaxStackDepth() {
        return mInstructions.getMaxStackDepth();
    }

    public int getMaxLocals() {
        return mInstructions.getMaxLocals();
    }

    public byte[] getByteCodes() {
        return mInstructions.getByteCodes();
    }

    public ExceptionHandler[] getExceptionHandlers() {
        return mInstructions.getExceptionHandlers();
    }

    private void addCode(int stackAdjust, byte opcode) {
        mInstructions.new CodeInstruction(stackAdjust, new byte[] {opcode});
    }

    private void addCode(int stackAdjust, byte opcode, byte operand) {
        mInstructions.new CodeInstruction
            (stackAdjust, new byte[] {opcode, operand});
    }

    private void addCode(int stackAdjust, byte opcode, short operand) {
        mInstructions.new CodeInstruction
            (stackAdjust, 
             new byte[] {opcode, (byte)(operand >> 8), (byte)operand});
    }

    private void addCode(int stackAdjust, byte opcode, int operand) {
        byte[] bytes = new byte[5];

        bytes[0] = opcode;
        bytes[1] = (byte)(operand >> 24);
        bytes[2] = (byte)(operand >> 16);
        bytes[3] = (byte)(operand >> 8);
        bytes[4] = (byte)operand;

        mInstructions.new CodeInstruction(stackAdjust, bytes);
    }

    private void addCode(int stackAdjust, byte opcode, ConstantInfo info) {
        // The zeros get filled in later, when the ConstantInfo index 
        // is resolved.
        mInstructions.new ConstantOperandInstruction
            (stackAdjust,
             new byte[] {opcode, (byte)0, (byte)0}, info);
    }

    /**
     * Returns LocalVariable references for all the parameters passed into
     * the method being assembled, not including any "this" reference.
     * Returns a zero-length array if there are no passed in parameters.
     *
     * <p>The names of the LocalVariables returned by this method are initially
     * set to null. It is encouraged that a name be provided.
     */
    public LocalVariable[] getParameters() {
        return (LocalVariable[])mParameters.clone();
    }

    /**
     * Creates a LocalVariable reference from a name and type. Although name
     * is optional, it is encouraged that a name be provided. Names do not 
     * need to be unique.
     *
     * @param name Optional name for the LocalVariable.
     * @param type The type of data that the requested LocalVariable can 
     * store. 
     */
    public LocalVariable createLocalVariable(String name, 
                                             TypeDescriptor type) {
        LocalVariable localVar = mInstructions.createLocalVariable(name, type);

        if (mSaveLocalVariableInfo) {
            mCodeAttr.localVariableUse(localVar);
        }

        return localVar;
    }

    /**
     * Creates a label, whose location must be set. To create a label and
     * locate it here, the following example demonstrates how the call to
     * setLocation can be chained:
     *
     * <pre>
     * CodeBuilder builder;
     * ...
     * Label label = builder.createLabel().setLocation();
     * </pre>
     *
     * @see Label#setLocation
     */ 
    public Label createLabel() {
        return mInstructions.new LabelInstruction();
    }

    /**
     * Sets up an exception handler located here, the location of the next
     * code to be generated.
     *
     * @param startLocation Location or label at the start of the section of
     * code to be wrapped by an exception handler.
     * @param endLocation Location or label directly after the end of the
     * section of code.
     * @param catchClassName The name of the type of exception to be caught;
     * if null, then catch every object.
     */
    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        Location catchLocation = createLabel().setLocation();

        ConstantClassInfo catchClass;
        if (catchClassName == null) {
            catchClass = null;
        }
        else {
            catchClass = ConstantClassInfo.make(mCp, catchClassName);
        }

        ExceptionHandler handler = 
            new ExceptionHandler(startLocation, endLocation, 
                                 catchLocation, catchClass);

        mInstructions.addExceptionHandler(handler);
    }
    
    /**
     * Map the location of the next code to be generated to a line number 
     * in source code. This enables line numbers in a stack trace from the 
     * generated code.
     */
    public void mapLineNumber(int lineNumber) {
        if (mSaveLineNumberInfo) {
            mCodeAttr.mapLineNumber(createLabel().setLocation(), lineNumber);
        }
    }

    // load-constant-to-stack style instructions

    /**
     * Generates code that loads a constant string value onto the stack.
     * If value is null, the generated code loads a null onto the stack.
     * Strings that exceed 65535 UTF encoded bytes in length are loaded by
     * creating a StringBuffer, appending substrings, and then converting to a
     * String.
     */
    public void loadConstant(String value) {
        if (value == null) {
            addCode(1, Opcode.ACONST_NULL);
            return;
        }

        int strlen = value.length();

        if (strlen <= (65535 / 3)) {
            // Guaranteed to fit in a Java UTF encoded string.
            ConstantInfo info = ConstantStringInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
            return;
        }

        // Compute actual UTF length.

        int utflen = 0;

        for (int i=0; i<strlen; i++) {
            int c = value.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            }
            else if (c > 0x07FF) {
                utflen += 3;
            }
            else {
                utflen += 2;
            }
        }

        if (utflen <= 65535) {
            ConstantInfo info = ConstantStringInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
            return;
        }

        // Break string up into chunks and construct in a StringBuffer.

        TypeDescriptor stringBufferDesc = 
            new TypeDescriptor(StringBuffer.class);
        
        TypeDescriptor intDesc = new TypeDescriptor(int.class);
        TypeDescriptor stringDesc = new TypeDescriptor(String.class);
        TypeDescriptor[] stringParam = new TypeDescriptor[] {stringDesc};
        
        newObject(stringBufferDesc);
        dup();
        loadConstant(strlen);
        invokeConstructor("java.lang.StringBuffer",
                          new TypeDescriptor[] {intDesc});
            
        int beginIndex;
        int endIndex = 0;
        
        while (endIndex < strlen) {
            beginIndex = endIndex;

            // Make each chunk as large as possible.
            utflen = 0;
            for (; endIndex < strlen; endIndex++) {
                int c = value.charAt(endIndex);
                int size;
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    size = 1;
                }
                else if (c > 0x07FF) {
                    size = 3;
                }
                else {
                    size = 2;
                }

                if ((utflen + size) > 65535) {
                    break;
                }
                else {
                    utflen += size;
                }
            }

            String substr = value.substring(beginIndex, endIndex);

            ConstantInfo info = ConstantStringInfo.make(mCp, substr);
            mInstructions.new LoadConstantInstruction(1, info);

            invokeVirtual("java.lang.StringBuffer", "append",
                          stringBufferDesc, stringParam);
        }
        
        invokeVirtual("java.lang.StringBuffer", "toString",
                      stringDesc, null);
    }

    /**
     * Generates code that loads a constant boolean value onto the stack.
     */
    public void loadConstant(boolean value) {
        loadConstant(value?1:0);
    }

    /**
     * Generates code that loads a constant int, char, short or byte value 
     * onto the stack.
     */
    public void loadConstant(int value) {
        if (-1 <= value && value <= 5) {
            byte op;

            switch(value) {
            case -1:
                op = Opcode.ICONST_M1;
                break;
            case 0:
                op = Opcode.ICONST_0;
                break;
            case 1:
                op = Opcode.ICONST_1;
                break;
            case 2:
                op = Opcode.ICONST_2;
                break;
            case 3:
                op = Opcode.ICONST_3;
                break;
            case 4:
                op = Opcode.ICONST_4;
                break;
            case 5:
                op = Opcode.ICONST_5;
                break;
            default:
                op = Opcode.NOP;
            }

            addCode(1, op);
        }
        else if (-128 <= value && value <= 127) {
            addCode(1, Opcode.BIPUSH, (byte)value);
        }
        else if (-32768 <= value && value <= 32767) {
            addCode(1, Opcode.SIPUSH, (short)value);
        }
        else {
            ConstantInfo info = ConstantIntegerInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
        }
    }

    /**
     * Generates code that loads a constant long value onto the stack.
     */
    public void loadConstant(long value) {
        if (value == 0) {
            addCode(2, Opcode.LCONST_0);
        }
        else if (value == 1) {
            addCode(2, Opcode.LCONST_1);
        }
        else {
            ConstantInfo info = ConstantLongInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(2, info, true);
        }
    }

    /**
     * Generates code that loads a constant float value onto the stack.
     */
    public void loadConstant(float value) {
        if (value == 0) {
            addCode(1, Opcode.FCONST_0);
        }
        else if (value == 1) {
            addCode(1, Opcode.FCONST_1);
        }
        else if (value == 2) {
            addCode(1, Opcode.FCONST_2);
        }
        else {
            ConstantInfo info = ConstantFloatInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(1, info);
        }
    }

    /**
     * Generates code that loads a constant double value onto the stack.
     */
    public void loadConstant(double value) {
        if (value == 0) {
            addCode(2, Opcode.DCONST_0);
        }
        else if (value == 1) {
            addCode(2, Opcode.DCONST_1);
        }
        else {
            ConstantInfo info = ConstantDoubleInfo.make(mCp, value);
            mInstructions.new LoadConstantInstruction(2, info, true);
        }
    }

    // load-local-to-stack style instructions

    /**
     * Generates code that loads a local variable onto the stack. Parameters
     * passed to a method and the "this" reference are all considered local
     * variables, as well as any that were created.
     *
     * @param local The local variable reference
     * @see #getParameters
     * @see #createLocalVariable
     */
    public void loadLocal(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        TypeDescriptor type = local.getType();
        Class clazz = type.getClassArg();

        int stackAdjust = 1;

        if (clazz != null && type.getDimensions() == 0 && 
            (clazz == long.class || clazz == double.class)) {

            stackAdjust++;
        }

        mInstructions.new LoadLocalInstruction(stackAdjust, local);
    }

    /**
     * Loads a reference to "this" onto the stack. Static methods have no 
     * "this" reference, and an exception is thrown when attempting to
     * generate "this" in a static method.
     */
    public void loadThis() {
        if (mThisReference != null) {
            loadLocal(mThisReference);
        }
        else {
            throw new RuntimeException
                ("Attempt to load \"this\" reference in a static method");
        }
    }

    // store-from-stack-to-local style instructions

    /**
     * Generates code that pops a value off of the stack into a local variable.
     * Parameters passed to a method and the "this" reference are all 
     * considered local variables, as well as any that were created.
     *
     * @param local The local variable reference
     * @see #getParameters
     * @see #createLocalVariable
     */
    public void storeLocal(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        TypeDescriptor type = local.getType();
        Class clazz = type.getClassArg();

        int stackAdjust = -1;

        if (clazz != null && type.getDimensions() == 0 && 
            (clazz == long.class || clazz == double.class)) {

            stackAdjust--;
        }

        mInstructions.new StoreLocalInstruction(stackAdjust, local);
    }

    // load-to-stack-from-array style instructions

    /**
     * Generates code that loads a value from an array. An array
     * reference followed by an index must be on the stack. The array 
     * reference and index are replaced by the value retrieved from the array 
     * after the generated instruction has executed.
     *
     * <p>The type doesn't need to be an exact match for objects. Object.class
     * works fine for all objects. For primitive types, use the class that
     * matches that type. For an int the type is int.class.
     *
     * @param type The type of data stored in the array.
     */
    public void loadFromArray(Class type) {
        byte op;
        int stackAdjust;

        if (type == int.class) {
            stackAdjust = -1;
            op = Opcode.IALOAD;
        }
        else if (type == boolean.class || type == byte.class) {
            stackAdjust = -1;
            op = Opcode.BALOAD;
        }
        else if (type == short.class) {
            stackAdjust = -1;
            op = Opcode.SALOAD;
        }
        else if (type == char.class) {
            stackAdjust = -1;
            op = Opcode.CALOAD;
        }
        else if (type == long.class) {
            stackAdjust = 0;
            op = Opcode.LALOAD;
        }
        else if (type == float.class) {
            stackAdjust = -1;
            op = Opcode.FALOAD;
        }
        else if (type == double.class) {
            stackAdjust = 0;
            op = Opcode.DALOAD;
        }
        else {
            stackAdjust = -1;
            op = Opcode.AALOAD;
        }

        addCode(stackAdjust, op);
    }

    // store-to-array-from-stack style instructions

    /**
     * Generates code that stores a value to an array. An array
     * reference followed by an index, followed by a value (or two if a long
     * or double) must be on the stack. All items on the stack are gone
     * after the generated instruction has executed.
     *
     * <p>The type doesn't need to be an exact match for objects. Object.class
     * works fine for all objects. For primitive types, use the class that
     * matches that type. For an int the type is int.class.
     *
     * @param type The type of data stored in the array.
     */
    public void storeToArray(Class type) {
        byte op;
        int stackAdjust;

        if (type == int.class) {
            stackAdjust = -3;
            op = Opcode.IASTORE;
        }
        else if (type == boolean.class || type == byte.class) {
            stackAdjust = -3;
            op = Opcode.BASTORE;
        }
        else if (type == short.class) {
            stackAdjust = -3;
            op = Opcode.SASTORE;
        }
        else if (type == char.class) {
            stackAdjust = -3;
            op = Opcode.CASTORE;
        }
        else if (type == long.class) {
            stackAdjust = -4;
            op = Opcode.LASTORE;
        }
        else if (type == float.class) {
            stackAdjust = -3;
            op = Opcode.FASTORE;
        }
        else if (type == double.class) {
            stackAdjust = -4;
            op = Opcode.DASTORE;
        }
        else {
            stackAdjust = -3;
            op = Opcode.AASTORE;
        }

        addCode(stackAdjust, op);
    }

    // load-field-to-stack style instructions

    /**
     * Generates code that loads a value from a field from this class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    public void loadField(String fieldName,
                          TypeDescriptor type) {
        getfield(0, Opcode.GETFIELD, constantField(fieldName, type), type);
    }

    /**
     * Generates code that loads a value from a field from any class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    public void loadField(String className,
                          String fieldName,
                          TypeDescriptor type) {

        getfield(0, Opcode.GETFIELD,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    /**
     * Generates code that loads a value from a static field from this class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    public void loadStaticField(String fieldName,
                                TypeDescriptor type) {

        getfield(1, Opcode.GETSTATIC, constantField(fieldName, type), type);
    }

    /**
     * Generates code that loads a value from a static field from any class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    public void loadStaticField(String className,
                                String fieldName,
                                TypeDescriptor type) {

        getfield(1, Opcode.GETSTATIC,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    private void getfield(int stackAdjust, byte opcode, ConstantInfo info, 
                          TypeDescriptor type) {

        Class clazz = type.getClassArg();
        if (clazz != null && type.getDimensions() == 0 &&
            (clazz == long.class || clazz == double.class)) {
            stackAdjust++;
        }

        addCode(stackAdjust, opcode, info);
    }

    private ConstantFieldInfo constantField(String fieldName,
                                            TypeDescriptor type) {
        return mCp.addConstantField
            (mClassFile.getClassName(), fieldName, type);
    }

    // store-to-field-from-stack style instructions

    /**
     * Generates code that stores a value into a field from this class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    public void storeField(String fieldName,
                           TypeDescriptor type) {

        putfield(-1, Opcode.PUTFIELD, constantField(fieldName, type), type);
    }

    /**
     * Generates code that stores a value into a field from any class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    public void storeField(String className,
                           String fieldName,
                           TypeDescriptor type) {

        putfield(-1, Opcode.PUTFIELD, 
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    /**
     * Generates code that stores a value into a field from this class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    public void storeStaticField(String fieldName,
                                 TypeDescriptor type) {

        putfield(0, Opcode.PUTSTATIC, constantField(fieldName, type), type);
    }

    /**
     * Generates code that stores a value into a field from any class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDescriptor type) {

        putfield(0, Opcode.PUTSTATIC,
                 mCp.addConstantField(className, fieldName, type), 
                 type);
    }

    private void putfield(int stackAdjust, byte opcode, ConstantInfo info, 
                          TypeDescriptor type) {

        Class clazz = type.getClassArg();
        if (clazz != null && type.getDimensions() == 0 && 
            (clazz == long.class || clazz == double.class)) {
        
            stackAdjust -= 2;
        }
        else {
            stackAdjust--;
        }

        addCode(stackAdjust, opcode, info);
    }

    // return style instructions

    /**
     * Generates code that returns void.
     */
    public void returnVoid() {
        addCode(0, Opcode.RETURN);
    }

    /**
     * Generates code that returns an object or primitive type. The value to
     * return must be on the stack.
     *
     * <p>The type doesn't need to be an exact match for objects. Object.class
     * works fine for all objects. For primitive types, use the class that
     * matches that type. For an int the type is int.class.
     */
    public void returnValue(Class type) {
        int stackAdjust = -1;
        byte op;

        if (type == int.class || 
            type == boolean.class || 
            type == byte.class ||
            type == short.class ||
            type == char.class) {

            op = Opcode.IRETURN;
        }
        else if (type == long.class) {
            stackAdjust--;
            op = Opcode.LRETURN;
        }
        else if (type == float.class) {
            op = Opcode.FRETURN;
        }
        else if (type == double.class) {
            stackAdjust--;
            op = Opcode.DRETURN;
        }
        else if (type == void.class) {
            stackAdjust++;
            op = Opcode.RETURN;
        }
        else {
            op = Opcode.ARETURN;
        }

        addCode(stackAdjust, op);
    }

    // numerical conversion style instructions

    /**
     * Generates code that converts the value of a primitive type already
     * on the stack.
     */
    public void convert(Class fromType, Class toType) {
        int stackAdjust = 0;
        byte op;

        if (fromType == int.class ||
            fromType == boolean.class || 
            fromType == byte.class ||
            fromType == short.class ||
            fromType == char.class) {
            
            if (toType == byte.class) {
                op = Opcode.I2B;
            }
            else if (toType == short.class) {
                op = Opcode.I2S;
            }
            else if (toType == char.class) {
                op = Opcode.I2C;
            }
            else if (toType == float.class) {
                op = Opcode.I2F;
            }
            else if (toType == long.class) {
                stackAdjust = 1;
                op = Opcode.I2L;
            }
            else if (toType == double.class) {
                stackAdjust = 1;
                op = Opcode.I2D;
            }
            else if (toType == int.class) {
                return;
            }
            else {
                throw new RuntimeException("Invalid conversion: int to " +
                                           toType);
            }

            addCode(stackAdjust, op);
            return;
        }
        else if (fromType == long.class) {
            if (toType == int.class) {
                stackAdjust = -1;
                op = Opcode.L2I;
            }
            else if (toType == float.class) {
                stackAdjust = -1;
                op = Opcode.L2F;
            }
            else if (toType == double.class) {
                op = Opcode.L2D;
            }
            else if (toType == byte.class ||
                     toType == char.class ||
                     toType == short.class) {
                
                convert(fromType, int.class);
                convert(int.class, toType);
                return;
            }
            else if (toType == long.class) {
                return;
            }
            else {
                throw new RuntimeException("Invalid conversion: long to " +
                                           toType);
            }

            addCode(stackAdjust, op);
            return;
        }
        else if (fromType == float.class) {
            if (toType == int.class) {
                op = Opcode.F2I;
            }
            else if (toType == long.class) {
                stackAdjust = 1;
                op = Opcode.F2L;
            }
            else if (toType == double.class) {
                stackAdjust = 1;
                op = Opcode.F2D;
            }
            else if (toType == byte.class ||
                     toType == char.class ||
                     toType == short.class) {
                
                convert(fromType, int.class);
                convert(int.class, toType);
                return;
            }
            else if (toType == float.class) {
                return;
            }
            else {
                throw new RuntimeException("Invalid conversion: float to " +
                                           toType);
            }
            
            addCode(stackAdjust, op);
            return;
        }
        else if (fromType == double.class) {
            if (toType == int.class) {
                stackAdjust = -1;
                op = Opcode.D2I;
            }
            else if (toType == float.class) {
                stackAdjust = -1;
                op = Opcode.D2F;
            }
            else if (toType == long.class) {
                op = Opcode.D2L;
            }
            else if (toType == byte.class ||
                     toType == char.class ||
                     toType == short.class) {
                
                convert(fromType, int.class);
                convert(int.class, toType);
                return;
            }
            else if (toType == double.class) {
                return;
            }
            else {
                throw new RuntimeException("Invalid conversion: double to " +
                                           toType);
            }

            addCode(stackAdjust, op);
            return;
        }
        else {
            throw new RuntimeException("Invalid conversion: " + fromType +
                                       " to " + toType);
        }
    }

    // invocation style instructions

    /**
     * Generates code to invoke a method in any class. If the method is
     * non-static, the object reference and the method's argument(s) must be 
     * on the stack. If the method is static and has any arguments, just 
     * the method's arguments must be on the stack.
     */
    public void invoke(Method method) {
        TypeDescriptor ret = new TypeDescriptor(method.getReturnType());

        Class[] paramClasses = method.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        Class clazz = method.getDeclaringClass();

        if (Modifier.isStatic(method.getModifiers())) {
            invokeStatic(clazz.getName(),
                         method.getName(),
                         ret, 
                         params);
        }
        else if (clazz.isInterface()) {
            invokeInterface(clazz.getName(),
                            method.getName(),
                            ret, 
                            params);
        }
        else {
            invokeVirtual(clazz.getName(),
                          method.getName(),
                          ret, 
                          params);
        }
    }

    /**
     * Generates code to invoke a class constructor in any class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     */
    public void invoke(Constructor constructor) {
        Class[] paramClasses = constructor.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        invokeConstructor(constructor.getDeclaringClass().toString(), params);
    }

    /**
     * Generates code to invoke a virtual method in this class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeVirtual(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {

        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKEVIRTUAL, info);
    }

    /**
     * Generates code to invoke a virtual method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeVirtual(String className,
                              String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        ConstantInfo info = 
            mCp.addConstantMethod(className, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKEVIRTUAL, info);
    }

    /**
     * Generates code to invoke a static method in this class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeStatic(String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 0;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESTATIC, info);
    }

    /**
     * Generates code to invoke a static method in any class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeStatic(String className,
                             String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        ConstantInfo info =
            mCp.addConstantMethod(className, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 0;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESTATIC, info);
    }

    /**
     * Generates code to invoke an interface method in any class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeInterface(String className,
                                String methodName,
                                TypeDescriptor ret,
                                TypeDescriptor[] params) {

        ConstantInfo info = 
            mCp.addConstantInterfaceMethod(className, methodName, ret, params);

        int paramCount = 1;
        if (params != null) {
            paramCount += argSize(params);
        }

        int stackAdjust = returnSize(ret) - paramCount;

        byte[] bytes = new byte[5];

        bytes[0] = Opcode.INVOKEINTERFACE;
        bytes[1] = (byte)0;
        bytes[2] = (byte)0;
        bytes[3] = (byte)paramCount;
        bytes[4] = (byte)0;

        mInstructions.new ConstantOperandInstruction(stackAdjust, bytes, info);
    }

    /**
     * Generates code to invoke a private method in this class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokePrivate(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        ConstantInfo info = mCp.addConstantMethod
            (mClassFile.getClassName(), methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    /**
     * Generates code to invoke a method in the super class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDescriptor ret,
                            TypeDescriptor[] params) {
        ConstantInfo info = 
            mCp.addConstantMethod(superClassName, methodName, ret, params);

        int stackAdjust = returnSize(ret) - 1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    /**
     * Generates code to invoke a method in the super class.
     * The object reference and the method's argument(s) must be on the stack.
     */
    public void invokeSuper(Method method) {
        TypeDescriptor ret = new TypeDescriptor(method.getReturnType());

        Class[] paramClasses = method.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        invokeSuper(method.getDeclaringClass().getName(),
                    method.getName(),
                    ret, 
                    params);
    }

    /**
     * Generates code to invoke a class constructor in this class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeConstructor(TypeDescriptor[] params) {
        ConstantInfo info = 
            mCp.addConstantConstructor(mClassFile.getClassName(), params);

        int stackAdjust = -1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    /**
     * Generates code to invoke a class constructor in any class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeConstructor(String className, TypeDescriptor[] params) {
        ConstantInfo info = mCp.addConstantConstructor(className, params);

        int stackAdjust = -1;
        if (params != null) {
            stackAdjust -= argSize(params);
        }

        addCode(stackAdjust, Opcode.INVOKESPECIAL, info);
    }

    /**
     * Generates code to invoke a super class constructor. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeSuperConstructor(TypeDescriptor[] params) {
        invokeConstructor(mClassFile.getSuperClassName(), params);
    }

    /**
     * Generates code to invoke a super class constructor. The object 
     * reference and the constructor's argument(s) must be on the stack.
     */
    public void invokeSuper(Constructor constructor) {
        Class[] paramClasses = constructor.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        invokeSuperConstructor(params);
    }

    private int returnSize(TypeDescriptor ret) {
        if (ret == null) return 0;

        String className = ret.getClassName();
        
        if (className.equals(void.class.getName())) {
            return 0;
        }
        else if (className.equals(long.class.getName()) ||
                 className.equals(double.class.getName())) {
            return 2;
        }
        else {
            return 1;
        }
    }

    private int argSize(TypeDescriptor[] params) {
        int size = 0;
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                String className = params[i].getClassName();
                if (params[i].getDimensions() == 0 &&
                    (className.equals(long.class.getName()) ||
                     className.equals(double.class.getName()))) {
                    size += 2;
                }
                else {
                    size++;
                }
            }
        }

        return size;
    }

    // creation style instructions

    /**
     * Generates code to create a new object. Unless the new object is an 
     * array, it is invalid until a constructor method is invoked on it.
     * When creating arrays, the size for each dimension must be on the
     * operand stack.
     *
     * @see #invokeConstructor
     */
    public void newObject(TypeDescriptor type) {
        int dim = type.getSpecifiedDimensions();

        if (dim == 0) {
            ConstantInfo info = mCp.addConstantClass(type);
            addCode(1, Opcode.NEW, info);
            return;
        }

        TypeDescriptor componentType = type.getComponentType();

        if (dim == 1) {
            if (componentType.getDimensions() == 0) {
                Class clazz = componentType.getClassArg();

                if (clazz != null && clazz.isPrimitive()) {
                    byte atype = (byte)0;
                    
                    if (clazz == int.class) {
                        atype = (byte)10;
                    }
                    else if (clazz == byte.class) {
                        atype = (byte)8;
                    }
                    else if (clazz == boolean.class) {
                        atype = (byte)4;
                    }
                    else if (clazz == char.class) {
                        atype = (byte)5;
                    }
                    else if (clazz == float.class) {
                        atype = (byte)6;
                    }
                    else if (clazz == double.class) {
                        atype = (byte)7;
                    }
                    else if (clazz == short.class) {
                        atype = (byte)9;
                    }
                    else if (clazz == long.class) {
                        atype = (byte)11;
                    }
                    
                    addCode(0, Opcode.NEWARRAY, atype);
                    return;
                }
            }

            ConstantInfo info = mCp.addConstantClass(componentType);
            addCode(0, Opcode.ANEWARRAY, info);
            return;
        }

        // multidimensional
        int stackAdjust = -(dim - 1);
        
        ConstantInfo info = mCp.addConstantClass(componentType);
        
        byte[] bytes = new byte[4];
        
        bytes[0] = Opcode.MULTIANEWARRAY;
        bytes[1] = (byte)0;
        bytes[2] = (byte)0;
        bytes[3] = (byte)dim;
        
        mInstructions.new ConstantOperandInstruction(stackAdjust, bytes, info);
    }

    // stack operation style instructions

    /**
     * Generates code for the dup instruction.
     */
    public void dup() {
        addCode(1, Opcode.DUP);
    }

    /**
     * Generates code for the dup_x1 instruction.
     */
    public void dupX1() {
        addCode(1, Opcode.DUP_X1);
    }

    /**
     * Generates code for the dup_x2 instruction.
     */
    public void dupX2() {
        addCode(1, Opcode.DUP_X2);
    }

    /**
     * Generates code for the dup2 instruction.
     */
    public void dup2() {
        addCode(2, Opcode.DUP2);
    }

    /**
     * Generates code for the dup2_x1 instruction.
     */
    public void dup2X1() {
        addCode(2, Opcode.DUP2_X1);
    }

    /**
     * Generates code for the dup2_x2 instruction.
     */
    public void dup2X2() {
        addCode(2, Opcode.DUP2_X2);
    }

    /**
     * Generates code for the pop instruction.
     */
    public void pop() {
        addCode(-1, Opcode.POP);
    }

    /**
     * Generates code for the pop2 instruction.
     */
    public void pop2() {
        addCode(-2, Opcode.POP2);
    }

    /**
     * Generates code for the swap instruction.
     */
    public void swap() {
        addCode(0, Opcode.SWAP);
    }

    /**
     * Generates code for a swap2 instruction.
     */
    public void swap2() {
        dup2X2();
        pop2();
    }

    // flow control instructions

    private void branch(int stackAdjust, Location location, byte opcode) {
        mInstructions.new BranchInstruction(stackAdjust, opcode, location);
    }

    /**
     * Generates code that performs an unconditional branch to the specified
     * location or label.
     *
     * @param location The location or label to branch to
     */
    public void branch(Location location) {
        branch(0, location, Opcode.GOTO);
    }

    /**
     * Generates code that performs a conditional branch based on the
     * value of an object on the stack. A branch is performed based on whether
     * the object reference on the stack is null or not.
     *
     * @param location The location or label to branch to
     * @param choice If true, do branch when null, else branch when not null
     */
    public void ifNullBranch(Location location, boolean choice) {
        branch(-1, location, choice ? Opcode.IFNULL : Opcode.IFNONNULL);
    }


    /**
     * Generates code that performs a conditional branch based on the value of
     * two object references on the stack. A branch is performed based on
     * whether the two objects are equal.
     *
     * @param location The location or label to branch to
     * @param choice If true, branch when equal, else branch when not equal
     */
    public void ifEqualBranch(Location location, boolean choice) {
        branch(-2, location, choice ? Opcode.IF_ACMPEQ : Opcode.IF_ACMPNE);
    }

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between an int value on the stack and zero. The int value on the
     * stack is on the left side of the comparison expression.
     *
     * @param location The location or label to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @exception IllegalArgumentException When the choice is not valid
     */
    public void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IFEQ;
        }
        else if (choice == "!=") {
            opcode = Opcode.IFNE;
        }
        else if (choice == "<") {
            opcode = Opcode.IFLT;
        }
        else if (choice == ">=") {
            opcode = Opcode.IFGE;
        }
        else if (choice == ">") {
            opcode = Opcode.IFGT;
        }
        else if (choice == "<=") {
            opcode = Opcode.IFLE;
        }
        else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }
        
        branch(-1, location, opcode);
    }

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between two int values on the stack. The first int value on the stack
     * is on the left side of the comparison expression.
     *
     * @param location The location or label to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @exception IllegalArgumentException When the choice is not valid
     */
    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {

        choice = choice.intern();

        byte opcode;
        if (choice ==  "==") {
            opcode = Opcode.IF_ICMPEQ;
        }
        else if (choice == "!=") {
            opcode = Opcode.IF_ICMPNE;
        }
        else if (choice == "<") {
            opcode = Opcode.IF_ICMPLT;
        }
        else if (choice == ">=") {
            opcode = Opcode.IF_ICMPGE;
        }
        else if (choice == ">") {
            opcode = Opcode.IF_ICMPGT;
        }
        else if (choice == "<=") {
            opcode = Opcode.IF_ICMPLE;
        }
        else {
            throw new IllegalArgumentException
                ("Invalid comparision choice: " + choice);
        }

        branch(-2, location, opcode);
    }

    /**
     * Generates code for a switch statement. The generated code is either a
     * lookupswitch or tableswitch. The choice of which switch type to generate
     * is made based on the amount of bytes to be generated. A tableswitch
     * is usually smaller, unless the cases are sparse.
     *
     * <p>The key value to switch on must already be on the stack when this
     * instruction executes.
     *
     * @param cases The values to match on. The array length must be the same
     * as for locations.
     * @param locations The locations or labels to branch to for each case.
     * The array length must be the same as for cases.
     * @param defaultLocation The location or label to branch to if the key on
     * the stack was not matched.
     */
    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {

        mInstructions.new SwitchInstruction(cases, locations, defaultLocation);
    }

    /**
     * Generates code that performs a subroutine branch to the specified 
     * location. The instruction generated is either jsr or jsr_w. It is most
     * often used for implementing a finally block.
     *
     * @param location The location or label to branch to
     */
    public void jsr(Location location) {
        // Adjust the stack by one to make room for the return address.
        branch(1, location, Opcode.JSR);
    }

    /**
     * Generates code that returns from a subroutine invoked by jsr.
     *
     * @param local The local variable reference that contains the return
     * address. The local variable must be of an object type.
     */
    public void ret(LocalVariable local) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        mInstructions.new RetInstruction(local);
    }

    // math instructions

    /**
     * Generates code for either a unary or binary math operation on one
     * or two values pushed on the stack.
     *
     * <p>Pass in an opcode from the the Opcode class. The only valid math
     * opcodes are:
     *
     * <pre>
     * IADD, ISUB, IMUL, IDIV, IREM, INEG, IAND, IOR, IXOR, ISHL, ISHR, IUSHR
     * LADD, LSUB, LMUL, LDIV, LREM, LNEG, LAND, LOR, LXOR, LSHL, LSHR, LUSHR
     * FADD, FSUB, FMUL, FDIV, FREM, FNEG
     * DADD, DSUB, DMUL, DDIV, DREM, DNEG
     *
     * LCMP
     * FCMPG, FCMPL
     * DCMPG, DCMPL
     * </pre>
     *
     * A not operation (~) is performed by doing a loadConstant with either
     * -1 or -1L followed by math(Opcode.IXOR) or math(Opcode.LXOR).
     *
     * @param opcode An opcode from the Opcode class.
     * @exception IllegalArgumentException When the opcode selected is not
     * a math operation.
     * @see Opcode
     */
    public void math(byte opcode) {
        int stackAdjust;
        
        switch(opcode) {
        case Opcode.INEG:
        case Opcode.LNEG:
        case Opcode.FNEG:
        case Opcode.DNEG:
            stackAdjust = 0;
            break;
        case Opcode.IADD:
        case Opcode.ISUB:
        case Opcode.IMUL:
        case Opcode.IDIV:
        case Opcode.IREM:
        case Opcode.IAND:
        case Opcode.IOR:
        case Opcode.IXOR:
        case Opcode.ISHL:
        case Opcode.ISHR:
        case Opcode.IUSHR:
        case Opcode.FADD:
        case Opcode.FSUB:
        case Opcode.FMUL:
        case Opcode.FDIV:
        case Opcode.FREM:
        case Opcode.FCMPG:
        case Opcode.FCMPL:
            stackAdjust = -1;
            break;
        case Opcode.LADD:
        case Opcode.LSUB:
        case Opcode.LMUL:
        case Opcode.LDIV:
        case Opcode.LREM:
        case Opcode.LAND:
        case Opcode.LOR:
        case Opcode.LXOR:
        case Opcode.LSHL:
        case Opcode.LSHR:
        case Opcode.LUSHR:
        case Opcode.DADD:
        case Opcode.DSUB:
        case Opcode.DMUL:
        case Opcode.DDIV:
        case Opcode.DREM:
            stackAdjust = -2;
            break;
        case Opcode.LCMP:
        case Opcode.DCMPG:
        case Opcode.DCMPL:
            stackAdjust = -3;
            break;
        default:
            throw new IllegalArgumentException
                ("Not a math opcode: " + Opcode.getMnemonic(opcode));
        }

        addCode(stackAdjust, opcode);
    }

    // miscellaneous instructions

    /**
     * Generates code for an arraylength instruction. The object to get the
     * length from must already be on the stack.
     */
    public void arrayLength() {
        addCode(0, Opcode.ARRAYLENGTH);
    }

    /**
     * Generates code that throws an exception. The object to throw must
     * already be on the stack.
     */
    public void throwObject() {
        addCode(-1, Opcode.ATHROW);
    }

    /**
     * Generates code that performs an object cast operation. The object
     * to check must already be on the stack.
     */
    public void checkCast(TypeDescriptor type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addCode(0, Opcode.CHECKCAST, info);
    }

    /**
     * Generates code that performs an instanceof operation. The object to
     * check must already be on the stack.
     */
    public void instanceOf(TypeDescriptor type) {
        ConstantInfo info = mCp.addConstantClass(type);
        addCode(0, Opcode.INSTANCEOF, info);
    }

    /**
     * Generates code that increments a local integer variable by a signed 
     * constant amount.
     */
    public void integerIncrement(LocalVariable local, int amount) {
        if (local == null) {
            throw new NullPointerException("No local variable specified");
        }

        if (-32768 <= amount && amount <= 32767) {
            mInstructions.new ShortIncrementInstruction(local, (short)amount);
        }
        else {
            // Amount can't possibly fit in a 16-bit value, so use regular
            // instructions instead.

            loadLocal(local);
            loadConstant(amount);
            math(Opcode.IADD);
            storeLocal(local);
        }
    }

    /**
     * Generates code to enter the monitor on an object loaded on the stack.
     */
    public void monitorEnter() {
        addCode(-1, Opcode.MONITORENTER);
    }

    /**
     * Generates code to exit the monitor on an object loaded on the stack.
     */
    public void monitorExit() {
        addCode(-1, Opcode.MONITOREXIT);
    }

    /**
     * Generates an instruction that does nothing. (No-OPeration)
     */
    public void nop() {
        addCode(0, Opcode.NOP);
    }

    /**
     * Generates a breakpoint instruction for use in a debugging environment.
     */
    public void breakpoint() {
        addCode(0, Opcode.BREAKPOINT);
    }
}
