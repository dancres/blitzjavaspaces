/* ====================================================================
 * Trove - Copyright (c) 1997-2001 Walt Disney Internet Group
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

/******************************************************************************
 * A Map that implements a write-through cache to another map. Two maps are
 * supplied: one for caching and one for main storage. WrappedCache is not
 * thread-safe and must be wrapped with Collections.synchronizedMap to be made
 * thread-safe.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 2 <!-- $-->, <!--$$JustDate:--> 01/07/31 <!-- $-->
 */
public class WrappedCache extends AbstractMap implements Map {
    private final Map mCacheMap;
    private final Map mBackingMap;

    /**
     * @param cacheMap the cache map should offer fast access, but it should
     * automatically limit its maximum size
     * @param backingMap the backingMap will be read from only if the requested
     * entry isn't in the cache
     */
    public WrappedCache(Map cacheMap, Map backingMap) {
        mCacheMap = cacheMap;
        mBackingMap = backingMap;
    }

    /**
     * Returns the size of the backing map.
     */
    public int size() {
        return mBackingMap.size();
    }

    /**
     * Returns the empty status of the backing map.
     */
    public boolean isEmpty() {
        return mBackingMap.isEmpty();
    }

    /**
     * Returns true if the cache contains the key or else if the backing map
     * contains the key.
     */
    public boolean containsKey(Object key) {
        return mCacheMap.containsKey(key) || mBackingMap.containsKey(key);
    }

    /**
     * Returns true if the cache contains the value or else if the backing map
     * contains the value.
     */
    public boolean containsValue(Object value) {
        return mCacheMap.containsValue(value) ||
            mBackingMap.containsValue(value);
    }

    /**
     * Returns the value from the cache, or if not found, the backing map.
     * If the backing map is accessed, the value is saved in the cache for
     * future gets.
     */
    public Object get(Object key) {
        Object value = mCacheMap.get(key);
        if (value != null || mCacheMap.containsKey(key)) {
            return value;
        }
        value = mBackingMap.get(key);
        if (value != null || mBackingMap.containsKey(key)) {
            mCacheMap.put(key, value);
        }
        return value;
    }

    /**
     * Puts the entry into both the cache and backing map. The old value in
     * the backing map is returned.
     */
    public Object put(Object key, Object value) {
        mCacheMap.put(key, value);
        return mBackingMap.put(key, value);
    }

    /**
     * Removes the key from both the cache and backing map. The old value in
     * the backing map is returned.
     */
    public Object remove(Object key) {
        mCacheMap.remove(key);
        return mBackingMap.remove(key);
    }

    /**
     * Clears both the cache and backing map.
     */
    public void clear() {
        mCacheMap.clear();
        mBackingMap.clear();
    }

    /**
     * Returns the key set of the backing map.
     */
    public Set keySet() {
        return mBackingMap.keySet();
    }

    /**
     * Returns the values of the backing map.
     */
    public Collection values() {
        return mBackingMap.values();
    }

    /**
     * Returns the entry set of the backing map.
     */
    public Set entrySet() {
        return mBackingMap.entrySet();
    }
}
