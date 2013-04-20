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

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/******************************************************************************
 * A special ClassLoader that allows classes to be defined by directly
 * injecting the bytecode. All classes other than those injected are loaded
 * from the parent ClassLoader, which is the ClassLoader that loaded this
 * class. If a directory is passed in, the ClassInjector looks there for 
 * non-injected class files before asking the parent ClassLoader for a class.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  7/30/01 <!-- $-->
 */
public class ClassInjector extends ClassLoader {
    private static Map cShared = new NullKeyMap(new IdentityMap());

    /**
     * Returns a shared ClassInjector instance.
     */
    public static ClassInjector getInstance() {
        return getInstance(null);
    }

    /**
     * Returns a shared ClassInjector instance for the given ClassLoader.
     */
    public static ClassInjector getInstance(ClassLoader loader) {
        ClassInjector injector = null;

        synchronized (cShared) {
            Reference ref = (Reference)cShared.get(loader);
            if (ref != null) {
                injector = (ClassInjector)ref.get();
            }
            if (injector == null) {
                injector = new ClassInjector(loader);
                cShared.put(loader, new WeakReference(injector));
            }
            return injector;
        }
    }

    // Parent ClassLoader, used to load classes that aren't defined by this.
    private ClassLoader mSuperLoader;
    
    private File[] mRootClassDirs;
    private String mRootPackage;

    // A set of all the classes defined by the ClassInjector.
    private Map mDefined = Collections.synchronizedMap(new HashMap());
    
    // A map to store raw bytecode for future use in getResourceAsStream().
    private Map mGZippedBytecode;

