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
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class FilteredCodeAssembler implements CodeAssembler {
    protected final CodeAssembler mAssembler;

    public FilteredCodeAssembler(CodeAssembler assembler) {
        mAssembler = assembler;
    }

    public LocalVariable[] getParameters() {
        return mAssembler.getParameters();
    }

    public LocalVariable createLocalVariable(String name, 
                                             TypeDescriptor type) {
        return mAssembler.createLocalVariable(name, type);
    }

    public Label createLabel() {
        return mAssembler.createLabel();
    }

    public void exceptionHandler(Location startLocation,
                                 Location endLocation,
                                 String catchClassName) {
        mAssembler.exceptionHandler
            (startLocation, endLocation, catchClassName);
    }
    
    public void mapLineNumber(int lineNumber) {
        mAssembler.mapLineNumber(lineNumber);
    }

    public void loadConstant(String value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(boolean value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(int value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(long value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(float value) {
        mAssembler.loadConstant(value);
    }

    public void loadConstant(double value) {
        mAssembler.loadConstant(value);
    }

    public void loadLocal(LocalVariable local) {
        mAssembler.loadLocal(local);
    }

    public void loadThis() {
        mAssembler.loadThis();
    }

    public void storeLocal(LocalVariable local) {
        mAssembler.storeLocal(local);
    }

    public void loadFromArray(Class type) {
        mAssembler.loadFromArray(type);
    }

    public void storeToArray(Class type) {
        mAssembler.storeToArray(type);
    }

    public void loadField(String fieldName,
                          TypeDescriptor type) {
        mAssembler.loadField(fieldName, type);
    }

    public void loadField(String className,
                          String fieldName,
                          TypeDescriptor type) {
        mAssembler.loadField(className, fieldName, type);
    }

    public void loadStaticField(String fieldName,
                                TypeDescriptor type) {
        mAssembler.loadStaticField(fieldName, type);
    }

    public void loadStaticField(String className,
                                String fieldName,
                                TypeDescriptor type) {
        mAssembler.loadStaticField(className, fieldName, type);
    }

    public void storeField(String fieldName,
                           TypeDescriptor type) {
        mAssembler.storeField(fieldName, type);
    }

    public void storeField(String className,
                           String fieldName,
                           TypeDescriptor type) {
        mAssembler.storeField(className, fieldName, type);
    }

    public void storeStaticField(String fieldName,
                                 TypeDescriptor type) {
        mAssembler.storeStaticField(fieldName, type);
    }

    public void storeStaticField(String className,
                                 String fieldName,
                                 TypeDescriptor type) {
        mAssembler.storeStaticField(className, fieldName, type);
    }

    public void returnVoid() {
        mAssembler.returnVoid();
    }

    public void returnValue(Class type) {
        mAssembler.returnValue(type);
    }

    public void convert(Class fromType, Class toType) {
        mAssembler.convert(fromType, toType);
    }

    public void invokeVirtual(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        mAssembler.invokeVirtual(methodName, ret, params);
    }

    public void invokeVirtual(String className,
                              String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        mAssembler.invokeVirtual(className, methodName, ret, params);
    }

    public void invokeStatic(String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        mAssembler.invokeStatic(methodName, ret, params);
    }

    public void invokeStatic(String className,
                             String methodName,
                             TypeDescriptor ret,
                             TypeDescriptor[] params) {
        mAssembler.invokeStatic(className, methodName, ret, params);
    }

    public void invokeInterface(String className,
                                String methodName,
                                TypeDescriptor ret,
                                TypeDescriptor[] params) {
        mAssembler.invokeInterface(className, methodName, ret, params);
    }

    public void invokePrivate(String methodName,
                              TypeDescriptor ret,
                              TypeDescriptor[] params) {
        mAssembler.invokePrivate(methodName, ret, params);
    }

    public void invokeSuper(String superClassName,
                            String methodName,
                            TypeDescriptor ret,
                            TypeDescriptor[] params) {
        mAssembler.invokeSuper(superClassName, methodName, ret, params);
    }

    public void invokeConstructor(TypeDescriptor[] params) {
        mAssembler.invokeConstructor(params);
    }

    public void invokeConstructor(String className, TypeDescriptor[] params) {
        mAssembler.invokeConstructor(className, params);
    }

    public void invokeSuperConstructor(TypeDescriptor[] params) {
        mAssembler.invokeSuperConstructor(params);
    }

    public void newObject(TypeDescriptor type) {
        mAssembler.newObject(type);
    }

    public void dup() {
        mAssembler.dup();
    }

    public void dupX1() {
        mAssembler.dupX1();
    }

    public void dupX2() {
        mAssembler.dupX2();
    }

    public void dup2() {
        mAssembler.dup2();
    }

    public void dup2X1() {
        mAssembler.dup2X1();
    }

    public void dup2X2() {
        mAssembler.dup2X2();
    }

    public void pop() {
        mAssembler.pop();
    }

    public void pop2() {
        mAssembler.pop2();
    }

    public void swap() {
        mAssembler.swap();
    }

    public void swap2() {
        mAssembler.swap2();
    }

    public void branch(Location location) {
        mAssembler.branch(location);
    }

    public void ifNullBranch(Location location, boolean choice) {
        mAssembler.ifNullBranch(location, choice);
    }

    public void ifEqualBranch(Location location, boolean choice) {
        mAssembler.ifEqualBranch(location, choice);
    }

    public void ifZeroComparisonBranch(Location location, String choice) 
        throws IllegalArgumentException {
        mAssembler.ifZeroComparisonBranch(location, choice);
    }

    public void ifComparisonBranch(Location location, String choice)
        throws IllegalArgumentException {
        mAssembler.ifComparisonBranch(location, choice);
    }

    public void switchBranch(int[] cases, 
                             Location[] locations, Location defaultLocation) {
        mAssembler.switchBranch(cases, locations, defaultLocation);
    }

    public void jsr(Location location) {
        mAssembler.jsr(location);
    }

    public void ret(LocalVariable local) {
        mAssembler.ret(local);
    }

    public void math(byte opcode) {
        mAssembler.math(opcode);
    }

    public void arrayLength() {
        mAssembler.arrayLength();
    }

    public void throwObject() {
        mAssembler.throwObject();
    }

    public void checkCast(TypeDescriptor type) {
        mAssembler.checkCast(type);
    }

    public void instanceOf(TypeDescriptor type) {
        mAssembler.instanceOf(type);
    }

    public void integerIncrement(LocalVariable local, int amount) {
        mAssembler.integerIncrement(local, amount);
    }

    public void monitorEnter() {
        mAssembler.monitorEnter();
    }

    public void monitorExit() {
        mAssembler.monitorExit();
    }

    public void nop() {
        mAssembler.nop();
    }

    public void breakpoint() {
        mAssembler.breakpoint();
    }
}
