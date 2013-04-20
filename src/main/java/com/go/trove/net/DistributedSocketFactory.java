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

package com.go.trove.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.ref.*;
import com.go.trove.util.IdentityMap;

/******************************************************************************
 * A SocketFactory implementation for distributing load among several
 * SocketFactories. If an exception occurs on a socket, its pool is put into
 * the "dead" list. A special thread will run in the background, trying to
 * resurrect the dead SocketSocket. As soon as its able to create sockets
 * again, its added back into the "live" list.
 * <p>
 * Consider wrapping with a {@link LazySocketFactory} for automatic checking
 * against socket factories that may be dead.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/01/22 <!-- $-->
 */
public class DistributedSocketFactory implements SocketFactory {
    private final long mTimeout;

    private int mFactoryIndex;

    // Contains only the live SocketFactories.
    private List mFactories;

    // Maps SocketPools to resurrector threads.
    private Map mResurrectors;

    // Maps CheckedSockets to the SocketPools that they came from.
    private Map mSocketSources;

    private CheckedSocket.ExceptionListener mListener;

    /**
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public DistributedSocketFactory(long timeout) {
        mTimeout = timeout;
        mFactories = Collections.synchronizedList(new ArrayList());
        mResurrectors = Collections.synchronizedMap(new HashMap());
        mSocketSources = Collections.synchronizedMap(new IdentityMap());

        mListener = new CheckedSocket.ExceptionListener() {
            public void exceptionOccurred(CheckedSocket s, Exception e, int count) {
                if (count == 1) {
                    deadFactory((SocketFactory)mSocketSources.get(s));
                }
            }
        };
    }

    public void addSocketFactory(SocketFactory factory) {
        mFactories.add(factory);
    }

    public void removeSocketFactory(SocketFactory factory) {
        mFactories.remove(factory);
        Thread t = (Thread)mResurrectors.remove(factory);
        if (t != null) {
            t.interrupt();
        }
    }

    public InetAddressAndPort getInetAddressAndPort() {
        try {
            return getFactory(selectFactory(null)).getInetAddressAndPort();
        }
        catch (ConnectException e) {
            return InetAddressAndPort.UNKNOWN;
        }
    }

    public InetAddressAndPort getInetAddressAndPort(Object session) {
        try {
            return getFactory(selectFactory(session))
                .getInetAddressAndPort(session);
        }
        catch (ConnectException e) {
            return InetAddressAndPort.UNKNOWN;
        }
    }

    public long getDefaultTimeout() {
        return mTimeout;
    }

    public CheckedSocket createSocket()
        throws ConnectException, SocketException
    {
        return createSocket(null, mTimeout);
    }

    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException
    {
        return createSocket(session, mTimeout);
    }

    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        return createSocket(null, timeout);
    }

    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        long startTime = timeout > 0 ? System.currentTimeMillis() : 0;
        int index = selectFactory(session);
        int count = mFactories.size();

        for (int i=0; i<count; i++) {
            SocketFactory factory = null;
            try {
                factory = getFactory(index++);
                CheckedSocket socket = factory.createSocket(session, timeout);
                socket.addExceptionListener(mListener);
                mSocketSources.put(socket, factory);
                return socket;
            }
            catch (SocketException e) {
                deadFactory(factory);
                
                if (timeout == 0) {
                    throw e;
                }
                
                if (timeout > 0) {
                    timeout -= (System.currentTimeMillis() - startTime);
                    if (timeout < 0) {
                        throw e;
                    }
                }
            }
        }

        throw new ConnectException("Unable to create socket");
    }

    public CheckedSocket getSocket() throws ConnectException, SocketException {
        return getSocket(null, mTimeout);
    }

    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException
    {
        return getSocket(session, mTimeout);
    }

    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException
    {
        return getSocket(null, timeout);
    }

    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        long startTime = timeout > 0 ? System.currentTimeMillis() : 0;
        int index = selectFactory(session);
        int count = mFactories.size();

        for (int i=0; i<count; i++) {
            SocketFactory factory = null;
            try {
                factory = getFactory(index++);
                CheckedSocket socket = factory.getSocket(session, timeout);
                socket.addExceptionListener(mListener);
                mSocketSources.put(socket, factory);
                return socket;
            }
            catch (SocketException e) {
                deadFactory(factory);
                
                if (timeout == 0) {
                    throw e;
                }
                
                if (timeout > 0) {
                    timeout -= (System.currentTimeMillis() - startTime);
                    if (timeout < 0) {
                        throw e;
                    }
                }
            }
        }

        throw new ConnectException("Unable to get socket");
    }

    public void recycleSocket(CheckedSocket socket)
        throws SocketException, IllegalArgumentException
    {
        if (socket == null) {
            return;
        }

        SocketFactory source = (SocketFactory)mSocketSources.remove(socket);

        if (source == null) {
            throw new IllegalArgumentException
                ("Socket did not originate from this pool");
        }

        socket.removeExceptionListener(mListener);
        source.recycleSocket(socket);
    }

    public void clear() {
        synchronized (mFactories) {
            for (int i = mFactories.size(); --i >= 0; ) {
                ((SocketFactory)mFactories.get(i)).clear();
            }
        }
    }

    public int getAvailableCount() {
        int count = 0;
        synchronized (mFactories) {
            for (int i = mFactories.size(); --i >= 0; ) {
                count += ((SocketFactory)mFactories.get(i))
                    .getAvailableCount();
            }
        }
        return count;
    }

    /**
     * The provided index must be positive, but it can be out of the factory
     * list bounds.
     */
    private SocketFactory getFactory(int index) throws ConnectException {
        synchronized (mFactories) {
            int size = mFactories.size();
            if (size <= 0) {
                throw new ConnectException("No SocketFactories available");
            }
            return (SocketFactory)mFactories.get(index % size);
        }
    }

