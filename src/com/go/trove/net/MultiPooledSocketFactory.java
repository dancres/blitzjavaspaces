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

import java.net.*;
import java.util.*;

/******************************************************************************
 * Pooled SocketFactory implementation that connects to multiple hosts that may
 * resolve to multiple InetAddresses. If running under Java 2, version 1.3,
 * changes in the address resolution are automatically detected using
 * {@link InetAddressResolver}.
 * <p>
 * Consider wrapping with a {@link LazySocketFactory} for automatic checking
 * against socket factories that may be dead.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/12 <!-- $-->
 */
public class MultiPooledSocketFactory extends DistributedSocketFactory {
    // Just references InetAddressResolvers to keep them from going away.
    private Object[] mResolvers;

    /**
     * @param hosts hosts to connect to; length matches ports
     * @param ports ports to connect to; length matches hosts
     * @param timeout Maximum time to wait (in milliseconds) for new
     * connections to be established before throwing an exception
     */
    public MultiPooledSocketFactory(String[] hosts,
                                    int[] ports,
                                    long timeout) {
        super(timeout);

        try {
            mResolvers = new InetAddressResolver[hosts.length];
            for (int i=0; i<hosts.length; i++) {
                Listener listener = new Listener(ports[i], timeout);
                mResolvers[i] =
                    InetAddressResolver.listenFor(hosts[i], listener);
            }
        }
        catch (NoClassDefFoundError e) {
            // Timer class probably wasn't found, so InetAddressResolver
            // cannot be used.
            mResolvers = null;
            for (int i=0; i<hosts.length; i++) {
                Listener listener = new Listener(ports[i], timeout);
                try {
                    listener.resolved(InetAddress.getAllByName(hosts[i]));
                }
                catch (UnknownHostException e2) {
                    listener.unknown(e2);
                }
            }
        }
    }

    /**
     * Create socket factories for newly resolved addresses. Default
     * implementation returns a LazySocketFactory wrapping a
     * PooledSocketFactory wrapping a PlainSocketFactory.
     */
    protected SocketFactory createSocketFactory(InetAddress address,
                                                int port, long timeout)
    {
        SocketFactory factory;
        factory = new PlainSocketFactory(address, port, timeout);
        factory = new PooledSocketFactory(factory);
        factory = new LazySocketFactory(factory);
        return factory;
    }

    private class Listener implements InetAddressListener {
        private final int mPort;
        private final long mTimeout;

        // Maps InetAddress objects to SocketFactories.
        private Map mAddressedFactories;
    
        public Listener(int port, long timeout) {
            mPort = port;
            mTimeout = timeout;
            mAddressedFactories = new HashMap();
        }

        public void unknown(UnknownHostException e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);

            // Remove all the addressed factories.
            Iterator it = mAddressedFactories.keySet().iterator();
            while (it.hasNext()) {
                SocketFactory factory = 
                    (SocketFactory)mAddressedFactories.get(it.next());
                removeSocketFactory(factory);
            }
        }
        
        public void resolved(InetAddress[] addresses) {
            // Add newly discovered addresses.
            for (int i=0; i<addresses.length; i++) {
                InetAddress address = addresses[i];
                if (!mAddressedFactories.containsKey(address)) {
                    SocketFactory factory =
                        createSocketFactory(address, mPort, mTimeout);
                    mAddressedFactories.put(address, factory);
                    addSocketFactory(factory);
                }
            }
            
            // Remove addresses no longer being routed to.
            Iterator it = mAddressedFactories.keySet().iterator();
        mainLoop:
            while (it.hasNext()) {
                InetAddress address = (InetAddress)it.next();
                for (int i=0; i<addresses.length; i++) {
                    if (addresses[i].equals(address)) {
                        continue mainLoop;
                    }
                }
                SocketFactory factory =
                    (SocketFactory)mAddressedFactories.get(address);
                removeSocketFactory(factory);
            }
        }
    }
}
