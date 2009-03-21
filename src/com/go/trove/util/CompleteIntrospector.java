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

import java.beans.*;
import java.util.*;
import java.lang.ref.*;

/******************************************************************************
 * A JavaBean Introspector that ensures interface properties are properly
 * discovered.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/13 <!-- $-->
 */
public class CompleteIntrospector {
    // Weakly maps Class objects to softly referenced PropertyDescriptor maps.
    private static Map cPropertiesCache;

    static {
        cPropertiesCache = new IdentityMap();

        // The Introspector has a poor design in that a special GLOBAL setting
        // is used. The default setting has a negative affect because the
        // BeanInfo search path disregards packages. By default, the search
        // path provides a BeanInfo for Component, which is for
        // java.awt.Component. However, any class with this name, in any
        // package, will be given the visible properties of java.awt.Component.
        Introspector.setBeanInfoSearchPath(new String[0]);
    }

    /**
     * Test program.
     */
    public static void main(String[] args) throws Exception {
        Map map = getAllProperties(Class.forName(args[0]));
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            PropertyDescriptor desc = (PropertyDescriptor)map.get(key);
            System.out.println(key + " = " + desc);
        }
    }

    /**
     * A function that returns a Map of all the available properties on
     * a given class including write-only properties. The properties returned
     * is mostly a superset of those returned from the standard JavaBeans 
     * Introspector except more properties are made available to interfaces.
     * 
     * @return an unmodifiable mapping of property names (Strings) to
     * PropertyDescriptor objects.
     *
     */
    public static Map getAllProperties(Class clazz)
        throws IntrospectionException {
        
        synchronized (cPropertiesCache) {
            Map properties;

            Reference ref = (Reference)cPropertiesCache.get(clazz);
            if (ref != null) {
                properties = (Map)ref.get();
                if (properties != null) {
                    return properties;
                }
                else {
                    // Clean up cleared reference.
                    cPropertiesCache.remove(clazz);
                }
            }

            properties = Collections.unmodifiableMap(createProperties(clazz));
            cPropertiesCache.put(clazz, new SoftReference(properties));
            return properties;
        }
    }

    private static Map createProperties(Class clazz)
        throws IntrospectionException {

        Map properties = new HashMap();

        if (clazz == null || clazz.isPrimitive()) {
            return properties;
        }
        
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(clazz);
        }
        catch (LinkageError e) {
            throw new IntrospectionException(e.toString());
        }

        if (info != null) {
            PropertyDescriptor[] pdArray = info.getPropertyDescriptors();
            
            // Standard properties.
            int length = pdArray.length;
            for (int i=0; i<length; i++) {
                properties.put(pdArray[i].getName(), pdArray[i]);
            }
        }

        // Properties defined in Object are also available to interfaces.
        if (clazz.isInterface()) {
            properties.putAll(getAllProperties(Object.class));
        }

        // Ensure that all implemented interfaces are properly analyzed.
        Class[] interfaces = clazz.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            properties.putAll(getAllProperties(interfaces[i]));
        }

        // Filter out properties with names that contain '$' characters.
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()) {
            String propertyName = (String)it.next();
            if (propertyName.indexOf('$') >= 0) {
                it.remove();
            }
        }

        return properties;
    }
}
