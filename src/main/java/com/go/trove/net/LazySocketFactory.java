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

/******************************************************************************
 * A socket implementation that lazily establishs a connection. It only
 * connects when actually needed. Setting options and getting I/O streams will
 * not force a connection to be established. As soon as a read or write
 * operation is performed, a connection is established.
 * <p>
 * If the first write operation requires a connection to be established, then a
 * recycled connection is requested. The connection is tested by writing the
 * data to it. If this fails, a new connection is requested and the operation
 * is tried again.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/05 <!-- $-->
 */
public class LazySocketFactory implements SocketFactory {
    private final SocketFactory mFactory;

    public LazySocketFactory(SocketFactory factory) {
        mFactory = factory;
    }

    public InetAddressAndPort getInetAddressAndPort() {
        return mFactory.getInetAddressAndPort();
    }

    public InetAddressAndPort getInetAddressAndPort(Object session) {
        return mFactory.getInetAddressAndPort(session);
    }
    
    public long getDefaultTimeout() {
        return mFactory.getDefaultTimeout();
    }

    public CheckedSocket createSocket()
        throws ConnectException, SocketException
    {
        return mFactory.createSocket();
    }

    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(session);
    }

    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(timeout);
    }

    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return mFactory.createSocket(session, timeout);
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket() throws ConnectException, SocketException {
        return CheckedSocket.check(new LazySocket(mFactory));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, session));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, timeout));
    }

    /**
     * Returns a socket that will lazily connect.
     */    
    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException
    {
        return CheckedSocket.check(new LazySocket(mFactory, session, timeout));
    }

    public void recycleSocket(CheckedSocket cs)
        throws SocketException, IllegalArgumentException
    {
        if (cs == null) {
            return;
        }

        // Bust through two layers of wrapping to get at actual socket.

        Socket s = cs.mSocket;
        if (s instanceof LazySocket) {
            cs = ((LazySocket)s).recycle();
            if (cs == null) {
                return;
            }
        }

        mFactory.recycleSocket(cs);
    }

    public void clear() {
        mFactory.clear();
    }

    public int getAvailableCount(){
        return mFactory.getAvailableCount();
    }
}