    /**
     * Returns an index which is positive, but may be out of the factory list
     * bounds.
     */
    private int selectFactory(Object session) throws ConnectException {
        if (session != null) {
            return session.hashCode() & 0x7fffffff;
        }
        else {
            synchronized (mFactories) {
                return mFactoryIndex++ & 0x7fffffff;
            }
        }
    }

    private void deadFactory(SocketFactory factory) {
        if (factory == null) {
            return;
        }

        synchronized (mFactories) {
            // Only remove factory if its not the last one left.
            if (mFactories.contains(factory) && mFactories.size() > 1) {
                mFactories.remove(factory);
                
                Resurrector r = new Resurrector(this, factory);
                Thread t = new Thread(null, r, "Resurrector " +
                                      factory.getInetAddressAndPort());
                t.setDaemon(true);
                t.start();
                mResurrectors.put(factory, t);
            }
        }
    }

    private static class Resurrector implements Runnable {
        // Weakly references owner so that this thread won't prevent it from
        // being garbage collected.
        private final Reference mOwner;
        private final SocketFactory mFactory;

        public Resurrector(DistributedSocketFactory owner,
                           SocketFactory factory) {
            mOwner = new WeakReference(owner);
            mFactory = factory;
        }

        public void run() {
            DistributedSocketFactory owner = null;
            try {
                while (!Thread.interrupted()) {
                    owner = (DistributedSocketFactory)mOwner.get();
                    if (owner == null) {
                        break;
                    }

                    try {
                        mFactory.recycleSocket(mFactory.createSocket());
                        owner.mFactories.add(mFactory);
                        break;
                    }
                    catch (IOException e) {
                    }
                    
                    owner = null;

                    // Wait at 5 seconds before trying again.
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
            }
            finally {
                owner = (DistributedSocketFactory)mOwner.get();
                if (owner != null) {
                    owner.mResurrectors.remove(mFactory);
                }
            }
        }
    }
}
