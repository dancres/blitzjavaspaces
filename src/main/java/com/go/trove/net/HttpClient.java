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
import java.util.*;
import com.go.trove.io.*;

/******************************************************************************
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/06/14 <!-- $-->
 */
public class HttpClient {
    private final SocketFactory mFactory;
    private final int mReadTimeout;

    private String mMethod = "GET";
    private String mURI = "";
    private String mProtocol = "HTTP/1.0";
    private HttpHeaderMap mHeaders;

    private Object mSession;
    
    /**
     * Constructs a HttpClient with a read timeout that matches the given
     * factory's connect timeout.
     *
     * @param factory source of socket connections
     */
    public HttpClient(SocketFactory factory) {
        this(factory, factory.getDefaultTimeout());
    }

    /**
     * @param factory source of socket connections
     * @param readTimeout timeout on socket read operations before throwing a
     * InterruptedIOException
     */
    public HttpClient(SocketFactory factory, long readTimeout) {
        mFactory = factory;
        if (readTimeout == 0) {
            mReadTimeout = 1;
        }
        else if (readTimeout < 0) {
            mReadTimeout = 0;
        }
        else if (readTimeout > Integer.MAX_VALUE) {
            mReadTimeout = Integer.MAX_VALUE;
        }
        else {
            mReadTimeout = (int)readTimeout;
        }
    }

    /**
     * Set the HTTP request method, which defaults to "GET".
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setMethod(String method) {
        mMethod = method;
        return this;
    }

    /**
     * Set the URI to request, which can include a query string.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setURI(String uri) {
        mURI = uri;
        return this;
    }

    /**
     * Set the HTTP protocol string, which defaults to "HTTP/1.0". 
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setProtocol(String protocol) {
        mProtocol = protocol;
        return this;
    }

    /**
     * Set a header name-value pair to the request.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setHeader(String name, Object value) {
        if (mHeaders == null) {
            mHeaders = new HttpHeaderMap();
        }
        mHeaders.put(name, value);
        return this;
    }

    /**
     * Add a header name-value pair to the request in order for multiple values
     * to be specified.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient addHeader(String name, Object value) {
        if (mHeaders == null) {
            mHeaders = new HttpHeaderMap();
        }
        mHeaders.add(name, value);
        return this;
    }

    /**
     * Set all the headers for this request, replacing any existing headers.
     * If any more headers are added to this request, they will be stored in
     * the given HttpHeaderMap.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setHeaders(HttpHeaderMap headers) {
        mHeaders = headers;
        return this;
    }
    
    /**
     * Convenience method for setting the "Connection" header to "Keep-Alive"
     * or "Close".
     *
     * @param b true for persistent connection
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setPersistent(boolean b) {
        if (b) {
            setHeader("Connection", "Keep-Alive");
        }
        else {
            setHeader("Connection", "Close");
        }
        return this;
    }

    /**
     * Convenience method for preparing a post to the server. This method sets
     * the method to "POST", sets the "Content-Length" header, and sets the
     * "Content-Type" header to "application/x-www-form-urlencoded". When
     * calling getResponse, PostData must be provided.
     *
     * @param contentLength number of bytes to be posted
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient preparePost(int contentLength) {
        setMethod("POST");
        setHeader("Content-Type", "application/x-www-form-urlencoded");
        setHeader("Content-Length", new Integer(contentLength));
        return this;
    }

    /**
     * Optionally specify a session for getting connections. If SocketFactory
     * is distributed, then session helps to ensure the same server is routed
     * to on multiple requests.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setSession(Object session) {
        mSession = session;
        return this;
    }

    /**
     * Opens a connection, passes on the current request settings, and returns
     * the server's response.
     */
    public Response getResponse() throws ConnectException, SocketException {
        return getResponse(null);
    }

