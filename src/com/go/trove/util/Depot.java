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
import com.go.trove.util.tq.*;

/******************************************************************************
 * Depot implements a simple and efficient caching strategy. It is thread-safe,
 * and it allows requests of different objects to occur concurrently. Depot
 * is best suited as a front-end for accessing objects from a remote device,
 * like a database. If the remote device is not responding, the Depot will
 * continue to serve invalidated objects so that the requester may continue
 * as normal.
 * <p>
 * Depot is designed as a cache in front of an object {@link Factory factory}.
 * Objects may be invalidated, but they are not explicitly removed from the
 * cache until a replacement has been provided by the factory. The factory is
 * invoked from another thread, allowing for the requester to timeout and use
 * an invalidated object. When the factory eventually finishes, its object will
 * be cached.
 * <p>
 * By allowing for eventual completion of the factory, Depot enables
 * applications to dynamically adjust to the varying performance and
 * reliability of remote data providers.
 * <p>
 * Depot will never return an object or null that did not originate from the
 * factory. When retrieving an object that wasn't found cached, a call to the
 * factory will block until it is finished.
 * <p>
 * Objects may be invalided from the Depot
 * {@link PerishablesFactory automatically}. This approach is based on a fixed
 * time expiration and is somewhat inflexible. An ideal invalidation strategy
 * requires asynchronous notification from the actual data providers.
 *
 * @author Brian S O'Neill, Travis Greer
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/07/09 <!-- $-->
 * @see MultiKey
 */
public class Depot {
    private Factory mDefaultFactory;
    private Map mValidCache;
    private Map mInvalidCache;
    private TransactionQueue mQueue;
    private long mTimeout;

    private Map mRetrievers;

    private final Object mExpireLock = new Object();
    private Map mExpirations;

    /**
     * @param factory Default factory from which objects are obtained
     * @param validCache Map to use for caching valid objects
     * @param invalidCache Map to use for caching invalid objects
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     */
    public Depot(Factory factory, Map validCache, Map invalidCache,
                 TransactionQueue tq, long timeout) {
        init(factory, validCache, invalidCache, tq, timeout);
    }
    
    /**
     * @param factory Default factory from which objects are obtained
     * @param cacheSize Number of items guaranteed to be in cache, if negative,
     * cache is completely disabled.
     * @param tq TransactionQueue for scheduling factory invocations.
     * @param timeout Default timeout (in milliseconds) to apply to "get"
     * method.
     */
    public Depot(Factory factory, int cacheSize, 
                 TransactionQueue tq, long timeout) {
        Map valid, invalid;

        if (cacheSize < 0) {
            valid = Utils.VOID_MAP;
            invalid = Utils.VOID_MAP;
        }
        else {
            valid = new Cache(cacheSize);
            invalid = new Cache((Cache)valid);
        }

        init(factory, valid, invalid, tq, timeout);
    }

    private void init(Factory factory, Map validCache, Map invalidCache,
                      TransactionQueue tq, long timeout) {
        mDefaultFactory = factory;
        mValidCache = Collections.synchronizedMap(validCache);
        mInvalidCache = Collections.synchronizedMap(invalidCache);
        mQueue = tq;
        mTimeout = timeout;

        mRetrievers = Collections.synchronizedMap(new HashMap());
    }

    /**
     * Returns the total number objects in the Depot.
     */
    public int size() {
        synchronized (mValidCache) {
            return mValidCache.size() + mInvalidCache.size();
        }
    }

    public boolean isEmpty() {
        synchronized (mValidCache) {
            return mValidCache.isEmpty() && mInvalidCache.isEmpty();
        }
    }

    /**
     * Returns the number of valid objects in the Depot.
     */
    public int validSize() {
        return mValidCache.size();
    }

    /**
     * Returns the number of invalid objects in the Depot.
     */
    public int invalidSize() {
        return mInvalidCache.size();
    }
    
    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param key key of object to retrieve
     */
    public Object get(Object key) {
        return get(mDefaultFactory, key, mTimeout);
    }

    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param key key of object to retrieve
     * @param timeout max time (in milliseconds) to wait for an invalid value
     * to be replaced from the factory, if negative, wait forever. Ignored if
     * no cached value exists at all.
     */
    public Object get(Object key, long timeout) {
        return get(mDefaultFactory, key, timeout);
    }

    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param factory factory to use to retrieve object if not cached
     * @param key key of object to retrieve
     */
    public Object get(Factory factory, Object key) {
        return get(factory, key, mTimeout);
    }

