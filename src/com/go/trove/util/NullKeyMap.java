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

import java.util.*;
import java.io.Serializable;

/******************************************************************************
 * A Map supporting null keys that wraps a Map that doesn't support null keys.
 * NullKeyMap substitutes null keys with a special placeholder object. This
 * technique does not work when the wrapped Map is a TreeMap because it cannot
 * be compared against other objects. In order for TreeMaps to support null
 * keys, use any of the null ordering comparators found in the {@link Utils}
 * class.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class NullKeyMap extends AbstractMap implements Serializable {
    // Instead of using null as a key, use this placeholder.
    private static final Object NULL = new Serializable() {};

    private Map mMap;

    private transient Set mKeySet;
    private transient Set mEntrySet;

    /**
     * @param map The map to wrap.
     */
    public NullKeyMap(Map map) {
        mMap = map;
    }
    
    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return (key == null) ? mMap.containsKey(NULL) : mMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return mMap.containsValue(value);
    }

    public Object get(Object key) {
        return (key == null) ? mMap.get(NULL) : mMap.get(key);
    }

    public Object put(Object key, Object value) {
        return mMap.put((key == null) ? NULL : key, value);
    }

    public Object remove(Object key) {
        return (key == null) ? mMap.remove(NULL) : mMap.remove(key);
    }

    public void putAll(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mMap.clear();
    }

    public Set keySet() {
        if (mKeySet == null) {
            mKeySet = new AbstractSet() {
                public Iterator iterator() {
                    final Iterator it = mMap.keySet().iterator();

                    return new Iterator() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        public Object next() {
                            Object key = it.next();
                            return (key == NULL) ? null : key;
                        }

                        public void remove() {
                            it.remove();
                        }
                    };
                }
                
                public boolean contains(Object key) {
                    return containsKey((key == null) ? NULL : key);
                }

                public boolean remove(Object key) {
                    if (key == null) {
                        key = NULL;
                    }
                    if (containsKey(key)) {
                        NullKeyMap.this.remove(key);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                public int size() {
                    return NullKeyMap.this.size();
                }
                
                public void clear() {
                    NullKeyMap.this.clear();
                }
            };
        }

        return mKeySet;
    }

    public Collection values() {
        return mMap.values();
    }

    public Set entrySet() {
        if (mEntrySet == null) {
            mEntrySet = new AbstractSet() {
                public Iterator iterator() {
                    final Iterator it = mMap.entrySet().iterator();

                    return new Iterator() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        public Object next() {
                            final Map.Entry entry = (Map.Entry)it.next();
                            if (entry.getKey() == NULL) {
                                return new AbstractMapEntry() {
                                    public Object getKey() {
                                        return null;
                                    }

                                    public Object getValue() {
                                        return entry.getValue();
                                    }

                                    public Object setValue(Object value) {
                                        return entry.setValue(value);
                                    }
                                };
                            }
                            else {
                                return entry;
                            }
                        }

                        public void remove() {
                            it.remove();
                        }
                    };
                }
                
                public boolean contains(Object obj) {
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = (Map.Entry)obj;
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (key == null) {
                        key = NULL;
                    }
                    Object lookup = get(key);
                    if (lookup == null) {
                        return value == null;
                    }
                    else {
                        return lookup.equals(value);
                    }
                }

                public boolean remove(Object obj) {
                    if (!(obj instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry entry = ((Map.Entry)obj);
                    Object key = entry.getKey();
                    if (key == null) {
                        key = NULL;
                    }
                    if (containsKey(key)) {
                        NullKeyMap.this.remove(key);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                public int size() {
                    return NullKeyMap.this.size();
                }
                
                public void clear() {
                    NullKeyMap.this.clear();
                }
            };
        }

        return mEntrySet;
    }
}