    /**
     * Opens a connection, passes on the current request settings, and returns
     * the server's response. The optional PostData parameter is used to
     * supply post data to the server. The Content-Length header specifies
     * how much data will be read from the PostData InputStream. If it is not
     * specified, data will be read from the InputStream until EOF is reached.
     *
     * @param postData additional data to supply to the server, if request
     * method is POST
     */
    public Response getResponse(PostData postData)
        throws ConnectException, SocketException
    {
        CheckedSocket socket = mFactory.getSocket(mSession);
        socket.setSoTimeout(mReadTimeout);

        try {
            CharToByteBuffer buffer = new FastCharToByteBuffer
                (new DefaultByteBuffer(), "8859_1");
            buffer = new InternedCharToByteBuffer(buffer);
            
            buffer.append(mMethod);
            buffer.append(' ');
            buffer.append(mURI);
            buffer.append(' ');
            buffer.append(mProtocol);
            buffer.append("\r\n");
            if (mHeaders != null) {
                mHeaders.appendTo(buffer);
            }
            buffer.append("\r\n");

            OutputStream out;
            InputStream in;

            out = new FastBufferedOutputStream(socket.getOutputStream());
            buffer.writeTo(out);
            if (postData != null) {
                writePostData(out, postData);
            }
            out.flush();
            in = new FastBufferedInputStream(socket.getInputStream());
            
            // Read first line to see if connection is working.
            char[] buf = new char[100];
            String line;
            try {
                line = HttpUtils.readLine(in, buf);
            }
            catch (IOException e) {
                line = null;
            }

            if (line == null) {
                // Try again with new connection.
                try {
                    socket.close();
                }
                catch (IOException e) {
                }

                socket = mFactory.createSocket(mSession);
                socket.setSoTimeout(mReadTimeout);

                out = new FastBufferedOutputStream(socket.getOutputStream());
                buffer.writeTo(out);
                if (postData != null) {
                    writePostData(out, postData);
                }
                out.flush();
                in = new FastBufferedInputStream(socket.getInputStream());

                // Read first line again.
                if ((line = HttpUtils.readLine(in, buf)) == null) {
                    throw new ConnectException("No response from server");
                }
            }

            return new Response(socket, mMethod, in, buf, line);
        }
        catch (SocketException e) {
            throw e;
        }
        catch (InterruptedIOException e) {
            throw new ConnectException("Read timeout expired: " +
                                       mReadTimeout + ", " + e);
        }
        catch (IOException e) {
            throw new SocketException(e.toString());
        }
    }