    /**
     * Retrieve an object from the Depot. If the requested object is in the
     * cache of valid objects, it is returned immediately. If the object is
     * found in the cache of invalid objects, then it will be returned only if
     * the factory cannot create a replacement in a timely manner. If the
     * requested object is not in any cache at all, the factory is called to
     * create the object, and the calling thread will block until the factory
     * has finished.
     *
     * @param factory factory to use to retrieve object if not cached
     * @param key key of object to retrieve
     * @param timeout max time (in milliseconds) to wait for an invalid value
     * to be replaced from the factory, if negative, wait forever. Ignored if
     * no cached value exists at all.
     */
    public Object get(Factory factory, Object key, long timeout) {
        Retriever r;

        key = Utils.intern(key);
        synchronized (key) {
             Object value = mValidCache.get(key);
             if (value != null || mValidCache.containsKey(key)) {
                 validTest: {
                     if (value instanceof Perishable) {
                         if (!((Perishable)value).isValid()) {
                             break validTest;
                         }
                     }
                     
                     synchronized (mExpireLock) {
                         if (mExpirations == null) {
                             return value;
                         }
                         Long expire = (Long)mExpirations.get(key);
                         if (expire == null ||
                             System.currentTimeMillis() <= expire.longValue()) {
                             // Value is still valid.
                             return value;
                         }
                         
                         // Value has expired.
                         mExpirations.remove(key);
                     }
                 }
                 
                 mValidCache.remove(key);
                 mInvalidCache.put(key, value);
                 
                 r = retrieve(factory, key, value, false);
             }
             else {
                 value = mInvalidCache.get(key);
                 
                 if (value != null || mInvalidCache.containsKey(key)) {
                     r = retrieve(factory, key, value, false);
                 }
                 else {
                     // Wait forever since not even an invalid value exists.
                     timeout = -1;
                     r = retrieve(factory, key, value, true);
                 }
             }

            if (r == null) {
                return value;
            }
            else {
                return r.waitForValue(timeout);
            }
        }
    }

    /**
     * Invalidate the object referenced by the given key, if it is already
     * cached in this Depot. Invalidated objects are not removed from the
     * Depot until a replacement object has been successfully created from the
     * factory.
     *
     * @param key key of object to invalidate
     */
    public void invalidate(Object key) {
        key = Utils.intern(key);
        synchronized (key) {
            if (mValidCache.containsKey(key)) {
                Object value = mValidCache.remove(key);
                mInvalidCache.put(key, value);
            }
        }
    }

