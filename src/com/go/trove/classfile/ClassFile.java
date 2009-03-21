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
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

/******************************************************************************
 * A class used to create Java class files. Call the writeTo method
 * to produce a class file.
 *
 * <p>See <i>The Java Virtual Machine Specification</i> (ISBN 0-201-63452-X)
 * for information on how class files are structured. Section 4.1 describes
 * the ClassFile structure.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/17 <!-- $-->
 */
public class ClassFile {
    private static final int MAGIC = 0xCAFEBABE;
    private static final int JDK1_1_MAJOR_VERSION = 45;
    private static final int JDK1_1_MINOR_VERSION = 3;
    
    private int mMajorVersion = JDK1_1_MAJOR_VERSION;
    private int mMinorVersion = JDK1_1_MINOR_VERSION;

    private String mClassName;
    private String mSuperClassName;
    private String mInnerClassName;
    private TypeDescriptor mType;

    private ConstantPool mCp;
    
    private AccessFlags mAccessFlags;

    private ConstantClassInfo mThisClass;
    private ConstantClassInfo mSuperClass;
    
    // Holds ConstantInfo objects.
    private List mInterfaces = new ArrayList(2);
    private Set mInterfaceSet = new HashSet(7);
    
    // Holds objects.
    private List mFields = new ArrayList();
    private List mMethods = new ArrayList();
    private List mAttributes = new ArrayList();
    
    private SourceFileAttr mSource;

    private List mInnerClasses;
    private int mAnonymousInnerClassCount = 0;
    private InnerClassesAttr mInnerClassesAttr;