    private void writePostData(OutputStream out, PostData postData)
        throws IOException
    {
        InputStream in = postData.getInputStream();
        
        int contentLength = -1;
        if (mHeaders != null) {
            Integer i = mHeaders.getInteger("Content-Length");
            if (i != null) {
                contentLength = i.intValue();
            }
        }
        
        byte[] buf;
        if (contentLength < 0 || contentLength > 4000) {
            buf = new byte[4000];
        }
        else {
            buf = new byte[contentLength];
        }
        
        try {
            int len;
            if (contentLength < 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            else {
                while (contentLength > 0) {
                    len = buf.length;
                    if (contentLength < len) {
                        len = contentLength;
                    }
                    if ((len = in.read(buf, 0, len)) <= 0) {
                        break;
                    }
                    out.write(buf, 0, len);
                    contentLength -= len;
                }
            }
        }
        finally {
            in.close();
        }
    }

    /**
     * A factory for supplying data to be written to server in a POST request.
     */
    public static interface PostData {
        /**
         * Returns the actual data via an InputStream. If the client needs to
         * reconnect to the server, this method may be called again. The
         * InputStream is closed when all the post data has been read from it.
         */
        public InputStream getInputStream() throws IOException;
    }

    public class Response {
        private final int mStatusCode;
        private final String mStatusMessage;
        private final HttpHeaderMap mHeaders;

        private InputStream mIn;

        Response(CheckedSocket socket, String method,
                 InputStream in, char[] buf, String line) throws IOException
        {
            int statusCode = -1;
            String statusMessage = "";

            int space = line.indexOf(' ');
            if (space > 0) {
                int nextSpace = line.indexOf(' ', space + 1);
                String sub;
                if (nextSpace < 0) {
                    sub = line.substring(space + 1);
                }
                else {
                    sub = line.substring(space + 1, nextSpace);
                    statusMessage = line.substring(nextSpace + 1);
                }
                try {
                    statusCode = Integer.parseInt(sub);
                }
                catch (NumberFormatException e) {
                }
            }

            if (statusCode < 0) {
                throw new ProtocolException("Invalid HTTP response: " + line);
            }

            mStatusCode = statusCode;
            mStatusMessage = statusMessage;
            mHeaders = new HttpHeaderMap();
            mHeaders.readFrom(in, buf);

            // Used for controlling persistent connections.
            int contentLength;
            if ("Keep-Alive".equalsIgnoreCase
                (mHeaders.getString("Connection"))) {

                if ("HEAD".equals(method)) {
                    contentLength = 0;
                }
                else {
                    Integer i = mHeaders.getInteger("Content-Length");
                    if (i != null) {
                        contentLength = i.intValue();
                    }
                    else {
                        contentLength = -1;
                    }
                }
            }
            else {
                contentLength = -1;
            }

            mIn = new ResponseInput(socket, in, contentLength);
        }

        /**
         * Resturns the server's status code, 200 for OK, 404 for not found,
         * etc.
         */
        public int getStatusCode() {
            return mStatusCode;
        }

        /**
         * Returns the server's status message accompanying the status code.
         * This message is intended for humans only.
         */
        public String getStatusMessage() {
            return mStatusMessage;
        }

        public HttpHeaderMap getHeaders() {
            return mHeaders;
        }

        /**
         * Returns an InputStream supplying the body of the response. When all
         * of the response body has been read, the connection is either closed
         * or recycled, depending on if all the criteria is met for supporting
         * persistent connections. Further reads on the InputStream will
         * return EOF.
         */
        public InputStream getInputStream() {
            return mIn;
        }
    }

    private class ResponseInput extends InputStream {
        private CheckedSocket mSocket;
        private InputStream mIn;
        private int mContentLength;
        
        /**
         * @param contentLength Used for supporting persistent connections. If
         * negative, then close connection when EOF is read.
         */
        public ResponseInput(CheckedSocket socket,
                             InputStream in, int contentLength)
            throws IOException
        {
            mSocket = socket;
            mIn = in;
            if ((mContentLength = contentLength) == 0) {
                recycle();
            }
        }

        public int read() throws IOException {
            if (mContentLength == 0) {
                return -1;
            }

            int b = mIn.read();

            if (b < 0) {
                close();
            }
            else if (mContentLength > 0) {
                if (--mContentLength == 0) {
                    recycle();
                }
            }

            return b;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (mContentLength == 0) {
                return -1;
            }

            if (mContentLength < 0) {
                len = mIn.read(b, off, len);
                if (len < 0) {
                    close();
                }
                else if (len == 0) {
                    close();
                    len = -1;
                }
                return len;
            }

            if (len > mContentLength) {
                len = mContentLength;
            }
            else if (len == 0) {
                return 0;
            }

            len = mIn.read(b, off, len);

            if (len < 0) {
                close();
            }
            else if (len == 0) {
                close();
                len = -1;
            }
            else {
                if ((mContentLength -= len) == 0) {
                    recycle();
                }
            }

            return len;
        }

        public long skip(long n) throws IOException {
            if (mContentLength == 0) {
                return 0;
            }

            if (mContentLength < 0) {
                return mIn.skip(n);
            }

            if (n > mContentLength) {
                n = mContentLength;
            }
            else if (n == 0) {
                return 0;
            }

            n = mIn.skip(n);

            if ((mContentLength -= n) == 0) {
                recycle();
            }

            return n;
        }

        public int available() throws IOException {
            return mIn.available();
        }

        public void close() throws IOException {
            if (mSocket != null) {
                mContentLength = 0;
                mSocket = null;
                mIn.close();
            }
        }

        private void recycle() throws IOException {
            if (mSocket != null) {
                if (mContentLength == 0) {
                    CheckedSocket s = mSocket;
                    mSocket = null;
                    mFactory.recycleSocket(s);
                }
                else {
                    mSocket = null;
                    mIn.close();
                }
            }
        }
    }
}