    private URLStreamHandler mFaker;

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent, and it has no root class directory or root package.
     */
    public ClassInjector() {
        this(null, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that has no root class directory or root
     * package.
     *
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     */
    public ClassInjector(ClassLoader parent) {
        this(parent, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent.
     *
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(File rootClassDir, String rootPackage) {
        this(null, (rootClassDir == null) ? null : new File[]{rootClassDir},
             rootPackage);
    }
    
    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(ClassLoader parent, 
                         File rootClassDir, String rootPackage) {
        this(parent, (rootClassDir == null) ? null : new File[]{rootClassDir},
             rootPackage);
    }
    
    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent.
     *
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(File[] rootClassDirs, String rootPackage) {
        this(null, rootClassDirs, rootPackage);
    }
    
    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(ClassLoader parent, 
                         File[] rootClassDirs, 
                         String rootPackage) {
        this(parent, rootClassDirs, rootPackage, false);
    }

    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     * @param keepRawBytecode if true, will cause the ClassInjector to store
     * the raw bytecode of defined classes.
     */
    public ClassInjector(ClassLoader parent, 
                         File[] rootClassDirs, 
                         String rootPackage,
                         boolean keepRawBytecode) {
        super();
        if (parent == null) {
            parent = getClass().getClassLoader();
        }
        mSuperLoader = parent;
        if (rootClassDirs != null) {
            mRootClassDirs = (File[])rootClassDirs.clone();
        }
        if (rootPackage != null && !rootPackage.endsWith(".")) {
            rootPackage += '.';
        }
        mRootPackage = rootPackage;

        if (keepRawBytecode) {
            mGZippedBytecode = Collections.synchronizedMap(new HashMap());
        }
    }
    
    /**
     * Get a stream used to define a class. Close the stream to finish the
     * definition.
     *
     * @param the fully qualified name of the class to be defined.
     */
    public OutputStream getStream(String name) {
        return new Stream(name);
    }

    public URL getResource(String name) {
        
        if (mGZippedBytecode != null) {
            if (mGZippedBytecode.containsKey(name)) {
                try {
                    return new URL("file", null, -1, name, getURLFaker());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("created URL for " + name);
            }
        }
        return mSuperLoader.getResource(name);
    }

    private URLStreamHandler getURLFaker() {
        if (mFaker == null) {
            mFaker = new URLFaker();
        }
        return mFaker;
    }

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        Class clazz = findLoadedClass(name);

        if (clazz == null) {
            synchronized (this) {
                clazz = findLoadedClass(name);

                if (clazz == null) {
                    clazz = loadFromFile(name);

                    if (clazz == null) {
                        if (mSuperLoader != null) {
                            clazz = mSuperLoader.loadClass(name);
                        }
                        else {
                            clazz = findSystemClass(name);
                        }

                        if (clazz == null) {
                            throw new ClassNotFoundException(name);
                        }
                    }
                }
            }
        }
        
        if (resolve) {
            resolveClass(clazz);
        }
        
        return clazz;
    }
    
    protected void define(String name, byte[] data) {
        defineClass(name, data, 0, data.length);
        if (mGZippedBytecode != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gz = new GZIPOutputStream(baos);
                gz.write(data,0,data.length);
                gz.close();
                mGZippedBytecode.put(name.replace('.','/') + ".class",
                                     baos.toByteArray());
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
    
    private Class loadFromFile(String name) throws ClassNotFoundException {
        if (mRootClassDirs == null) {
            return null;
        }

        String fileName = name;
        
        if (mRootPackage != null) {
            if (fileName.startsWith(mRootPackage)) {
                fileName = fileName.substring(mRootPackage.length());
            }
            else {
                return null;
            }
        }

        fileName = fileName.replace('.', File.separatorChar);
        ClassNotFoundException error = null;

        for (int i=0; i<mRootClassDirs.length; i++) {
            File file = new File(mRootClassDirs[i], fileName + ".class");
            
            if (file.exists()) {
                try {
                    byte[] buffer = new byte[(int)file.length()];
                    int avail = buffer.length;
                    int offset = 0;
                    InputStream in = new FileInputStream(file);
                    
                    int len = -1;
                    while ( (len = in.read(buffer, offset, avail)) > 0 ) {
                        offset += len;
                        
                        if ( (avail -= len) <= 0 ) {
                            avail = buffer.length;
                            byte[] newBuffer = new byte[avail * 2];
                            System.arraycopy(buffer, 0, newBuffer, 0, avail);
                            buffer = newBuffer;
                        }
                    }
                    
                    in.close();
                    
                    return defineClass(name, buffer, 0, offset);
                }
                catch (IOException e) {
                    if (error == null) {
                        error = new ClassNotFoundException
                            (fileName + ": " + e.toString());
                    }
                }
            }
        }

        if (error != null) {
            throw error;
        }
        else {
            return null;
        }
    }

    private class Stream extends ByteArrayOutputStream {
        private String mName;
        
        public Stream(String name) {
            super(1024);
            mName = name;
        }
        
        public void close() {
            synchronized (mDefined) {
                if (mDefined.get(mName) == null) {
                    define(mName, toByteArray());
                    mDefined.put(mName, mName);
                }
            }
        }
    }

    private class URLFaker extends URLStreamHandler {
        
        protected URLConnection openConnection(URL u) throws IOException {
            return new ClassInjector.ResourceConnection(u);
        }
    }

    private class ResourceConnection extends URLConnection {

        String resourceName;
        public ResourceConnection(URL u) {
            super(u);
            resourceName = u.getFile();
        }
                    
        // not really needed here but it was abstract.
        public void connect() {}
                    
        public InputStream getInputStream() throws IOException {

            try {
                if (mGZippedBytecode != null) {
                                
                    if (mGZippedBytecode.get(resourceName) != null) {
                        return new GZIPInputStream(new ByteArrayInputStream
                                ((byte[])mGZippedBytecode.get(resourceName)));
                    }
                    else {
                        System.out.println(resourceName + " not found in bytecode map.");
                    }
                }
                else {
                    System.out.println("no bytecode map configured in "+ ClassInjector.this);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
     
            return null;
        }
    }
}
