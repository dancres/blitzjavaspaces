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
import java.io.InputStream;
import java.net.URL;

/******************************************************************************
 * ClassLoader that delegates class loading requests if the parent ClassLoader
 * (and its ancestors) can't fulfill the request. Class loading requests are
 * handed off to scouts, which are called upon in sequence to find the class,
 * until the class is found. If neither the parent or scouts can find a
 * requested class, a normal ClassNotFoundException is thrown.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  7/27/01 <!-- $-->
 */
public class DelegateClassLoader extends ClassLoader {
    private ClassLoader[] mScouts;

    /**
     * @param parent ClassLoader that gets first chance to load a class
     * @param scouts ClassLoaders that get next chances to load a class, if the
     * parent couldn't find it.
     */
    public DelegateClassLoader(ClassLoader parent, ClassLoader[] scouts) {
        super(parent);
        mScouts = (ClassLoader[])scouts.clone();
   }

    /**
     * @param scouts ClassLoaders that get next chances to load a class, if the
     * parent couldn't find it.
     * @deprecated Actual parent ClassLoader must always be provided to prevent
     * unexpected behavior.
     */
    public DelegateClassLoader(ClassLoader[] scouts) {
        super();
        mScouts = (ClassLoader[])scouts.clone();
    }

    protected synchronized Class findClass(String name)
        throws ClassNotFoundException
    {
        for (int i=0; i<mScouts.length; i++) {
            try {
                return mScouts[i].loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(name);
    }
    
    protected URL findResource(String name) {
        URL resource = null;

        try {
            for (int i=0; i<mScouts.length; i++) {
                resource = mScouts[i].getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return resource;
    }
}