    /**
     * Invalidates objects in the Depot, using a filter. Each key that the
     * filter accepts is invalidated.
     */
    public void invalidateAll(Filter filter) {
        synchronized (mValidCache) {
            Iterator it = mValidCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                Object key = entry.getKey();
                if (filter.accept(key)) {
                    it.remove();
                    mInvalidCache.put(key, entry.getValue());
                }
            }
        }
    }

    /**
     * Invalidates all the objects in the Depot.
     */
    public void invalidateAll() {
        synchronized (mValidCache) {
            synchronized (mInvalidCache) {
                mInvalidCache.putAll(mValidCache);
                mValidCache.clear();
            }
        }
    }

    /**
     * Put a value into the Depot, bypassing the factory. Invalidating an
     * object and relying on the factory to produce a new value is generally
     * preferred. This method will notify any threads waiting on a factory to
     * produce a value, but it will not disrupt the behavior of the factory.
     *
     * @param key key with which to associate the value.
     * @param value value to be associated with key.
     */
    public void put(Object key, Object value) {
        key = Utils.intern(key);
        synchronized (key) {
            mInvalidCache.remove(key);
            mValidCache.put(key, value);
            Retriever r = (Retriever)mRetrievers.get(key);
            if (r != null) {
                // Bypass the factory produced value so that any waiting
                // threads are notified.
                r.bypassValue(value);
            }
        }
    }

    /**
     * Completely removes an item from the Depot's caches. Invalidating an
     * object is preferred, and remove should be called only if the object
     * should absolutely never be used again.
     */
    public Object remove(Object key) {
        key = Utils.intern(key);
        synchronized (key) {
            if (mValidCache.containsKey(key)) {
                return mValidCache.remove(key);
            }
            if (mInvalidCache.containsKey(key)) {
                return mInvalidCache.remove(key);
            }
        }
        return null;
    }

    /**
     * Completely removes all the items from the Depot that the given filter
     * accepts.
     */
    public void removeAll(Filter filter) {
        synchronized (mValidCache) {
            synchronized (mInvalidCache) {
                Iterator it = mValidCache.keySet().iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    if (filter.accept(key)) {
                        it.remove();
                    }
                }
                it = mInvalidCache.keySet().iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    if (filter.accept(key)) {
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * Completely removes all items from the Depot's caches. Invalidating all
     * the objects is preferred, and clear should be called only if all the
     * cached objects should absolutely never be used again.
     */
    public void clear() {
        synchronized (mValidCache) {
            synchronized (mInvalidCache) {
                mValidCache.clear();
                mInvalidCache.clear();
            }
        }
    }

    void setExpiration(Object key, long duration) {
        Long expire = new Long
            (System.currentTimeMillis() + duration);
        synchronized (mExpireLock) {
            if (mExpirations == null) {
                mExpirations = new IdentityMap();
            }
            mExpirations.put(key, expire);
        }
    }

    /**
     * Caller must lock interned key.
     */
    private Retriever retrieve(Factory factory,
                               Object key,
                               Object originalValue,
                               boolean priority)
    {
        Retriever r = (Retriever)mRetrievers.get(key);

        if (r == null) {
            r = new Retriever(factory, key, originalValue);
            if (mQueue.enqueue(r)) {
                mRetrievers.put(key, r);
            }
            else if (priority) {
                // Skip the queue, service it in this thread.
                mRetrievers.put(key, r);
                r.service();
            }
            else {
                return null;
            }
        }

        return r;
    }

    /**
     * Implement this interface in order for Depot to retrieve objects when
     * needed, often in a thread other than the requester's.
     *
     * @see PerishablesFactory
     */
    public interface Factory {
        /**
         * Create an object that is mapped by the given key. This method must
         * be thread-safe, but simply making it synchronized may severely
         * impact the Depot's support of concurrent activity.
         * <p>
         * Create may abort its operation by throwing an InterruptedException.
         * This forces an invalid object to be used or null if none. If an
         * InterruptedException is thrown, nether the invalid object or null
         * will be cached. Null is cached only if the factory returns it
         * directly.
         *
         * @throws InterruptedException explicitly throwing this exception
         * allows the factory to abort creating an object.
         */
        public Object create(Object key) throws InterruptedException;
    }

    /**
     * A special kind of Factory that creates objects that are considered
     * invalid after a specific amount of time has elapsed.
     */
    public interface PerishablesFactory extends Factory {
        /**
         * Returns the maximum amout of time (in milliseconds) that objects
         * from this factory should remain valid. Returning a value less than
         * or equal to zero causes objects to be immediately invalidated.
         */
        public long getValidDuration();
    }

    /**
     * Values returned from the Factories may implement this interface if they
     * manually handle expiration.
     */
    public interface Perishable {
        /**
         * If this Perishable is still valid, but it came from a
         * PerishablesFactory, it may be considered invalid if the valid
         * duration has elapsed.
         */
        public boolean isValid();
    }

    public interface Filter {
        /**
         * Returns true if the given key should be included in an operation,
         * such as invalidation.
         */
        public boolean accept(Object key);
    }

    private class Retriever implements Transaction {
        private final Factory mFactory;
        private final Object mKey;
        private Object mValue;
        private boolean mDone;

        /**
         * @param key Key must already be interned.
         */
        public Retriever(Factory factory, Object key, Object originalValue) {
            mFactory = factory;
            mKey = key;
            mValue = originalValue;
        }

        public Object waitForValue(long timeout) {
            try {
                synchronized (mKey) {
                    if (mDone || timeout == 0) {
                        return mValue;
                    }
                    
                    if (timeout < 0) {
                        timeout = 0;
                    }
                    
                    mKey.wait(timeout);
                }
            }
            catch (InterruptedException e) {
            }

            return mValue;
        }

        public void bypassValue(Object value) {
            synchronized (mKey) {
                mValue = value;
                mKey.notifyAll();
            }
        }

        public void service() {
            try {
                Thread t = Thread.currentThread();
                String originalName = t.getName();
                t.setName(originalName + ' ' + mKey);
                try {
                    mValue = mFactory.create(mKey);
                    synchronized (mKey) {
                        if (mFactory instanceof PerishablesFactory) {
                            long duration = ((PerishablesFactory)mFactory)
                                .getValidDuration();
                            if (duration <= 0) {
                                mInvalidCache.put(mKey, mValue);
                                mValidCache.remove(mKey);
                            }
                            else {
                                mInvalidCache.remove(mKey);
                                mValidCache.put(mKey, mValue);
                                setExpiration(mKey, duration);
                            }
                        }
                        else {
                            mInvalidCache.remove(mKey);
                            mValidCache.put(mKey, mValue);
                        }
                    }
                }
                catch (InterruptedException e) {
                }
                finally {
                    t.setName(originalName);
                }
            }
            finally {
                done();
            }
        }

        public void cancel() {
            done();
        }

        private void done() {
            mDone = true;
            mRetrievers.remove(mKey);
            synchronized (mKey) {
                mKey.notifyAll();
            }
        }
    }
}
