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
import com.go.trove.util.*;

/******************************************************************************
 * Allows client socket connections to be established with a timeout.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/05 <!-- $-->
 */
public class SocketConnector {
    // Limit the number of threads that may simultaneously connect to a
    // specific destination.
    private static final int CONNECT_THREAD_MAX = 5;

    // Maps address:port pairs to ThreadPools for connecting.
    private static Map mConnectors =
        Collections.synchronizedMap(new SoftHashMap());

    /**
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(String host, int port, long timeout)
        throws SocketException
    {
        return connect((Object)host, port, timeout);
    }


    /**
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time.
     */
    public static Socket connect(InetAddress address, int port, long timeout)
        throws SocketException
    {
        return connect((Object)address, port, timeout);
    }

    /**
     * @param address either a string or InetAddress.
     * @param timeout Max time to wait for new connection. If negative, wait
     * is infinite.
     * @return null if couldn't connect in time
     */
    private static Socket connect(Object address, int port, long timeout)
        throws SocketException
    {
        Key key = new Key(address, port);
        ThreadPool pool;
        synchronized (mConnectors) {
            pool = (ThreadPool)mConnectors.get(key);
            if (pool == null) {
                pool = new ThreadPool
                    ("SocketConnector[" + key + ']', CONNECT_THREAD_MAX);
                pool.setIdleTimeout(10000);
                mConnectors.put(key, pool);
            }
        }

        Connector connector = new Connector(key);
        Thread thread;

        long start;
        if (timeout > 0) {
            start = System.currentTimeMillis();
        }
        else {
            start = 0;
        }

        try {
            thread = pool.start(connector, timeout);
        }
        catch (InterruptedException e) {
            return null;
        }

        if (timeout > 0) {
            timeout = timeout - (System.currentTimeMillis() - start);
            if (timeout < 0) {
                timeout = 0;
            }
        }

        try {
            Socket socket = connector.connect(timeout);
            if (socket != null) {
                return socket;
            }
        }
        catch (InterruptedException e) {
        }

        thread.interrupt();
        return null;
    }

    private SocketConnector() {
    }

    private static class Key {
        final Object mAddress;
        final int mPort;

        Key(Object address, int port) {
            mAddress = address;
            mPort = port;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key key = (Key)obj;
                return key.mAddress.equals(mAddress) && key.mPort == mPort;
            }
            return false;
        }

        public int hashCode() {
            return mAddress.hashCode() + mPort;
        }

        public String toString() {
            if (mAddress instanceof InetAddress) {
                return ((InetAddress)mAddress).getHostAddress() + ':' + mPort;
            }
            else {
                return String.valueOf(mAddress) + ':' + mPort;
            }
        }
    }

    private static class Connector implements Runnable {
        private final Key mKey;
        private Object mSocketOrException;
        private boolean mDoneWaiting;

        public Connector(Key key) {
            mKey = key;
        }

        public synchronized Socket connect(long timeout)
            throws SocketException, InterruptedException
        {
            try {
                if (mSocketOrException == null) {
                    if (timeout < 0) {
                        wait();
                    }
                    else if (timeout > 0) {
                        wait(timeout);
                    }
                    else {
                        return null;
                    }
                }
            }
            finally {
                mDoneWaiting = true;
            }

            if (mSocketOrException instanceof Socket) {
                return (Socket)mSocketOrException;
            }
            else if (mSocketOrException instanceof InterruptedIOException) {
                throw new InterruptedException();
            }
            else if (mSocketOrException instanceof Exception) {
                throw new SocketException
                    ("Unable to connect to " + mKey + ", " +
                     ((Exception)mSocketOrException).getMessage());
            }

            return null;
        }

        public void run() {
            try {
                Socket socket;
                Object address = mKey.mAddress;
                if (address instanceof InetAddress) {
                    socket = new Socket((InetAddress)address, mKey.mPort);
                }
                else {
                    socket = new Socket(String.valueOf(address), mKey.mPort);
                }

                synchronized (this) {
                    if (mDoneWaiting) {
                        try {
                            socket.close();
                        }
                        catch (IOException e) {
                        }
                    }
                    else {
                        mSocketOrException = socket;
                        notify();
                    }
                }
            }
            catch (Exception e) {
                synchronized (this) {
                    mSocketOrException = e;
                    notify();
                }
            }
        }
    }
}