    // Is non-null for inner classes.
    private ClassFile mOuterClass;

    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * This constructor creates a ClassFile for a class that extends
     * java.lang.Object.
     * <p>
     * Use the {@link #getAccessFlags access flags} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     */
    public ClassFile(String className) {
        this(className, (String)null);
    }
    
    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getAccessFlags access flags} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClass Super class.
     */
    public ClassFile(String className, Class superClass) {
        this(className, superClass.getName());
    }

    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getAccessFlags access flags} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClassName Full super class name.
     */
    public ClassFile(String className, String superClassName) {
        if (superClassName == null) {
            if (!className.equals(Object.class.getName())) {
                superClassName = Object.class.getName();
            }
        }

        mCp = new ConstantPool();

        // public, non-final, concrete class
        mAccessFlags = new AccessFlags(Modifier.PUBLIC);

        mThisClass = ConstantClassInfo.make(mCp, className);
        mSuperClass = ConstantClassInfo.make(mCp, superClassName);

        mClassName = className;
        mSuperClassName = superClassName;
    }

    /**
     * Used to construct a ClassFile when read from a stream.
     */
    private ClassFile(ConstantPool cp, AccessFlags accessFlags,
                      ConstantClassInfo thisClass,
                      ConstantClassInfo superClass,
                      ClassFile outerClass) {

        mCp = cp;

        mAccessFlags = accessFlags;

        mThisClass = thisClass;
        mSuperClass = superClass;

        mClassName = thisClass.getClassName();
        if (superClass != null) {
            mSuperClassName = superClass.getClassName();
        }

        mOuterClass = outerClass;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getSuperClassName() {
        return mSuperClassName;
    }

    /**
     * Returns a TypeDescriptor for the type of this ClassFile.
     */
    public TypeDescriptor getType() {
        if (mType == null) {
            mType = new TypeDescriptor(mClassName);
        }
        return mType;
    }

    public AccessFlags getAccessFlags() {
        return mAccessFlags;
    }

    /**
     * Returns the names of all the interfaces that this class implements.
     */
    public String[] getInterfaces() {
        int size = mInterfaces.size();
        String[] names = new String[size];

        for (int i=0; i<size; i++) {
            names[i] = ((ConstantClassInfo)mInterfaces.get(i)).getClassName();
        }

        return names;
    }

    /**
     * Returns all the fields defined in this class.
     */
    public FieldInfo[] getFields() {
        FieldInfo[] fields = new FieldInfo[mFields.size()];
        return (FieldInfo[])mFields.toArray(fields);
    }

    /**
     * Returns all the methods defined in this class, not including
     * constructors and static initializers.
     */
    public MethodInfo[] getMethods() {
        int size = mMethods.size();
        List methodsOnly = new ArrayList(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            String name = method.getName();
            if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
                methodsOnly.add(method);
            }
        }

        MethodInfo[] methodsArray = new MethodInfo[methodsOnly.size()];
        return (MethodInfo[])methodsOnly.toArray(methodsArray);
    }

    /**
     * Returns all the constructors defined in this class.
     */
    public MethodInfo[] getConstructors() {
        int size = mMethods.size();
        List ctorsOnly = new ArrayList(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            if ("<init>".equals(method.getName())) {
                ctorsOnly.add(method);
            }
        }

        MethodInfo[] ctorsArray = new MethodInfo[ctorsOnly.size()];
        return (MethodInfo[])ctorsOnly.toArray(ctorsArray);
    }

    /**
     * Returns the static initializer defined in this class or null if there
     * isn't one.
     */
    public MethodInfo getInitializer() {
        int size = mMethods.size();

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            if ("<clinit>".equals(method.getName())) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns all the inner classes defined in this class. If no inner classes
     * are defined, then an array of length zero is returned.
     */
    public ClassFile[] getInnerClasses() {
        if (mInnerClasses == null) {
            return new ClassFile[0];
        }

        ClassFile[] innerClasses = new ClassFile[mInnerClasses.size()];
        return (ClassFile[])mInnerClasses.toArray(innerClasses);
    }

    /**
     * Returns true if this ClassFile represents an inner class.
     */
    public boolean isInnerClass() {
        return mOuterClass != null;
    }

    /**
     * If this ClassFile represents a non-anonymous inner class, returns its
     * short inner class name.
     */
    public String getInnerClassName() {
        return mInnerClassName;
    }

    /**
     * Returns null if this ClassFile does not represent an inner class.
     *
     * @see #isInnerClass()
     */
    public ClassFile getOuterClass() {
        return mOuterClass;
    }

    /**
     * Returns a value indicating how deeply nested an inner class is with
     * respect to its outermost enclosing class. For top level classes, 0
     * is returned. For first level inner classes, 1 is returned, etc.
     */
    public int getClassDepth() {
        int depth = 0;

        ClassFile outer = mOuterClass;
        while (outer != null) {
            depth++;
            outer = outer.mOuterClass;
        }

        return depth;
    }

    /**
     * Returns the source file of this class file or null if not set.
     */
    public String getSourceFile() {
        if (mSource == null) {
            return null;
        }
        else {
            return mSource.getFileName();
        }
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides access to the ClassFile's ContantPool.
     *
     * @return The constant pool for this class file.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }
    
    /**
     * Add an interface that this class implements.
     *
     * @param interfaceName Full interface name.
     */
    public void addInterface(String interfaceName) {
        if (!mInterfaceSet.contains(interfaceName)) {
            mInterfaces.add(ConstantClassInfo.make(mCp, interfaceName));
            mInterfaceSet.add(interfaceName);
        }
    }
    
    /**
     * Add an interface that this class implements.
     */
    public void addInterface(Class i) {
        addInterface(i.getName());
    }
    
    /**
     * Add a field to this class.
     */
    public FieldInfo addField(AccessFlags flags,
                              String fieldName,
                              TypeDescriptor type) {
        FieldInfo fi = new FieldInfo(this, flags, fieldName, type);
        mFields.add(fi);
        return fi;
    }
    
    /**
     * Add a method to this class.
     *
     * @param ret Is null if method returns void.
     * @param params May be null if method accepts no parameters.
     */
    public MethodInfo addMethod(AccessFlags flags,
                                String methodName,
                                TypeDescriptor ret,
                                TypeDescriptor[] params) {
        MethodDescriptor md = new MethodDescriptor(ret, params);
        return addMethod(flags, methodName, md);
    }

    /**
     * Add a method to this class.
     */
    public MethodInfo addMethod(AccessFlags flags,
                                String methodName,
                                MethodDescriptor md) {
        MethodInfo mi = new MethodInfo(this, flags, methodName, md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add a method to this class. This method is handy for implementing
     * methods defined by a pre-existing interface.
     */
    public MethodInfo addMethod(Method method) {
        AccessFlags flags = new AccessFlags(method.getModifiers());
        flags.setAbstract(false);

        TypeDescriptor ret = new TypeDescriptor(method.getReturnType());

        Class[] paramClasses = method.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        MethodInfo mi = addMethod(flags, method.getName(), ret, params);
        
        // exception stuff...
        Class[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        return mi;
    }

    /**
     * Add a constructor to this class.
     *
     * @param params May be null if constructor accepts no parameters.
     */
    public MethodInfo addConstructor(AccessFlags flags,
                                     TypeDescriptor[] params) {
        MethodDescriptor md = new MethodDescriptor(null, params);
        MethodInfo mi = new MethodInfo(this, flags, "<init>", md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add a static initializer to this class.
     */
    public MethodInfo addInitializer() {
        MethodDescriptor md = new MethodDescriptor(null, null);
        AccessFlags af = new AccessFlags();
        af.setStatic(true);
        MethodInfo mi = new MethodInfo(this, af, "<clinit>", md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     */
    public ClassFile addInnerClass(String innerClassName) {
        return addInnerClass(innerClassName, (String)null);
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClass Super class.
     */
    public ClassFile addInnerClass(String innerClassName, Class superClass) {
        return addInnerClass(innerClassName, superClass.getName());
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClassName Full super class name.
     * @param isStatic True specifies a static inner class.
     */
    public ClassFile addInnerClass(String innerClassName, 
                                   String superClassName) {
        String fullInnerClassName;
        if (innerClassName == null) {
            fullInnerClassName = 
                mClassName + '$' + (++mAnonymousInnerClassCount);
        }
        else {
            fullInnerClassName = mClassName + '$' + innerClassName;
        }

        ClassFile inner = new ClassFile(fullInnerClassName, superClassName);
        AccessFlags access = inner.getAccessFlags();
        access.setPrivate(true);
        access.setStatic(true);
        inner.mInnerClassName = innerClassName;
        inner.mOuterClass = this;

        if (mInnerClasses == null) {
            mInnerClasses = new ArrayList();
        }

        mInnerClasses.add(inner);
        
        // Record the inner class in this, the outer class.
        if (mInnerClassesAttr == null) {
            addAttribute(new InnerClassesAttr(mCp));
        }

        mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName, 
                                        innerClassName, access);

        // Record the inner class in itself.
        inner.addAttribute(new InnerClassesAttr(inner.getConstantPool()));
        inner.mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName,
                                              innerClassName, access);

        return inner;
    }

    /**
     * Set the source file of this class file by adding a source file
     * attribute. The source doesn't actually have to be a file,
     * but the virtual machine spec names the attribute "SourceFile_attribute".
     */
    public void setSourceFile(String fileName) {
        addAttribute(new SourceFileAttr(mCp, fileName));
    }

    /**
     * Mark this class as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this class as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    /**
     * Add an attribute to this class.
     */
    public void addAttribute(Attribute attr) {
        if (attr instanceof SourceFileAttr) {
            if (mSource != null) {
                mAttributes.remove(mSource);
            }
            mSource = (SourceFileAttr)attr;
        }
        else if (attr instanceof InnerClassesAttr) {
            if (mInnerClassesAttr != null) {
                mAttributes.remove(mInnerClassesAttr);
            }
            mInnerClassesAttr = (InnerClassesAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        Attribute[] attrs = new Attribute[mAttributes.size()];
        return (Attribute[])mAttributes.toArray(attrs);
    }

    /**
     * Sets the version to use when writing the generated ClassFile. Currently,
     * only version 45, 3 is supported, and is set by default.
     *
     * @exception IllegalArgumentException when the version isn't supported
     */
    public void setVersion(int major, int minor) 
        throws IllegalArgumentException {

        if (major != JDK1_1_MAJOR_VERSION ||
            minor != JDK1_1_MINOR_VERSION) {

            throw new IllegalArgumentException("Version " + major + ", " +
                                               minor + " is not supported");
        }

        mMajorVersion = major;
        mMinorVersion = minor;
    }

    /**
     * Writes the ClassFile to the given OutputStream. When finished, the
     * stream is flushed, but not closed.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (!(out instanceof DataOutput)) {
            out = new DataOutputStream(out);
        }

        writeTo((DataOutput)out);
        
        out.flush();
    }

    /**
     * Writes the ClassFile to the given DataOutput.
     */
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeInt(MAGIC);
        dout.writeShort(mMinorVersion);
        dout.writeShort(mMajorVersion);
        
        mCp.writeTo(dout);
        
        int modifier = mAccessFlags.getModifier();
        dout.writeShort(modifier | Modifier.SYNCHRONIZED);

        dout.writeShort(mThisClass.getIndex());
        if (mSuperClass != null) {
            dout.writeShort(mSuperClass.getIndex());
        }
        else {
            dout.writeShort(0);
        }
        
        int size = mInterfaces.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Interfaces count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            int index = ((ConstantInfo)mInterfaces.get(i)).getIndex();
            dout.writeShort(index);
        }
        
        size = mFields.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Field count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            FieldInfo field = (FieldInfo)mFields.get(i);
            field.writeTo(dout);
        }
        
        size = mMethods.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Method count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            method.writeTo(dout);
        }
        
        size = mAttributes.size();
        if (size > 65535) {
            throw new RuntimeException
                ("Attribute count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = (Attribute)mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    /**
     * Reads a ClassFile from the given InputStream. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param in source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in) throws IOException {
        return readFrom(in, null, null);
    }

    /**
     * Reads a ClassFile from the given DataInput. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param din source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din) throws IOException {
        return readFrom(din, null, null);
    }

    /**
     * Reads a ClassFile from the given InputStream. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param in source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }
        return readFrom((DataInput)in, loader, attrFactory);
    }

    /**
     * Reads a ClassFile from the given DataInput. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param din source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        return readFrom(din, loader, attrFactory, new HashMap(11), null);
    }

    /**
     * @param loadedClassFiles Maps name to ClassFiles for classes already
     * loaded. This prevents infinite loop: inner loads outer loads inner...
     */
    private static ClassFile readFrom(DataInput din,
                                      ClassFileDataLoader loader,
                                      AttributeFactory attrFactory,
                                      Map loadedClassFiles,
                                      ClassFile outerClass)
        throws IOException
    {
        int magic = din.readInt();
        if (magic != MAGIC) {
            throw new IOException("Incorrect magic number: 0x" + 
                                  Integer.toHexString(magic));
        }

        int minor = din.readUnsignedShort();
        /*
        if (minor != JDK1_1_MINOR_VERSION) {
            throw new IOException("Minor version " + minor + 
                                  " not supported, version " + 
                                  JDK1_1_MINOR_VERSION + " is.");
        }
        */

        int major = din.readUnsignedShort();
        /*
        if (major != JDK1_1_MAJOR_VERSION) {
            throw new IOException("Major version " + major + 
                                  "not supported, version " + 
                                  JDK1_1_MAJOR_VERSION + " is.");
        }
        */

        ConstantPool cp = ConstantPool.readFrom(din);
        AccessFlags accessFlags = new AccessFlags(din.readUnsignedShort());
        accessFlags.setSynchronized(false);

        int index = din.readUnsignedShort();
        ConstantClassInfo thisClass = (ConstantClassInfo)cp.getConstant(index);

        index = din.readUnsignedShort();
        ConstantClassInfo superClass = null;
        if (index > 0) {
            superClass = (ConstantClassInfo)cp.getConstant(index);
        }

        ClassFile cf =
            new ClassFile(cp, accessFlags, thisClass, superClass, outerClass);
        loadedClassFiles.put(cf.getClassName(), cf);

        // Read interfaces.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            index = din.readUnsignedShort();
            ConstantClassInfo info = (ConstantClassInfo)cp.getConstant(index);
            cf.addInterface(info.getClassName());
        }
        
        // Read fields.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mFields.add(FieldInfo.readFrom(cf, din, attrFactory));
        }
        
        // Read methods.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mMethods.add(MethodInfo.readFrom(cf, din, attrFactory));
        }

        // Read attributes.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            Attribute attr = Attribute.readFrom(cp, din, attrFactory);
            cf.addAttribute(attr);
            if (attr instanceof InnerClassesAttr) {
                cf.mInnerClassesAttr = (InnerClassesAttr)attr;
            }
        }

        // Load inner and outer classes.
        if (cf.mInnerClassesAttr != null && loader != null) {
            InnerClassesAttr.Info[] infos =
                cf.mInnerClassesAttr.getInnerClassesInfo();
            for (int i=0; i<infos.length; i++) {
                InnerClassesAttr.Info info = infos[i];

                if (thisClass.equals(info.getInnerClass())) {
                    // This class is an inner class.
                    if (info.getInnerClassName() != null) {
                        cf.mInnerClassName = info.getInnerClassName();
                    }
                    ConstantClassInfo outer = info.getOuterClass();
                    if (cf.mOuterClass == null && outer != null) {
                        cf.mOuterClass = readOuterClass
                            (outer, loader, attrFactory, loadedClassFiles);
                    }
                    AccessFlags innerFlags = info.getAccessFlags();
                    accessFlags.setStatic(innerFlags.isStatic());
                    accessFlags.setPrivate(innerFlags.isPrivate());
                    accessFlags.setProtected(innerFlags.isProtected());
                    accessFlags.setPublic(innerFlags.isPublic());
                }
                else if (thisClass.equals(info.getOuterClass())) {
                    // This class is an outer class.
                    ConstantClassInfo inner = info.getInnerClass();
                    if (inner != null) {
                        ClassFile innerClass = readInnerClass
                            (inner, loader, attrFactory, loadedClassFiles, cf);
                        
                        if (innerClass != null) {
                            if (innerClass.getInnerClassName() == null) {
                                innerClass.mInnerClassName =
                                    info.getInnerClassName();
                            }
                            if (cf.mInnerClasses == null) {
                                cf.mInnerClasses = new ArrayList();
                            }
                            cf.mInnerClasses.add(innerClass);
                        }
                    }
                }
            }
        }

        return cf;
    }

    private static ClassFile readOuterClass(ConstantClassInfo outer,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map loadedClassFiles)
        throws IOException
    {
        String name = outer.getClassName();

        ClassFile outerClass = (ClassFile)loadedClassFiles.get(name);
        if (outerClass != null) {
            return outerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, null);
    }

    private static ClassFile readInnerClass(ConstantClassInfo inner,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map loadedClassFiles,
                                            ClassFile outerClass)
        throws IOException
    {
        String name = inner.getClassName();

        ClassFile innerClass = (ClassFile)loadedClassFiles.get(name);
        if (innerClass != null) {
            return innerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, outerClass);
    }
}
