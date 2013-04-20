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

/******************************************************************************
 * A socket that tracks if any I/O exceptions have occured and ensures that
 * plain I/O exceptions are thrown as SocketExceptions. InterruptedIOExceptions
 * do not affect the exception count, and they are not converted to
 * SocketExceptions.
 * <p>
 * All socket exceptions thrown will actually be instances of
 * {@link CheckedSocketException}, which subclasses SocketException. The error
 * messages will contain additional information, and the original exception
 * and socket can be obtained from it.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/03/16 <!-- $-->
 */
public class CheckedSocket extends FilteredSocket {
    /**
     * Returns a new CheckedSocket instance only if the given socket isn't
     * already one.
     */
    public static CheckedSocket check(Socket socket) throws SocketException {
        if (socket instanceof CheckedSocket) {
            return (CheckedSocket)socket;
        }
        else {
            return new CheckedSocket(socket);
        }
    }

    private int mExceptionCount;
    private InputStream mIn;
    private OutputStream mOut;

    private Set mExceptionListeners;

    protected CheckedSocket(Socket socket) throws SocketException {
        super(socket);
    }

    /**
     * Returns the total number of exceptions encountered while using this
     * socket, excluding InterruptedIOExceptions. If this count is not zero,
     * then the socket is potentially in an unrecoverable state.
     */
    public int getExceptionCount() {
        return mExceptionCount;
    }

    /**
     * Internally, the collection of listeners is saved in a set so that
     * listener instances may be added multiple times without harm.
     */
    public synchronized void addExceptionListener(ExceptionListener listener) {
        if (mExceptionListeners == null) {
            mExceptionListeners = new HashSet();
        }
        mExceptionListeners.add(listener);
    }

    public synchronized void removeExceptionListener(ExceptionListener listener) {
        if (mExceptionListeners != null) {
            mExceptionListeners.remove(listener);
        }
    }
    
    public synchronized InputStream getInputStream() throws IOException {
        if (mIn != null) {
            return mIn;
        }
        
        try {
            return mIn = new Input(super.getInputStream());
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        if (mOut != null) {
            return mOut;
        }
        
        try {
            return mOut = new Output(super.getOutputStream());
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        try {
            super.setTcpNoDelay(on);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        try {
            return super.getTcpNoDelay();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        try {
            super.setSoLinger(on, linger);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSoLinger() throws SocketException {
        try {
            return super.getSoLinger();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSoTimeout(int timeout) throws SocketException {
        try {
            super.setSoTimeout(timeout);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSoTimeout() throws SocketException {
        try {
            return super.getSoTimeout();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setSendBufferSize(int size) throws SocketException {
        try {
            super.setSendBufferSize(size);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getSendBufferSize() throws SocketException {
        try {
            return super.getSendBufferSize();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        try {
            super.setReceiveBufferSize(size);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public int getReceiveBufferSize() throws SocketException {
        try {
            return super.getReceiveBufferSize();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void close() throws IOException {
        try {
            super.close();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void setKeepAlive(boolean on) throws SocketException {
        try {
            super.setKeepAlive(on);
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public boolean getKeepAlive() throws SocketException {
        try {
            return super.getKeepAlive();
        }
        catch (Exception e) {
            throw handleSocketException(e);
        }
    }

    public void shutdownInput() throws IOException {
        try {
            super.shutdownInput();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    public void shutdownOutput() throws IOException {
        try {
            super.shutdownOutput();
        }
        catch (Exception e) {
            throw handleIOException(e);
        }
    }

    /**
     * @param e should be instance of IOException or RuntimeException.
     */
    IOException handleIOException(Exception e) {
        if (e instanceof InterruptedIOException) {
            return CheckedInterruptedIOException.create
                ((InterruptedIOException)e, mSocket);
        }

        int count;
        synchronized (this) {
            count = ++mExceptionCount;
        }
        exceptionOccurred(e, count);

        if (e instanceof CheckedSocketException) {
            return (CheckedSocketException)e;
        }
        else if (e instanceof NullPointerException) {
            // Workaround for a bug in the Socket class that sometimes
            // causes a NullPointerException on a closed socket.
            return CheckedSocketException.create(e, mSocket, "Socket closed");
        }
        else {
            return CheckedSocketException.create(e, mSocket);
        }
    }

    /**
     * @param e Should be instance of SocketException or RuntimeException.
     */
    SocketException handleSocketException(Exception e) {
        int count;
        synchronized (this) {
            count = ++mExceptionCount;
        }
        exceptionOccurred(e, count);

        if (e instanceof CheckedSocketException) {
            return (CheckedSocketException)e;
        }
        else if (e instanceof NullPointerException) {
            // Workaround for a bug in the Socket class that sometimes
            // causes a NullPointerException on a closed socket.
            return CheckedSocketException.create(e, mSocket, "Socket closed");
        }
        else {
            return CheckedSocketException.create(e, mSocket);
        }
    }

    private synchronized void exceptionOccurred(Exception e, int count) {
        if (mExceptionListeners != null) {
            Iterator it = mExceptionListeners.iterator();
            while (it.hasNext()) {
                ((ExceptionListener)it.next())
                    .exceptionOccurred(this, e, count);
            }
        }
    }

    public static interface ExceptionListener {
        /**
         * @param count new exception count, which will be one the first time
         * this method is called.
         */
        public void exceptionOccurred(CheckedSocket s, Exception e, int count);
    }

    private class Input extends InputStream {
        private final InputStream mStream;

        public Input(InputStream in) {
            mStream = in;
        }

        public int read() throws IOException {
            try {
                return mStream.read();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int read(byte[] b) throws IOException {
            try {
                return mStream.read(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return mStream.read(b, off, len);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public long skip(long n) throws IOException {
            try {
                return mStream.skip(n);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public int available() throws IOException {
            try {
                return mStream.available();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void close() throws IOException {
            try {
                mStream.close();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void mark(int readlimit) {
            mStream.mark(readlimit);
        }

        public void reset() throws IOException {
            try {
                mStream.reset();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }

        public boolean markSupported() {
            return mStream.markSupported();
        }
    }

    private class Output extends OutputStream {
        private final OutputStream mStream;

        public Output(OutputStream out) {
            mStream = out;
        }

        public void write(int b) throws IOException {
            try {
                mStream.write(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void write(byte[] b) throws IOException {
            try {
                mStream.write(b);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                mStream.write(b, off, len);
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void flush() throws IOException {
            try {
                mStream.flush();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
        
        public void close() throws IOException {
            try {
                mStream.close();
            }
            catch (IOException e) {
                throw handleIOException(e);
            }
        }
    }
}
