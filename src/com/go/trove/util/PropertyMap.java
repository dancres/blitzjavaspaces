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
 * A class that is similar to {@link java.util.Properties} but preserves
 * original property order and supports a special {@link #subMap} view
 * operation. PropertyMap also has more convenience methods for getting
 * properties as certain types.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $--> 4 <!-- $$JustDate:--> 01/02/26 <!-- $-->
 */
public class PropertyMap extends AbstractMap {
    public static final Class ELEMENT_TYPE = String.class;

    private static String internStr(String str) {
        return (String)Utils.intern(str);
    }

    private Map mMappings;
    private String mSeparator;
    private String mPrefix;

    private transient Set mSubMapKeySet;
    private transient Set mEntrySet;

    /**
     * Construct a PropetyMap using a dot (".") separator.
     */
    public PropertyMap() {
        this(null, ".");
    }

    /**
     * @param map Map of defaults.
     */
    public PropertyMap(Map map) {
        this(map, ".");
    }

    /**
     * @param separator Sub-key separator, i.e. ".".
     */
    public PropertyMap(String separator) {
        this(null, separator);
    }

    /**
     * @param map Optional map of defaults.
     * @param separator Sub-key separator, i.e. ".".
     *
     * @see #putDefaults(Map)
     */
    public PropertyMap(Map map, String separator) {
        UsageMap usageMap = new UsageMap();
        usageMap.setReverseOrder(true);
        mMappings = usageMap;
        mSeparator = separator;
        if (map != null) {
            putAll(map);
        }
    }

    private PropertyMap(String prefix, PropertyMap source) {
        mMappings = source;
        mSeparator = source.getSeparator();
        mPrefix = prefix;
    }

    public String getSeparator() {
        return mSeparator;
    }

    /**
     * Returns a view of this map for keys that are the same as the given key,
     * or start with it (and a separator). The names of the keys in the
     * sub-map have their prefix truncated.
     *
     * <p>A sub-map of
     *
     * <pre>
     * "x" = "a"
     * "foo" = "b"
     * "foo." = "c"
     * "foo.bar" = "d"
     * "foo.bar.splat" = "e"
     * "foo..bar" = "f"
     * ".foo" = "g"
     * "" = "h"
     * null = "i"
     * </pre>
     * 
     * using a key of "foo" results in
     *
     * <pre>
     * null = "b"
     * "" = "c"
     * "bar" = "d"
     * "bar.splat" = "e"
     * ".bar" = "f"
     * </pre>
     *
     * using a key of "x" results in
     *
     * <pre>
     * null = "a"
     * </pre>
     *
     * using a key of "" results in
     *
     * <pre>
     * "foo" = "g"
     * null = "h"
     * </pre>
     *
     * and using a key of null results in the original map.
     */
    public PropertyMap subMap(String key) {
        if (key == null) {
            return this;
        }
        else {
            return new PropertyMap(key, this);
        }
    }

    /**
     * Returns the key names of each sub-map in this PropertyMap. The returned
     * set is unmodifiable.
     */
    public Set subMapKeySet() {
        if (mSubMapKeySet == null) {
            mSubMapKeySet = new SubMapKeySet();
        }
        return mSubMapKeySet;
    }

    public int size() {
        if (mPrefix == null) {
            return mMappings.size();
        }
        else {
            return super.size();
        }
    }

    public boolean isEmpty() {
        if (mPrefix == null) {
            return mMappings.isEmpty();
        }
        else {
            return super.isEmpty();
        }
    }
    
    public boolean containsKey(Object key) {
        if (key == null) {
            return containsKey((String)null);
        }
        else {
            return containsKey(key.toString());
        }
    }
    
    public boolean containsKey(String key) {
        if (mPrefix == null) {
            return mMappings.containsKey(internStr(key));
        }
        else {
            return mMappings.containsKey(mPrefix + mSeparator + key);
        }
    }
    
    public Object get(Object key) {
        if (key == null) {
            return get((String)null);
        }
        else {
            return get(key.toString());
        }
    }

    public Object get(String key) {
        if (mPrefix == null) {
            return mMappings.get(internStr(key));
        }
        else {
            return mMappings.get(mPrefix + mSeparator + key);
        }
    }

    /**
     * Returns null if the given key isn't in this PropertyMap.
     *
     * @param key Key of property to read
     */
    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        else {
            return value.toString();
        }
    }
    
    /**
     * Returns the default value if the given key isn't in this PropertyMap.
     *
     * @param key Key of property to read
     * @param def Default value
     */
    public String getString(String key, String def) {
        Object value = get(key);
        if (value == null) {
            return def;
        }
        else {
            return value.toString();
        }
    }
    
    /**
     * Returns 0 if the given key isn't in this PropertyMap.
     *
     * @param key Key of property to read
     */
    public int getInt(String key) throws NumberFormatException {
        String value = getString(key);
        if (value == null) {
            return 0;
        }
        else {
            return Integer.parseInt(value);
        }
    }

    /**
     * Returns the default value if the given key isn't in this PropertyMap or
     * isn't a valid integer.
     *
     * @param key Key of property to read
     * @param def Default value
     */
    public int getInt(String key, int def) {
        String value = getString(key);
        if (value == null) {
            return def;
        }
        else {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
            }
            return def;
        }
    }

    /**
     * Returns null if the given key isn't in this PropertyMap or it isn't a
     * valid integer.
     *
     * @param key Key of property to read
     */
    public Integer getInteger(String key) {
        return getInteger(key, null);
    }

    /**
     * Returns the default value if the given key isn't in this PropertyMap or
     * it isn't a valid integer.
     *
     * @param key Key of property to read
     * @param def Default value
     */
    public Integer getInteger(String key, Integer def) {
        String value = getString(key);
        if (value == null) {
            return def;
        }
        else {
            try {
                return Integer.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            return def;
        }
    }

    /**
     * Returns null if the given key isn't in this PropertyMap.
     *
     * @param key Key of property to read
     */
    public Number getNumber(String key) throws NumberFormatException {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        else {
            try {
                return Integer.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            try {
                return Long.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            return Double.valueOf(value);
        }
    }

    /**
     * Returns the default value if the given key isn't in this PropertyMap or
     * isn't a valid number.
     *
     * @param key Key of property to read
     * @param def Default value
     */
    public Number getNumber(String key, Number def) {
        String value = getString(key);
        if (value == null) {
            return def;
        }
        else {
            try {
                return Integer.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            try {
                return Long.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            try {
                return Double.valueOf(value);
            }
            catch (NumberFormatException e) {
            }
            return def;
        }
    }

    /**
     * Returns true only if value is "true", ignoring case.
     *
     * @param key Key of property to read
     */
    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key));
    }

    /**
     * Returns the default value if the given key isn't in this PropertyMap or
     * if the the value isn't equal to "true", ignoring case.
     *
     * @param key Key of property to read
     * @param def Default value
     */
    public boolean getBoolean(String key, boolean def) {
        String value = getString(key);
        if (value == null) {
            return def;
        }
        else {
            return "true".equalsIgnoreCase(value);
        }
    }

    public Set entrySet() {
        if (mPrefix == null) {
            return mMappings.entrySet();
        }
        else {
            if (mEntrySet == null) {
                mEntrySet = new EntrySet();
            }
            return mEntrySet;
        }
    }

    public Set keySet() {
        if (mPrefix == null) {
            return mMappings.keySet();
        }
        else {
            return super.keySet();
        }
    }

    public Collection values() {
        if (mPrefix == null) {
            return mMappings.values();
        }
        else {
            return super.values();
        }
    }

    /**
     * The key is always converted to a String.
     */
    public Object put(Object key, Object value) {
        if (key == null) {
            return put((String)null, value);
        }
        else {
            return put(key.toString(), value);
        }
    }

    public Object put(String key, Object value) {
        if (mPrefix == null) {
            return mMappings.put(internStr(key), value);
        }
        else {
            return mMappings.put(mPrefix + mSeparator + key, value);
        }
    }

    /**
     * Copies the entries of the given map into this one only for keys that
     * aren't contained in this map. Is equivalent to putAll if this map is
     * empty.
     */
    public void putDefaults(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            Object key = entry.getKey();
            if (!containsKey(key)) {
                put(key, entry.getValue());
            }
        }
    }
    
    public Object remove(Object key) {
        if (key == null) {
            return remove((String)null);
        }
        else {
            return remove(key.toString());
        }
    }
    
    public Object remove(String key) {
        if (mPrefix == null) {
            return mMappings.remove(internStr(key));
        }
        else {
            return mMappings.remove(mPrefix + mSeparator + key);
        }
    }
    
    public void clear() {
        if (mPrefix == null) {
            mMappings.clear();
        }
        else {
            super.clear();
        }
    }

    private class SubMapKeySet extends AbstractSet {
        public int size() {
            int size = 0;
            Iterator it = iterator();
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }
        
        public Iterator iterator() {
            return new Iterator() {
                final Iterator mIterator = keySet().iterator();
                Set mSeen = new HashSet();
                String mNext;

                public boolean hasNext() {
                    if (mNext != null) {
                        return true;
                    }

                    String sep = mSeparator;

                    while (mIterator.hasNext()) {
                        String key = (String)mIterator.next();
                        if (key != null) {
                            int index = key.indexOf(sep);
                            if (index >= 0) {
                                String subKey = key.substring(0, index);
                                if (!mSeen.contains(subKey)) {
                                    mSeen.add(subKey);
                                    mNext = subKey;
                                    return true;
                                }
                            }
                        }
                    }

                    return false;
                }

                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    else {
                        Object next = mNext;
                        mNext = null;
                        return next;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private class EntrySet extends AbstractSet {
        public int size() {
            int size = 0;
            Iterator it = iterator();
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }
        
        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry) {
                Object key = ((Map.Entry)obj).getKey();
                Object value = ((Map.Entry)obj).getValue();

                if (PropertyMap.this.containsKey(key)) {
                    Object v = PropertyMap.this.get(key);
                    if (v == null) {
                        if (value == null) {
                            PropertyMap.this.remove(key);
                            return true;
                        }
                    }
                    else if (v.equals(value)) {
                        PropertyMap.this.remove(key);
                        return true;
                    }
                }
            }
            return false;
        }
        
        public void clear() {
            Iterator it = iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        public Iterator iterator() {
            return new Iterator() {
                final Iterator mIterator = mMappings.entrySet().iterator();
                Map.Entry mNext;

                public boolean hasNext() {
                    if (mNext != null) {
                        return true;
                    }

                    String prefix = mPrefix;
                    String sep = mSeparator;

                    while (mIterator.hasNext()) {
                        final Map.Entry entry = (Map.Entry)mIterator.next();
                        String key = (String)entry.getKey();

                        if (key != null && key.startsWith(prefix)) {
                            final String subKey;
                            if (key.length() == prefix.length()) {
                                subKey = null;
                            }
                            else {
                                if (key.startsWith(sep, prefix.length())) {
                                    subKey = key.substring
                                        (prefix.length() + sep.length());
                                }
                                else {
                                    continue;
                                }
                            }

                            mNext = new AbstractMapEntry() {
                                public Object getKey() {
                                    return subKey;
                                }

                                public Object getValue() {
                                    return entry.getValue();
                                }
                                
                                public Object setValue(Object value) {
                                    return put(subKey, value);
                                }
                            };

                            return true;
                        }
                    }

                    return false;
                }

                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    else {
                        Object next = mNext;
                        mNext = null;
                        return next;
                    }
                }

                public void remove() {
                    mIterator.remove();
                }
            };
        }
    }
}
