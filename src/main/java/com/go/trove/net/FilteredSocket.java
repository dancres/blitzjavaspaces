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
import java.io.*;

/******************************************************************************
 * A Socket wrapper that passes all calls to an internal Socket. This class is
 * designed for subclasses to override or hook into the behavior of a Socket
 * instance.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 00/12/05 <!-- $-->
 */
public class FilteredSocket extends Socket {
    protected final Socket mSocket;

    public FilteredSocket(Socket socket) throws SocketException {
        super(new Impl(socket));
        mSocket = socket;
    }

    // Implementation note: Java's socket API could use some improvement.
    // For example, java.net.Socket should be an interface. This would avoid
    // needing the ugly SocketImpl class provided instead.

    private static class Impl extends SocketImpl {
        private Socket mSocket;

        public Impl(Socket socket) {
            if (socket == null) {
                throw new IllegalArgumentException("Socket is null");
            }
            mSocket = socket;
        }

        public void setOption(int optId, Object value) throws SocketException {
            switch (optId) {
            case TCP_NODELAY:
                mSocket.setTcpNoDelay(((Boolean)value).booleanValue());
                return;
            case SO_LINGER:
                if (value instanceof Boolean) {
                    mSocket.setSoLinger(((Boolean)value).booleanValue(), 0);
                }
                else {
                    mSocket.setSoLinger(true, ((Integer)value).intValue());
                }
                return;
            case SO_TIMEOUT:
                mSocket.setSoTimeout(((Integer)value).intValue());
                return;
            case SO_SNDBUF:
                mSocket.setSendBufferSize(((Integer)value).intValue());
                return;
            case SO_RCVBUF:
                mSocket.setReceiveBufferSize(((Integer)value).intValue());
                return;
            case SO_BINDADDR:
            case SO_REUSEADDR:
            case IP_MULTICAST_IF:
            default:
                throw new SocketException("Invalid option: " + optId);
            }
        }

        public Object getOption(int optId) throws SocketException {
            switch (optId) {
            case TCP_NODELAY:
                return mSocket.getTcpNoDelay() ? Boolean.TRUE : Boolean.FALSE;
            case SO_BINDADDR:
                return mSocket.getLocalAddress();
            case SO_LINGER:
                return new Integer(mSocket.getSoLinger());
            case SO_TIMEOUT:
                return new Integer(mSocket.getSoTimeout());
            case SO_SNDBUF:
                return new Integer(mSocket.getSendBufferSize());
            case SO_RCVBUF:
                return new Integer(mSocket.getReceiveBufferSize());
            case SO_REUSEADDR:
            case IP_MULTICAST_IF:
            default:
                throw new SocketException("Invalid option: " + optId);
            }
        }

        protected void create(boolean stream) throws IOException {
            error();
        }

        protected void connect(String host, int port) throws IOException {
            error();
        }

        protected void connect(InetAddress host, int port) throws IOException {
            error();
        }

        protected void connect(SocketAddress host,
                               int port) throws IOException {
            error();
        }

        protected void bind(InetAddress host, int port) throws IOException {
            error();
        }

        protected void listen(int backlog) throws IOException {
            error();
        }

        protected void accept(SocketImpl s) throws IOException {
            error();
        }

        protected InputStream getInputStream() throws IOException {
            return mSocket.getInputStream();
        }

        protected OutputStream getOutputStream() throws IOException {
            return mSocket.getOutputStream();
        }

        protected int available() throws IOException {
            return getInputStream().available();
        }

        protected void close() throws IOException {
            mSocket.close();
        }

        protected InetAddress getInetAddress() {
            return mSocket.getInetAddress();
        }

        protected int getPort() {
            return mSocket.getPort();
        }

        protected int getLocalPort() {
            return mSocket.getLocalPort();
        }

        private void error() throws IOException {
            throw new IOException("Socket already connected");
        }

        protected void sendUrgentData(int aByte) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
