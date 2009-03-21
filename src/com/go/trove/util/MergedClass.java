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

package com.go.trove.util;

import java.lang.reflect.*;
import java.util.*;
import java.io.OutputStream;
import java.io.IOException;
import com.go.trove.classfile.*;

/******************************************************************************
 * Merges several classes together, producing a new class that has all of the
 * methods of the combined classes. All methods in the combined class delegate
 * to instances of the source classes. If multiple classes implement the same
 * method, the first one provided is used. The merged class implements all of
 * the interfaces provided by the source classes or interfaces.
 *
 * <p>This class performs a function almost the same as the Proxy class
 * introduced in JDK1.3. It differs in that it supports classes as well as
 * interfaces as input, and it binds to wrapped objects without using
 * runtime reflection. It is less flexible than Proxy in that there isn't way
 * to customize the method delegation, and so it isn't suitable for creating
 * dynamically generated interface implementations.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/02/20 <!-- $-->
 */
public class MergedClass {
    // Maps ClassInjectors to Maps that map to merged class names.
    // Map<ClassInjector, Map<MultiKey, String>>
    // The MultiKey is composed of class names and method prefixes. By storing
    // class names into the maps instead of classes, the classes may be
    // reclaimed by the garbage collector.
    private static Map cMergedMap;

    static {
        try {
            cMergedMap = new IdentityMap(7);
        }
        catch (LinkageError e) {
            cMergedMap = new HashMap(7);
        }
        catch (Exception e) {
            // Microsoft VM sometimes throws an undeclared
            // ClassNotFoundException instead of doing the right thing and
            // throwing some form of a LinkageError if the class couldn't
            // be found.
            cMergedMap = new HashMap(7);
        }
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor's parameter types match the source classes.
     * An IllegalArgumentException is thrown if any of the given conditions
     * are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types
     * <li>The given classes must be loadable from the given injector
     * <li>At most 254 classes may be merged
     * </ul>
     *
     * Note: Because a constructor is limited to 254 parameters, if more than
     * 254 classes are to be merged together, call {@link #getConstructor2}.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     */
    public static Constructor getConstructor(ClassInjector injector,
                                             Class[] classes)
        throws IllegalArgumentException
    {
        return getConstructor(injector, classes, null);
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor's parameter types match the source classes.
     * An IllegalArgumentException is thrown if any of the given conditions
     * are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a 
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * <li>At most 254 classes may be merged
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     * <p>
     * Note: Because a constructor is limited to 254 parameters, if more than
     * 254 classes are to be merged together, call {@link #getConstructor2}.
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     */
    public static Constructor getConstructor(ClassInjector injector,
                                             Class[] classes,
                                             String[] prefixes)
        throws IllegalArgumentException
    {
        if (classes.length > 254) {
            throw new IllegalArgumentException
                ("More than 254 merged classes: " + classes.length);
        }

        Class clazz = getMergedClass(injector, classes, prefixes);

        try {
            return clazz.getConstructor(classes);
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor accepts one parameter type: an
     * {@link InstanceFactory}. Merged instances are requested only when
     * first needed. An IllegalArgumentException is thrown if any of the given
     * conditions are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types
     * <li>The given classes must be loadable from the given injector
     * </ul>
     *
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     */
    public static Constructor getConstructor2(ClassInjector injector,
                                              Class[] classes)
        throws IllegalArgumentException
    {
        return getConstructor2(injector, classes, null);
    }

    /**
     * Returns the constructor for a class that merges all of the given source
     * classes. The constructor accepts one parameter type: an
     * {@link InstanceFactory}. Merged instances are requested only when
     * first needed. An IllegalArgumentException is thrown if any of the given
     * conditions are not satisfied:
     *
     * <ul>
     * <li>None of the given classes can represent a primitive type
     * <li>A source class can only be provided once, unless paired with a 
     * unique prefix.
     * <li>Any non-public classes must be in the same common package
     * <li>Duplicate methods cannot have conflicting return types, unless a
     * prefix is provided.
     * <li>The given classes must be loadable from the given injector
     * </ul>
     *
     * To help resolve ambiguities, a method prefix can be specified for each
     * passed in class. For each prefixed method, the non-prefixed method is
     * also generated, unless that method conflicts with the return type of
     * another method. If any passed in classes are or have interfaces, then
     * those interfaces will be implemented only if there are no conflicts.
     *
     * <p>The array of prefixes may be null, have null elements or
     * be shorter than the array of classes. A prefix of "" is treated as null.
     * 
     * @param injector ClassInjector that will receive class definition
     * @param classes Source classes used to derive merged class
     * @param prefixes Optional prefixes to apply to methods of each generated
     * class to eliminate duplicate method names
     */
    public static Constructor getConstructor2(ClassInjector injector,
                                              Class[] classes,
                                              String[] prefixes)
        throws IllegalArgumentException
    {
        Class clazz = getMergedClass(injector, classes, prefixes);

        try {
            return clazz.getConstructor(new Class[]{InstanceFactory.class});
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }
    }

    private static Class getMergedClass(ClassInjector injector,
                                        Class[] classes,
                                        String[] prefixes)
        throws IllegalArgumentException
    {
        ClassEntry[] classEntries = new ClassEntry[classes.length];
        for (int i=0; i<classes.length; i++) {
            // Load the classes from the ClassInjector, just like they will be
            // when the generated class is resolved.
            try {
                classes[i] = injector.loadClass(classes[i].getName());
            }
            catch (ClassNotFoundException e) {
                throw new IllegalArgumentException
                    ("Unable to load class from injector: " + classes[i]);
            }

            if (prefixes == null || i >= prefixes.length) {
                classEntries[i] = new ClassEntry(classes[i]);
            }
            else {
                String prefix = prefixes[i];
                if (prefix != null && prefix.length() == 0) {
                    prefix = null;
                }
                classEntries[i] = new ClassEntry(classes[i], prefix);
            }
        }

        return getMergedClass(injector, classEntries);
    }

    /**
     * Creates a class with a public constructor whose parameter types match
     * the given source classes. Another constructor is also created that
     * accepts an InstanceFactory.
     */
    private static synchronized Class getMergedClass(ClassInjector injector,
                                                     ClassEntry[] classEntries)
        throws IllegalArgumentException
    {
        Map classListMap = (Map)cMergedMap.get(injector);
        if (classListMap == null) {
            classListMap = new HashMap(7);
            cMergedMap.put(injector, classListMap);
        }

        Object key = generateKey(classEntries);
        String mergedName = (String)classListMap.get(key);
        if (mergedName != null) {
            try {
                return injector.loadClass(mergedName);
            }
            catch (ClassNotFoundException e) {
            }
        }

        ClassFile cf;
        try {
            cf = createClassFile(injector, classEntries);
        }
        catch (IllegalArgumentException e) {
            e.fillInStackTrace();
            throw e;
        }

        /*
        try {
            java.io.FileOutputStream out =
                new java.io.FileOutputStream(cf.getClassName() + ".class");
            cf.writeTo(out);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        */

        try {
            OutputStream stream = injector.getStream(cf.getClassName());
            cf.writeTo(stream);
            stream.close();
        }
        catch (IOException e) {
            throw new InternalError(e.toString());
        }

        Class merged;
        try {
            merged = injector.loadClass(cf.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new InternalError(e.toString());
        }

        classListMap.put(key, merged.getName());
        return merged;
    }

    private static Object generateKey(ClassEntry[] classEntries) {
        int length = classEntries.length;
        Object[] mainElements = new Object[length];
        for (int i=0; i<length; i++) {
            ClassEntry classEntry = classEntries[i];
            mainElements[i] = new MultiKey(new Object[] {
                classEntry.getClazz().getName(),
                classEntry.getMethodPrefix()
            });
        }
        return new MultiKey(mainElements);
    }

    private static ClassFile createClassFile(ClassInjector injector,
                                             ClassEntry[] classEntries)
        throws IllegalArgumentException
    {
        Set classSet = new HashSet(classEntries.length * 2 + 1);
        Set nonConflictingClasses = new HashSet(classEntries.length * 2 + 1);
        String commonPackage = null;
        Map methodMap = new HashMap();

        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            Class clazz = classEntry.getClazz();
            
            if (clazz.isPrimitive()) {
                throw new IllegalArgumentException
                    ("Merged classes cannot be primitive: " + clazz);
            }

            if (!classSet.add(classEntry)) {
                throw new IllegalArgumentException
                    ("Class is specified more than once: " + clazz);
            }

            if (!Modifier.isPublic(clazz.getModifiers())) {
                String classPackage = clazz.getName();
                int index = classPackage.lastIndexOf('.');
                if (index < 0) {
                    classPackage = "";
                }
                else {
                    classPackage = classPackage.substring(0, index);
                }

                if (commonPackage == null) {
                    commonPackage = classPackage;
                }
                else if (!commonPackage.equals(classPackage)) {
                    throw new IllegalArgumentException
                        ("Not all non-public classes defined in same " +
                         "package: " + commonPackage);
                }
            }

            // Innocent until proven guilty.
            nonConflictingClasses.add(classEntry);

            Method[] methods = clazz.getMethods();
            String prefix = classEntry.getMethodPrefix();

            for (int j=0; j<methods.length; j++) {
                Method method = methods[j];
                String name = method.getName();
                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(name)) {
                    continue;
                }

                MethodEntry methodEntry = new MethodEntry(method, name);
                MethodEntry existing = (MethodEntry)methodMap.get(methodEntry);

                if (existing == null) {
                    methodMap.put(methodEntry, methodEntry);
                }
                else if (existing.returnTypeDiffers(methodEntry)) {
                    nonConflictingClasses.remove(classEntry);
                    if (prefix == null) {
                        throw new IllegalArgumentException
                            ("Conflicting return types: " +
                             existing + ", " + methodEntry);
                    }
                }

                if (prefix != null) {
                    name = prefix + name;
                
                    methodEntry = new MethodEntry(method, name);
                    existing = (MethodEntry)methodMap.get(methodEntry);

                    if (existing == null) {
                        methodMap.put(methodEntry, methodEntry);
                    }
                    else if (existing.returnTypeDiffers(methodEntry)) {
                        nonConflictingClasses.remove(classEntry);
                        throw new IllegalArgumentException
                            ("Conflicting return types: " +
                             existing + ", " + methodEntry);
                    }
                }
            }
        }

        int id = 0;
        Iterator it = classSet.iterator();
        while (it.hasNext()) {
            id = id * 31 + it.next().hashCode();
        }

        String className = "MergedClass$";
        try {
            while (true) {
                className = "MergedClass$" + (id & 0xffffffffL);
                if (commonPackage != null && commonPackage.length() > 0) {
                    className = commonPackage + '.' + className;
                }
                try {
                    injector.loadClass(className);
                }
                catch (LinkageError e) {
                }
                id++;
            }
        }
        catch (ClassNotFoundException e) {
        }

        ClassFile cf = new ClassFile(className);
        cf.getAccessFlags().setFinal(true);
        cf.markSynthetic();

        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            if (nonConflictingClasses.contains(classEntry)) {
                addAllInterfaces(cf, classEntry.getClazz());
            }
        }

        AccessFlags privateAccess = new AccessFlags();
        privateAccess.setPrivate(true);
        AccessFlags privateFinalAccess = new AccessFlags();
        privateFinalAccess.setPrivate(true);
        privateFinalAccess.setFinal(true);
        AccessFlags publicAccess = new AccessFlags();
        publicAccess.setPublic(true);

        // Add field to store optional InstanceFactory.
        TypeDescriptor instanceFactoryType =
            new TypeDescriptor(InstanceFactory.class);
        cf.addField(privateAccess,
                    "instanceFactory", instanceFactoryType).markSynthetic();

        Method instanceFactoryMethod;
        try {
            instanceFactoryMethod =
                InstanceFactory.class.getMethod
                ("getInstance", new Class[]{int.class});
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }

        // Define fields which point to wrapped objects, and define methods
        // to access the fields.
        String[] fieldNames = new String[classEntries.length];
        TypeDescriptor[] types = new TypeDescriptor[classEntries.length];
        for (int i=0; i<classEntries.length; i++) {
            Class clazz = classEntries[i].getClazz();
            String fieldName = "m$" + i;
            TypeDescriptor type = new TypeDescriptor(clazz);
            cf.addField(privateAccess, fieldName, type).markSynthetic();
            fieldNames[i] = fieldName;
            types[i] = type;
            
            // Create method that returns field, calling the InstanceFactory
            // if necessary to initialize for the first time.
            MethodInfo mi = cf.addMethod
                (privateFinalAccess, fieldName, type, null);
            mi.markSynthetic();
            CodeBuilder builder = new CodeBuilder(mi);
            builder.loadThis();
            builder.loadField(fieldName, type);
            builder.dup();
            Label isNull = builder.createLabel();
            builder.ifNullBranch(isNull, true);
            // Return the initialized field.
            builder.returnValue(Object.class);
            isNull.setLocation();
            // Discard null field value.
            builder.pop();
            builder.loadThis();
            builder.loadField("instanceFactory", instanceFactoryType);
            builder.dup();
            Label haveInstanceFactory = builder.createLabel();
            builder.ifNullBranch(haveInstanceFactory, false);
            // No instanceFactory: return null.
            builder.loadConstant(null);
            builder.returnValue(Object.class);
            haveInstanceFactory.setLocation();
            builder.loadConstant(i);
            builder.invoke(instanceFactoryMethod);
            builder.checkCast(type);
            builder.dup();
            builder.loadThis();
            builder.swap();
            builder.storeField(fieldName, type);
            builder.returnValue(Object.class);
        }

        // Define a constructor that initializes fields from an Object array.
        if (classEntries.length <= 254) {
            MethodInfo mi = cf.addConstructor(publicAccess, types);
            
            CodeBuilder builder = new CodeBuilder(mi);
            builder.loadThis();
            builder.invokeSuperConstructor(null);
            LocalVariable[] params = builder.getParameters();
            
            for (int i=0; i<classEntries.length; i++) {
                builder.loadThis();
                builder.loadLocal(params[i]);
                builder.storeField(fieldNames[i], types[i]);
            }
            
            builder.returnVoid();
            builder = null;
        }

        // Define a constructor that saves an InstanceFactory.
        MethodInfo mi = cf.addConstructor
            (publicAccess, new TypeDescriptor[]{instanceFactoryType});

        CodeBuilder builder = new CodeBuilder(mi);
        builder.loadThis();
        builder.invokeSuperConstructor(null);
        builder.loadThis();
        builder.loadLocal(builder.getParameters()[0]);
        builder.storeField("instanceFactory", instanceFactoryType);

        builder.returnVoid();
        builder = null;

        Set methodSet = methodMap.keySet();

        // Define all the wrapper methods.
        for (int i=0; i<classEntries.length; i++) {
            ClassEntry classEntry = classEntries[i];
            String prefix = classEntry.getMethodPrefix();
            String fieldName = fieldNames[i];
            TypeDescriptor type = types[i];

            Method[] methods = classEntry.getClazz().getMethods();
            for (int j=0; j<methods.length; j++) {
                Method method = methods[j];
                // Workaround for JDK1.2 bug #4187388.
                if ("<clinit>".equals(method.getName())) {
                    continue;
                }

                MethodEntry methodEntry = new MethodEntry(method);
                if (methodSet.contains(methodEntry)) {
                    methodSet.remove(methodEntry);
                    addWrapperMethod(cf, methodEntry, fieldName, type);
                }

                if (prefix != null) {
                    methodEntry = new MethodEntry
                        (method, prefix + method.getName());
                    if (methodSet.contains(methodEntry)) {
                        methodSet.remove(methodEntry);
                        addWrapperMethod(cf, methodEntry, fieldName, type);
                    }
                }
            }
        }

        return cf;
    }

    private static void addAllInterfaces(ClassFile cf, Class clazz) {
        if (clazz == null) {
            return;
        }

        if (clazz.isInterface()) {
            cf.addInterface(clazz);
        }

        addAllInterfaces(cf, clazz.getSuperclass());

        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            addAllInterfaces(cf, interfaces[i]);
        }
    }

    private static void addWrapperMethod(ClassFile cf,
                                         MethodEntry methodEntry,
                                         String fieldName,
                                         TypeDescriptor type) {
        Method method = methodEntry.getMethod();

        // Don't override any methods in Object, especially final ones.
        if (isDefinedInObject(method)) {
            return;
        }

        AccessFlags flags = new AccessFlags(method.getModifiers());
        flags.setAbstract(false);
        flags.setFinal(true);
        flags.setSynchronized(false);
        flags.setNative(false);
        flags.setStatic(false);

        AccessFlags staticFlags = (AccessFlags)flags.clone();
        staticFlags.setStatic(true);

        TypeDescriptor ret = new TypeDescriptor(method.getReturnType());

        Class[] paramClasses = method.getParameterTypes();
        TypeDescriptor[] params = new TypeDescriptor[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = new TypeDescriptor(paramClasses[i]);
        }

        MethodInfo mi;
        if (Modifier.isStatic(method.getModifiers())) {
            mi = cf.addMethod
                (staticFlags, methodEntry.getName(), ret, params);
        }
        else {
            mi = cf.addMethod(flags, methodEntry.getName(), ret, params);
        }
        
        // Exception stuff...
        Class[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        // Delegate to wrapped object.
        CodeBuilder builder = new CodeBuilder(mi);

        if (!Modifier.isStatic(method.getModifiers())) {
            builder.loadThis();
            builder.loadField(fieldName, type);
            Label isNonNull = builder.createLabel();
            builder.dup();
            builder.ifNullBranch(isNonNull, false);
            // Discard null field value.
            builder.pop();
            // Call the field access method, which in turn calls the
            // InstanceFactory.
            builder.loadThis();
            builder.invokePrivate(fieldName, type, null);
            isNonNull.setLocation();
        }
        LocalVariable[] locals = builder.getParameters();
        for (int i=0; i<locals.length; i++) {
            builder.loadLocal(locals[i]);
        }
        builder.invoke(method);

        if (method.getReturnType() == void.class) {
            builder.returnVoid();
        }
        else {
            builder.returnValue(method.getReturnType());
        }
    }

    private static boolean isDefinedInObject(Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return true;
        }

        Class[] types = method.getParameterTypes();
        String name = method.getName();
        
        if (types.length == 0) {
            return 
                "hashCode".equals(name) ||
                "clone".equals(name) ||
                "toString".equals(name) ||
                "finalize".equals(name);
        }
        else {
            return
                types.length == 1 &&
                types[0] == Object.class && 
                "equals".equals(name);
        }
    }

    private MergedClass() {
    }

    /**
     * InstanceFactory allows merged class instances to be requested only
     * when first needed.
     */
    public interface InstanceFactory {
        /**
         * Return a merged class instance by index. This index corresponds to
         * the class index used when defining the MergedClass.
         */
        public Object getInstance(int index);
    }

    private static class ClassEntry {
        private final Class mClazz;
        private final String mPrefix;

        public ClassEntry(Class clazz) {
            this(clazz, null);
        }
        
        public ClassEntry(Class clazz, String prefix) {
            mClazz = clazz;
            mPrefix = prefix;
        }

        public Class getClazz() {
            return mClazz;
        }

        public String getMethodPrefix() {
            return mPrefix;
        }

        public int hashCode() {
            int hash = mClazz.getName().hashCode();
            return (mPrefix == null) ? hash : hash ^ mPrefix.hashCode();
        }

        public boolean equals(Object other) {
            if (other instanceof ClassEntry) {
                ClassEntry classEntry = (ClassEntry)other;
                if (mClazz == classEntry.mClazz) {
                    if (mPrefix == null) {
                        return classEntry.mPrefix == null;
                    }
                    else {
                        return mPrefix.equals(classEntry.mPrefix);
                    }
                }
            }
            return false;
        }

        public String toString() {
            return mClazz.toString();
        }
    }

    private static class MethodEntry {
        private final Method mMethod;
        private final String mName;
        private List mParams;
        private int mHashCode;

        public MethodEntry(Method method) {
            this(method, method.getName());
        }

        public MethodEntry(Method method, String name) {
            mMethod = method;
            mName = name;
            mParams = Arrays.asList(method.getParameterTypes());
            mHashCode = mName.hashCode() ^ mParams.hashCode();
        }

        public Method getMethod() {
            return mMethod;
        }

        public String getName() {
            return mName;
        }

        public boolean returnTypeDiffers(MethodEntry methodEntry) {
            return getMethod().getReturnType() !=
                methodEntry.getMethod().getReturnType();
        }

        public int hashCode() {
            return mHashCode;
        }

        public boolean equals(Object other) {
            if (!(other instanceof MethodEntry)) {
                return false;
            }
            MethodEntry methodEntry = (MethodEntry)other;
            return mName.equals(methodEntry.mName) &&
                mParams.equals(methodEntry.mParams);
        }

        public String toString() {
            return mMethod.toString();
        }
    }
}
