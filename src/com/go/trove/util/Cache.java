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

/******************************************************************************
 * Cache is a SoftHashMap that is guaranteed to have the most recently used
 * entries available. Calling "get" or "put" updates the internal MRU list,
 * but calling "containsKey" or "containsValue" will not.
 * <p>
 * Like its base class, Cache is not thread-safe and must be wrapped with
 * Collections.synchronizedMap to be made thread-safe.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/05/30 <!-- $-->
 */
public class Cache extends SoftHashMap {
    private final int mMaxRecent;
    // Contains hard references to entries.
    private final UsageMap mUsageMap;

    /**
     * Construct a Cache with an amount of recently used entries that are
     * guaranteed to always be in the Cache.
     *
     * @param maxRecent maximum amount of recently used entries guaranteed to
     * be in the Cache.
     * @throws IllegalArgumentException if maxRecent is less than or equal to
     * zero.
     */
    public Cache(int maxRecent) {
        if (maxRecent <= 0) {
            throw new IllegalArgumentException
                ("Max recent must be greater than zero: " + maxRecent);
        }
        mMaxRecent = maxRecent;
        mUsageMap = new UsageMap();
    }

    /**
     * Piggyback this Cache onto another one in order for the map of recently
     * used entries to be shared. If this Cache is more active than the one
     * it attaches to, then more of its most recently used entries will be
     * guaranteed to be in the Cache, possibly bumping out entries from the
     * other Cache.
     */
    public Cache(Cache cache) {
        mMaxRecent = cache.mMaxRecent;
        mUsageMap = cache.mUsageMap;
    }

    public Object get(Object key) {
        Object value = super.get(key);
        if (value != null || super.containsKey(key)) {
            adjustMRU(key, value);
        }
        return value;
    }

    public Object put(Object key, Object value) {
        if (value == null) {
            value = new Null();
        }
        adjustMRU(key, value);
        return super.put(key, value);
    }

    public Object remove(Object key) {
        synchronized (mUsageMap) {
            mUsageMap.remove(key);
        }
        return super.remove(key);
    }

    public void clear() {
        super.clear();
        synchronized (mUsageMap) {
            mUsageMap.clear();
        }
    }

    private void adjustMRU(Object key, Object value) {
        synchronized (mUsageMap) {
            Object existing = mUsageMap.get(key);
            
            if (existing != null) {
                if (value == null && existing instanceof Null) {
                    value = existing;
                }
            }
            else {
                if (!mUsageMap.containsKey(key)) {
                    // A new entry will be put into the UsageMap, so remove
                    // least recently used if MRU is too big.
                    while (mUsageMap.size() >= mMaxRecent) {
                        mUsageMap.remove(mUsageMap.lastKey());
                    }
                }
            }
            
            mUsageMap.put(key, value);
        }
    }
}
