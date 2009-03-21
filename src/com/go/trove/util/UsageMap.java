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
 * A Map that orders its keys based on how recently they have been used.
 * Most recently used keys appear first in the Map. Keys are marked as being
 * used whenever they are put into to the Map. To re-position a key, put it
 * back in.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 15 <!-- $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class UsageMap extends AbstractMap implements java.io.Serializable {
    private Map mRecentMap;
    private boolean mReverse;

    private Entry mMostRecent;
    private Entry mLeastRecent;

    private transient Set mEntrySet;

    /**
     * Creates a UsageMap in forward order, MRU first.
     */
    public UsageMap() {
        this(new HashMap());
    }

    /**
     * @param backingMap map to use for storage
     */
    public UsageMap(Map backingMap) {
        mRecentMap = backingMap;
    }

    /**
     * With reverse order, keys are ordered least recently used first. The
     * ordering of the map entries will be consistent with the order they were
     * put into it. Switching to and from reverse order is performed quickly
     * and is not affected by the current size of the map.
     */
    public void setReverseOrder(boolean reverse) {
        mReverse = reverse;
    }

    /**
     * Returns the first key in the map, the most recently used. If reverse
     * order, then the least recently used is returned.
     */
    public Object firstKey() throws NoSuchElementException {
        Entry first = (mReverse) ? mLeastRecent : mMostRecent;
        if (first != null) {
            return first.mKey;
        }
        else if (mRecentMap.size() == 0) {
            throw new NoSuchElementException();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the last key in the map, the least recently used. If reverse
     * order, then the most recently used is returned.
     */
    public Object lastKey() throws NoSuchElementException {
        Entry last = (mReverse) ? mMostRecent : mLeastRecent;
        if (last != null) {
            return last.mKey;
        }
        else if (mRecentMap.size() == 0) {
            throw new NoSuchElementException();
        }
        else {
            return null;
        }
    }

    public int size() {
        return mRecentMap.size();
    }

    public boolean isEmpty() {
        return mRecentMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return mRecentMap.containsKey(key);
    }

    public Object get(Object key) {
        Entry e = (Entry)mRecentMap.get(key);
        return (e == null) ? null : e.mValue;
    }

    public Object put(Object key, Object value) {
        Entry e = (Entry)mRecentMap.get(key);
        Object old;

        if (e == null) {
            old = null;
            e = new Entry(key, value);
            mRecentMap.put(key, e);
        }
        else {
            old = e.mValue;
            e.mValue = value;

            if (e == mMostRecent) {
                return old;
            }

            // Delete entry from linked list.
            if (e.mPrev == null) {
                mMostRecent = e.mNext;
            }
            else {
                e.mPrev.mNext = e.mNext;
            }
            if (e.mNext == null) {
                mLeastRecent = e.mPrev;
            }
            else {
                e.mNext.mPrev = e.mPrev;
            }
            e.mPrev = null;
        }

        if (mMostRecent == null) {
            mMostRecent = e;
        }
        else {
            e.mNext = mMostRecent;
            mMostRecent.mPrev = e;
            mMostRecent = e;
        }

        if (mLeastRecent == null) {
            mLeastRecent = e;
        }

        return old;
    }

    public Object remove(Object key) {
        Entry e = (Entry)mRecentMap.remove(key);
        
        if (e == null) {
            return null;
        }
        else {
            // Delete entry from linked list.
            if (e.mPrev == null) {
                mMostRecent = e.mNext;
            }
            else {
                e.mPrev.mNext = e.mNext;
            }
            if (e.mNext == null) {
                mLeastRecent = e.mPrev;
            }
            else {
                e.mNext.mPrev = e.mPrev;
            }

            return e.mValue;
        }
    }

    public void putAll(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            mRecentMap.put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mRecentMap.clear();
        mMostRecent = null;
        mLeastRecent = null;
    }

    public Set entrySet() {
        if (mEntrySet != null) {
            return mEntrySet;
        }

        mEntrySet = new AbstractSet() {
            public Iterator iterator() {
                if (mReverse) {
                    return new Iterator() {
                        private Entry mPrev = mLeastRecent;
                        private Entry mLast = null;

                        public boolean hasNext() {
                            return mPrev != null;
                        }

                        public Object next() {
                            if ((mLast = mPrev) == null) {
                                throw new NoSuchElementException();
                            }
                            else {
                                mPrev = mPrev.mPrev;
                                return mLast;
                            }
                        }

                        public void remove() {
                            if (mLast == null) {
                                throw new IllegalStateException();
                            }
                            else {
                                UsageMap.this.remove(mLast.mKey);
                                mLast = null;
                            }
                        }
                    };
                }
                else {
                    return new Iterator() {
                        private Entry mNext = mMostRecent;
                        private Entry mLast = null;

                        public boolean hasNext() {
                            return mNext != null;
                        }

                        public Object next() {
                            if ((mLast = mNext) == null) {
                                throw new NoSuchElementException();
                            }
                            else {
                                mNext = mNext.mNext;
                                return mLast;
                            }
                        }

                        public void remove() {
                            if (mLast == null) {
                                throw new IllegalStateException();
                            }
                            else {
                                UsageMap.this.remove(mLast.mKey);
                                mLast = null;
                            }
                        }
                    };
                }
            }

            public int size() {
                return mRecentMap.size();
            }

            public boolean isEmpty() {
                return mRecentMap.isEmpty();
            }
            
            public boolean contains(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                Entry e = (Entry)mRecentMap.get(((Map.Entry)obj).getKey());
                return e != null && e.equals(obj);
            }

            public boolean remove(Object obj) {
                if (!(obj instanceof Map.Entry)) {
                    return false;
                }
                if (contains(obj)) {
                    UsageMap.this.remove(((Map.Entry)obj).getKey());
                    return true;
                }
                else {
                    return false;
                }
            }

            public void clear() {
                UsageMap.this.clear();
            }
        };

        return mEntrySet;
    }

    private static class Entry extends AbstractMapEntry
        implements java.io.Serializable
    {
        public Entry mPrev;
        public Entry mNext;
        public Object mKey;
        public Object mValue;

        public Entry(Object key, Object value) {
            mKey = key;
            mValue = value;
        }

        public Object getKey() {
            return mKey;
        }

        public Object getValue() {
            return mValue;
        }

        public Object setValue(Object value) {
            Object old = mValue;
            mValue = value;
            return old;
        }
    }
}
