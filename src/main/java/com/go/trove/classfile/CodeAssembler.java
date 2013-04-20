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
 * CodeAssembler is a high-level interface for assembling Java Virtual Machine
 * byte code. It can also be used as a visitor to a disassembler.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public interface CodeAssembler {
    /**
     * Returns LocalVariable references for all the parameters passed into
     * the method being assembled, not including any "this" reference.
     * Returns a zero-length array if there are no passed in parameters.
     *
     * <p>The names of the LocalVariables returned by this method are initially
     * set to null. It is encouraged that a name be provided.
     */
    public LocalVariable[] getParameters();

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
                                             TypeDescriptor type);

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
    public Label createLabel();

    /**
     * Sets up an exception handler located here, the location of the next
     * code to be generated.
     *
     * @param startLocation Location at the start of the section of
     * code to be wrapped by an exception handler.
     * @param endLocation Location directly after the end of the
     * section of code.
     * @param catchClassName The name of the type of exception to be caught; 
     * if null, then catch every object.
     */
    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName);
    
    /**
     * Map the location of the next code to be generated to a line number 
     * in source code. This enables line numbers in a stack trace from the 
     * generated code.
     */
    public void mapLineNumber(int lineNumber);

    // load-constant-to-stack style instructions

    /**
     * Generates code that loads a constant string value onto the stack.
     * If value is null, the generated code loads a null onto the stack.
     * Strings that exceed 65535 UTF encoded bytes in length are loaded by
     * creating a StringBuffer, appending substrings, and then converting to a
     * String.
     */
    public void loadConstant(String value);

    /**
     * Generates code that loads a constant boolean value onto the stack.
     */
    public void loadConstant(boolean value);

    /**
     * Generates code that loads a constant int, char, short or byte value 
     * onto the stack.
     */
    public void loadConstant(int value);

    /**
     * Generates code that loads a constant long value onto the stack.
     */
    public void loadConstant(long value);

    /**
     * Generates code that loads a constant float value onto the stack.
     */
    public void loadConstant(float value);

    /**
     * Generates code that loads a constant double value onto the stack.
     */
    public void loadConstant(double value);

    // load-local-to-stack style instructions

    /**
     * Generates code that loads a local variable onto the stack. Parameters
     * passed to a method and the "this" reference are all considered local
     * variables, as well as any that were created.
     *
     * @param local The local variable reference
     */
    public void loadLocal(LocalVariable local);

    /**
     * Loads a reference to "this" onto the stack. Static methods have no 
     * "this" reference, and an exception is thrown when attempting to
     * generate "this" in a static method.
     */
    public void loadThis();

    // store-from-stack-to-local style instructions

    /**
     * Generates code that pops a value off of the stack into a local variable.
     * Parameters passed to a method and the "this" reference are all 
     * considered local variables, as well as any that were created.
     *
     * @param local The local variable reference
     */
    public void storeLocal(LocalVariable local);

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
    public void loadFromArray(Class type);

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
    public void storeToArray(Class type);

    // load-field-to-stack style instructions

    /**
     * Generates code that loads a value from a field from this class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    public void loadField(String fieldName,
                          TypeDescriptor type);

    /**
     * Generates code that loads a value from a field from any class. 
     * An object reference must be on the stack. After the generated code 
     * has executed, the object reference is replaced by the value retrieved 
     * from the field.
     */
    public void loadField(String className,
                          String fieldName,
                          TypeDescriptor type);

    /**
     * Generates code that loads a value from a static field from this class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    public void loadStaticField(String fieldName,
                                TypeDescriptor type);

    /**
     * Generates code that loads a value from a static field from any class. 
     * After the generated code has executed, the value retrieved is placed
     * on the stack.
     */
    public void loadStaticField(String className,
                                String fieldName,
                                TypeDescriptor type);

    // store-to-field-from-stack style instructions

    /**
     * Generates code that stores a value into a field from this class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    public void storeField(String fieldName,
                           TypeDescriptor type);

    /**
     * Generates code that stores a value into a field from any class. 
     * An object reference and value must be on the stack. After the generated 
     * code has executed, the object reference and value are gone from
     * the stack.
     */
    public void storeField(String className,
                           String fieldName,
                           TypeDescriptor type);

    /**
     * Generates code that stores a value into a field from this class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    public void storeStaticField(String fieldName,
                                 TypeDescriptor type);

    /**
     * Generates code that stores a value into a field from any class. 
     * A value must be on the stack. After the generated 
     * code has executed, the value is gone from the stack.
     */
    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDescriptor type);

    // return style instructions

    /**
     * Generates code that returns void.
     */
    public void returnVoid();

    /**
     * Generates code that returns an object or primitive type. The value to
     * return must be on the stack.
     *
     * <p>The type doesn't need to be an exact match for objects. Object.class
     * works fine for all objects. For primitive types, use the class that
     * matches that type. For an int the type is int.class.
     */
    public void returnValue(Class type);

    // numerical conversion style instructions

    /**
     * Generates code that converts the value of a primitive type already
     * on the stack.
     */
    public void convert(Class fromType, Class toType);

    // invocation style instructions

    /**
     * Generates code to invoke a virtual method in this class. The object
     * reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeVirtual(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params);

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
                              TypeDescriptor[] params);

    /**
     * Generates code to invoke a static method in this class. The method's
     * argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokeStatic(String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params);

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
                             TypeDescriptor[] params);

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
                                TypeDescriptor[] params);

    /**
     * Generates code to invoke a private method in this class.
     * The object reference and the method's argument(s) must be on the stack.
     *
     * @param ret May be null if method returns void.
     * @param params May be null if method takes no parameters.
     */
    public void invokePrivate(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params);

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
                            TypeDescriptor[] params);

    /**
     * Generates code to invoke a class constructor in this class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeConstructor(TypeDescriptor[] params);

    /**
     * Generates code to invoke a class constructor in any class. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeConstructor(String className, TypeDescriptor[] params);

    /**
     * Generates code to invoke a super class constructor. The object 
     * reference and the constructor's argument(s) must be on the stack.
     *
     * @param params May be null if constructor takes no parameters.
     */
    public void invokeSuperConstructor(TypeDescriptor[] params);

    // creation style instructions

    /**
     * Generates code to create a new object. Unless the new object is an 
     * array, it is invalid until a constructor method is invoked on it.
     * When creating arrays, the size for each dimension must be on the
     * operand stack.
     *
     * @see #invokeConstructor
     */
    public void newObject(TypeDescriptor type);

    // stack operation style instructions

    /**
     * Generates code for the dup instruction.
     */
    public void dup();

    /**
     * Generates code for the dup_x1 instruction.
     */
    public void dupX1();

    /**
     * Generates code for the dup_x2 instruction.
     */
    public void dupX2();

    /**
     * Generates code for the dup2 instruction.
     */
    public void dup2();

    /**
     * Generates code for the dup2_x1 instruction.
     */
    public void dup2X1();

    /**
     * Generates code for the dup2_x2 instruction.
     */
    public void dup2X2();

    /**
     * Generates code for the pop instruction.
     */
    public void pop();

    /**
     * Generates code for the pop2 instruction.
     */
    public void pop2();

    /**
     * Generates code for the swap instruction.
     */
    public void swap();

    /**
     * Generates code for a swap2 instruction.
     */
    public void swap2();

    // flow control instructions

    /**
     * Generates code that performs an unconditional branch to the specified
     * location.
     *
     * @param location The location to branch to
     */
    public void branch(Location location);

    /**
     * Generates code that performs a conditional branch based on the
     * value of an object on the stack. A branch is performed based on whether
     * the object reference on the stack is null or not.
     *
     * @param location The location to branch to
     * @param choice If true, do branch when null, else branch when not null
     */
    public void ifNullBranch(Location location, boolean choice);

    /**
     * Generates code that performs a conditional branch based on the value of
     * two object references on the stack. A branch is performed based on
     * whether the two objects are equal.
     *
     * @param location The location to branch to
     * @param choice If true, branch when equal, else branch when not equal
     */
    public void ifEqualBranch(Location location, boolean choice);

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between an int value on the stack and zero. The int value on the
     * stack is on the left side of the comparison expression.
     *
     * @param location The location to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @exception IllegalArgumentException When the choice is not valid
     */
    public void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException;

    /**
     * Generates code the performs a conditional branch based on a comparison
     * between two int values on the stack. The first int value on the stack
     * is on the left side of the comparison expression.
     *
     * @param location The location to branch to
     * @param choice One of "==", "!=", "<", ">=", ">" or "<="
     * @exception IllegalArgumentException When the choice is not valid
     */
    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException;

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
     * @param locations The locations to branch to for each case.
     * The array length must be the same as for cases.
     * @param defaultLocation The location to branch to if the key on
     * the stack was not matched.
     */
    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation);

    /**
     * Generates code that performs a subroutine branch to the specified 
     * location. The instruction generated is either jsr or jsr_w. It is most
     * often used for implementing a finally block.
     *
     * @param Location The location to branch to
     */
    public void jsr(Location location);

    /**
     * Generates code that returns from a subroutine invoked by jsr.
     *
     * @param local The local variable reference that contains the return
     * address. The local variable must be of an object type.
     */
    public void ret(LocalVariable local);

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
    public void math(byte opcode);

    // miscellaneous instructions

    /**
     * Generates code for an arraylength instruction. The object to get the
     * length from must already be on the stack.
     */
    public void arrayLength();

    /**
     * Generates code that throws an exception. The object to throw must
     * already be on the stack.
     */
    public void throwObject();

    /**
     * Generates code that performs an object cast operation. The object
     * to check must already be on the stack.
     */
    public void checkCast(TypeDescriptor type);

    /**
     * Generates code that performs an instanceof operation. The object to
     * check must already be on the stack.
     */
    public void instanceOf(TypeDescriptor type);

    /**
     * Generates code that increments a local integer variable by a signed 
     * constant amount.
     */
    public void integerIncrement(LocalVariable local, int amount);

    /**
     * Generates code to enter the monitor on an object loaded on the stack.
     */
    public void monitorEnter();

    /**
     * Generates code to exit the monitor on an object loaded on the stack.
     */
    public void monitorExit();

    /**
     * Generates an instruction that does nothing. (No-OPeration)
     */
    public void nop();

    /**
     * Generates a breakpoint instruction for use in a debugging environment.
     */
    public void breakpoint();
}
