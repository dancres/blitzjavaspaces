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
 * A ThreadPool contains a collection of re-usable threads. There is a slight
 * performance overhead in creating new threads, and so a ThreadPool can
 * improve performance in systems that create short-lived threads. Pooled
 * threads operate on Runnable targets and return back to the pool when the
 * Runnable.run method exits.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/03/12 <!-- $-->
 */
public class ThreadPool extends ThreadGroup {
    private static int cThreadID;

    private synchronized static int nextThreadID() {
        return cThreadID++;
    }

    // Fields that use the monitor of this instance.

    private long mTimeout = -1;
    private long mIdleTimeout = -1;

    // Fields that use the mListeners monitor.

    private Collection mListeners = new LinkedList();

    // Fields that use the mPool monitor.

    // Pool is accessed like a stack.
    private LinkedList mPool;
    private int mMax;
    private int mActive;
    private boolean mDaemon;
    private int mPriority;
    private boolean mClosed;

    /**
     * Create a ThreadPool of daemon threads.
     *
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(String name, int max) 
        throws IllegalArgumentException {

        this(name, max, true);
    }

    /**
     * Create a ThreadPool of daemon threads.
     *
     * @param parent Parent ThreadGroup
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(ThreadGroup parent, String name, int max) 
        throws IllegalArgumentException {

        this(parent, name, max, true);
    }

    /**
     * Create a ThreadPool.
     *
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     * @param daemon Set to true to create ThreadPool of daemon threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(String name, int max, boolean daemon) 
        throws IllegalArgumentException {

        super(name);

        init(max, daemon);
    }

    /**
     * Create a ThreadPool.
     *
     * @param parent Parent ThreadGroup
     * @param name Name of ThreadPool
     * @param max The maximum allowed number of threads
     * @param daemon Set to true to create ThreadPool of daemon threads
     *
     * @throws IllegalArgumentException
     */
    public ThreadPool(ThreadGroup parent, String name, int max,boolean daemon) 
        throws IllegalArgumentException {

        super(parent, name);

        init(max, daemon);
    }

    private void init(int max, boolean daemon) 
        throws IllegalArgumentException {

        if (max <= 0) {
            throw new IllegalArgumentException
                ("Maximum number of threads must be greater than zero: " +
                 max);
        }

        mMax = max;

        mDaemon = daemon;
        mPriority = Thread.currentThread().getPriority();
        mClosed = false;

        mPool = new LinkedList();
    }

    /**
     * Sets the timeout (in milliseconds) for getting threads from the pool
     * or for closing the pool. A negative value specifies an infinite timeout.
     * Calling the start method that accepts a timeout value will override 
     * this setting.
     */
    public synchronized void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    /**
     * Returns the timeout (in milliseconds) for getting threads from the pool.
     * The default value is negative, which indicates an infinite wait.
     */
    public synchronized long getTimeout() {
        return mTimeout;
    }

    /**
     * Sets the timeout (in milliseconds) for idle threads to exit. A negative
     * value specifies that an idle thread never exits.
     */
    public synchronized void setIdleTimeout(long timeout) {
        mIdleTimeout = timeout;
    }

    /**
     * Returns the idle timeout (in milliseconds) for threads to exit. The
     * default value is negative, which indicates that idle threads never exit.
     */
    public synchronized long getIdleTimeout() {
        return mIdleTimeout;
    }

