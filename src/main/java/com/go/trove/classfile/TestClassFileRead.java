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
 * Reads a class file and prints out its contents. 
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/27 <!-- $-->
 */
public class TestClassFileRead {
    /**
     * @param args first argument is path to class file.
     */
    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream(args[0]);
        in = new BufferedInputStream(in);

        ClassFileDataLoader loader = new ResourceClassFileDataLoader();

        ClassFile cf = ClassFile.readFrom(in, loader, null);
        in.close();

        while (cf.getOuterClass() != null) {
            cf = cf.getOuterClass();
        }

        dump(cf);
    }

    private static void dump(ClassFile cf) {
        dump(cf, "");
    }

    private static void dump(ClassFile cf, String indent) {
        println(cf, indent);
        println("className: " + cf.getClassName(), indent);
        println("superClassName: " + cf.getSuperClassName(), indent);
        println("innerClass: " + cf.isInnerClass(), indent);
        println("innerClassName: " + cf.getInnerClassName(), indent);
        println("type: " + cf.getType(), indent);
        println("accessFlags: " + cf.getAccessFlags(), indent);

        String[] interfaces = cf.getInterfaces();
        print("interfaces: ", indent);
        for (int i=0; i<interfaces.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(interfaces[i]);
        }
        println();

        FieldInfo[] fields = cf.getFields();
        println("fields: ", indent);
        for (int i=0; i<fields.length; i++) {
            dump(fields[i], indent + "    ");
        }

        MethodInfo[] methods = cf.getMethods();
        println("methods: ", indent);
        for (int i=0; i<methods.length; i++) {
            dump(methods[i], indent + "    ");
        }

        methods = cf.getConstructors();
        println("constructors: ", indent);
        for (int i=0; i<methods.length; i++) {
            dump(methods[i], indent + "    ");
        }

        MethodInfo init = cf.getInitializer();
        println("initializer: ", indent);
        if (init != null) {
            dump(init, indent + "    ");
        }

        ClassFile[] innerClasses = cf.getInnerClasses();
        println("innerClasses: ", indent);
        for (int i=0; i<innerClasses.length; i++) {
            dump(innerClasses[i], indent + "    ");
        }

        println("sourceFile: " + cf.getSourceFile(), indent);
        println("synthetic: " + cf.isSynthetic(), indent);
        println("deprecated: " + cf.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(cf.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(FieldInfo field, String indent) {
        println(field, indent);
        println("name: " + field.getName(), indent);
        println("type: " + field.getType(), indent);
        println("accessFlags: " + field.getAccessFlags(), indent);
        println("constantValue: " + field.getConstantValue(), indent);
        println("synthetic: " + field.isSynthetic(), indent);
        println("deprecated: " + field.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(field.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(MethodInfo method, String indent) {
        println(method, indent);
        println("name: " + method.getName(), indent);
        println("methodDescriptor: " + method.getMethodDescriptor(), indent);
        println("accessFlags: " + method.getAccessFlags(), indent);

        String[] exceptions = method.getExceptions();
        print("exceptions: ", indent);
        for (int i=0; i<exceptions.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(exceptions[i]);
        }
        println();

        if (method.getCodeAttr() != null) {
            println("code:", indent);
            
            PrintWriter writer = new PrintWriter(System.out);
            
            TypeDescriptor[] paramTypes =
                method.getMethodDescriptor().getParameterTypes();
            boolean isStatic = method.getAccessFlags().isStatic();
            
            new CodeDisassembler(method).disassemble
                (new CodeAssemblerPrinter(paramTypes, isStatic,
                                          writer, indent + "    ", null));
            
            writer.flush();
        }

        println("synthetic: " + method.isSynthetic(), indent);
        println("deprecated: " + method.isDeprecated(), indent);
        println("attributes: ", indent);
        dump(method.getAttributes(), indent + "    ");

        println();
    }

    private static void dump(Attribute[] attributes, String indent) {
        if (attributes == null) {
            return;
        }
        for (int i=0; i<attributes.length; i++) {
            Attribute attribute = attributes[i];
            println(attribute, indent);
            Attribute[] subAttributes = attribute.getAttributes();
            if (subAttributes != null && subAttributes.length > 0) {
                println("attributes: ", indent);
                dump(subAttributes, indent + "    ");
            }
        }
    }

    private static void print(Object obj, String indent) {
        System.out.print(indent);
        System.out.print(obj);
    }

    private static void println(Object obj, String indent) {
        print(obj, indent);
        println();
    }

    private static void println() {
        System.out.println();
    }
}
