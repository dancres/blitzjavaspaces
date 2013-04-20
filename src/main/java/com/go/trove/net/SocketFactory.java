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
 * Allows client sockets to be created or recycled.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/05 <!-- $-->
 */
public interface SocketFactory {
    /**
     * Returns the InetAddress and port that this factory will most likely
     * connect to. If the address isn't precisely known, its value is 0.0.0.0.
     * If the port isn't known, its value is -1.
     */
    public InetAddressAndPort getInetAddressAndPort();

    /**
     * Returns the InetAddress and port that this factory will most likely
     * connect to. If the address isn't precisely known, its value is 0.0.0.0.
     * If the port isn't known, its value is -1.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     */
    public InetAddressAndPort getInetAddressAndPort(Object session);
    
    /**
     * Returns the default timeout for creating or getting sockets or -1 if
     * infinite.
     */
    public long getDefaultTimeout();

    /**
     * Must always return a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket()
        throws ConnectException, SocketException;

    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(Object session)
        throws ConnectException, SocketException;

    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be created before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(long timeout)
        throws ConnectException, SocketException;
    
    /**
     * Returns a new socket connection. When the socket is no longer
     * needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to become available before throwing an exception
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket createSocket(Object session, long timeout)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket() throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(Object session)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be returned before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(long timeout)
        throws ConnectException, SocketException;

    /**
     * Returns a new or recycled socket connection. When the socket is no
     * longer needed, call {@link recycleSocket} so that it be used again.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @param timeout Maximum time to wait (in milliseconds) for a connection
     * to be returned before throwing a ConnectException
     * @throws ConnectException if timeout has elapsed and no socket is
     * available or factory is closed
     */
    public CheckedSocket getSocket(Object session, long timeout)
        throws ConnectException, SocketException;

    /**
     * Recycle a socket connection that was returned from the {@link getSocket}
     * or {@link createSocket} methods. Since SocketFactory has no knowledge of
     * any protocol being used on the socket, it is the responsibility of the
     * caller to ensure the socket is in a "clean" state. Depending on
     * implementation, the recycled socket may simply be closed.
     *
     * @param socket Socket which must have come from this factory. Passing in
     * null is ignored.
     */
    public void recycleSocket(CheckedSocket socket)
        throws SocketException, IllegalArgumentException;

    /**
     * Closes all recycled connections, but does not prevent new connections
     * from being created and recycled.
     */
    public void clear();

    /**
     * Returns the number of recycled sockets currently available.
     */    
    public int getAvailableCount();
}