    public void addThreadPoolListener(ThreadPoolListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeThreadPoolListener(ThreadPoolListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    /**
     * Returns the initial priority given to each thread in the pool. The
     * default value is that of the thread that created the ThreadPool.
     */
    public int getPriority() {
        synchronized (mPool) {
            return mPriority;
        }
    }
    
    /**
     * Sets the priority given to each thread in the pool.
     *
     * @throws IllegalArgumentException if priority is out of range
     */
    public void setPriority(int priority) throws IllegalArgumentException {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException
                ("Priority out of range: " + priority);
        }

        synchronized (mPool) {
            mPriority = priority;
        }
    }

    /**
     * @return The maximum allowed number of threads.
     */
    public int getMaximumAllowed() {
        synchronized (mPool) {
            return mMax;
        }
    }

    /**
     * @return The number of currently available threads in the pool.
     */
    public int getAvailableCount() {
        synchronized (mPool) {
            return mPool.size();
        }
    }

    /**
     * @return The total number of threads in the pool that are either
     * available or in use.
     */
    public int getPooledCount() {
        synchronized (mPool) {
            return mActive;
        }
    }

    /**
     * @return The total number of threads in the ThreadGroup.
     */
    public int getThreadCount() {
        return activeCount();
    }

    /**
     * @return Each thread that is active in the entire ThreadGroup.
     */
    public Thread[] getAllThreads() {
        int count = activeCount();
        Thread[] threads = new Thread[count];
        count = enumerate(threads);
        if (count >= threads.length) {
            return sort(threads);
        }
        else {
            Thread[] newThreads = new Thread[count];
            System.arraycopy(threads, 0, newThreads, 0, count);
            return sort(newThreads);
        }
    }

    private Thread[] sort(Thread[] threads) {
        Comparator c = BeanComparator.forClass(Thread.class)
            .orderBy("threadGroup.name")
            .orderBy("name")
            .orderBy("priority");
        Arrays.sort(threads, c);
        return threads;
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, getTimeout(), null);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param timeout Milliseconds to wait for a thread to become
     * available. If zero, don't wait at all. If negative, wait forever.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, long timeout)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, timeout, null);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }


    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param name The name to give the thread.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, String name)
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, getTimeout(), name);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * Waits for a Thread to become available and starts a Runnable in it. 
     * If there are no available threads and the number of active threads is 
     * less than the maximum allowed, then a newly created thread is returned.
     *
     * @param target The Runnable instance that gets started by the returned
     * thread.
     * @param timeout Milliseconds to wait for a thread to become
     * @param name The name to give the thread.
     * available. If zero, don't wait at all. If negative, wait forever.
     * @exception NoThreadException If no thread could be obtained.
     * @exception InterruptedException If interrupted while waiting for a
     * thread to become available.
     * @return A Thread that has been started on the given Runnable.
     */
    public Thread start(Runnable target, long timeout, String name) 
        throws NoThreadException, InterruptedException
    {
        try {
            return start0(target, timeout, name);
        }
        catch (NoThreadException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    private Thread start0(Runnable target, long timeout, String name) 
        throws NoThreadException, InterruptedException
    {
        PooledThread thread;

        while (true) {
            synchronized (mPool) {
                closeCheck();

                // Obtain a thread from the pool if non-empty.
                if (mPool.size() > 0) {
                    thread = (PooledThread)mPool.removeLast();
                }
                else {
                    // Create a new thread if the number of active threads
                    // is less than the maximum allowed.
                    if (mActive < mMax) {
                        return startThread(target, name);
                    }
                    else {
                        break;
                    }
                }
            }

            if (name != null) {
                thread.setName(name);
            }
            
            if (thread.setTarget(target)) {
                return thread;
            }
            
            // Couldn't set the target because the pooled thread is exiting.
            // Wait for it to exit to ensure that the active count is less
            // than the maximum and try to obtain another thread.
            thread.join();
        }
        
        if (timeout == 0) {
            throw new NoThreadException("No thread available from " + this);
        }

        // Wait for a thread to become available in the pool.
        synchronized (mPool) {
            closeCheck();

            if (timeout < 0) {
                while (mPool.size() <= 0) {
                    mPool.wait(0);
                    closeCheck();
                }
            }
            else {
                long expireTime = System.currentTimeMillis() + timeout;
                while (mPool.size() <= 0) {
                    mPool.wait(timeout);
                    closeCheck();

                    // Thread could have been notified, but another thread may
                    // have stolen the thread away.
                    if (mPool.size() <= 0 &&
                        System.currentTimeMillis() > expireTime) {
                        
                        throw new NoThreadException
                            ("No thread available after waiting " + 
                             timeout + " milliseconds: " + this);
                    }
                }
            }
        
            thread = (PooledThread)mPool.removeLast();
            if (name != null) {
                thread.setName(name);
            }
        
            if (thread.setTarget(target)) {
                return thread;
            }
        }
        
        // Couldn't set the target because the pooled thread is exiting.
        // Wait for it to exit to ensure that the active count is less
        // than the maximum and create a new thread.
        thread.join();
        return startThread(target, name);
    }

    public boolean isClosed() {
        return mClosed;
    }

    /**
     * Will close down all the threads in the pool as they become
     * available. This method may block forever if any threads are
     * never returned to the thread pool.
     */
    public void close() throws InterruptedException {
        close(getTimeout());
    }

    /**
     * Will close down all the threads in the pool as they become 
     * available. If all the threads cannot become available within the
     * specified timeout, any active threads not yet returned to the
     * thread pool are interrupted.
     *
     * @param timeout Milliseconds to wait before unavailable threads
     * are interrupted. If zero, don't wait at all. If negative, wait forever.
     */
    public void close(long timeout) throws InterruptedException {
        synchronized (mPool) {
            mClosed = true;
            mPool.notifyAll();
            
            if (timeout != 0) {
                if (timeout < 0) {
                    while (mActive > 0) {
                        // Infinite wait for notification.
                        mPool.wait(0);
                    }
                }
                else {
                    long expireTime = System.currentTimeMillis() + timeout;
                    while (mActive > 0) {
                        mPool.wait(timeout);
                        if (System.currentTimeMillis() > expireTime) {
                            break;
                        }
                    }
                }
            }
        }

        interrupt();
    }

    private PooledThread startThread(Runnable target, String name) {
        PooledThread thread;

        synchronized (mPool) {
            mActive++;
            thread = new PooledThread(getName() + ' ' + nextThreadID());
            thread.setPriority(mPriority);
            thread.setDaemon(mDaemon);
            
            if (name != null) {
                thread.setName(name);
            }

            thread.setTarget(target);
            thread.start();
        }

        ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
        synchronized (mListeners) {
            for (Iterator it = mListeners.iterator(); it.hasNext();) {
                ((ThreadPoolListener)it.next()).threadStarted(event);
            }
        }

        return thread;
    }

    private void closeCheck() throws NoThreadException {
        if (mClosed) {
            throw new NoThreadException("Thread pool is closed", true);
        }
    }

    void threadAvailable(PooledThread thread) {
        synchronized (mPool) {
            if (thread.getPriority() != mPriority) {
                thread.setPriority(mPriority);
            }
            mPool.addLast(thread);
            mPool.notify();
        }
    }

    void threadExiting(PooledThread thread) {
        synchronized (mPool) {
            if (mPool.remove(thread)) {
                mActive--;
                
                ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
                synchronized (mListeners) {
                    for (Iterator it = mListeners.iterator(); it.hasNext();) {
                        ((ThreadPoolListener)it.next()).threadExiting(event);
                    }
                }
                
                mPool.notify();
            }
        }
    }

    private class PooledThread extends Thread {
        private String mOriginalName;
        private Runnable mTarget;
        private boolean mExiting;

        public PooledThread(String name) {
            super(ThreadPool.this, name);
            mOriginalName = name;
        }

        synchronized boolean setTarget(Runnable target) {
            if (mTarget != null) {
                throw new IllegalStateException
                    ("Target runnable in pooled thread is already set");
            }

            if (mExiting) {
                return false;
            }
            else {
                mTarget = target;
                notify();
                return true;
            }
        }

        private synchronized Runnable waitForTarget() {
            Runnable target;
            
            if ((target = mTarget) == null) {
                long idle = getIdleTimeout();
                
                if ((target = mTarget) == null) {
                    if (idle != 0) {
                        try {
                            if (idle < 0) {
                                wait(0);
                            }
                            else {
                                wait(idle);
                            }
                        }
                        catch (InterruptedException e) {
                        }
                    }
                    
                    if ((target = mTarget) == null) {
                        mExiting = true;
                    }
                }
            }

            return target;
        }

        public void run() {
            try {
                while (!isClosed()) {
                    if (Thread.interrupted()) {
                        continue;
                    }

                    Runnable target;

                    if ((target = waitForTarget()) == null) {
                        break;
                    }

                    try {
                        target.run();
                    }
                    catch (ThreadDeath death) {
                        break;
                    }
                    catch (Throwable e) {
                        uncaughtException(Thread.currentThread(), e);
                        e = null;
                    }

                    // Allow the garbage collector to reclaim target from
                    // stack while we wait for another target.
                    target = null;

                    mTarget = null;
                    setName(mOriginalName);
                    threadAvailable(this);
                }
            }
            finally {
                threadExiting(this);
            }
        }
    }
}
