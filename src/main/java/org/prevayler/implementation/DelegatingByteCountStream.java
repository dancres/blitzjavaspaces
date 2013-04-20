/*
  The copyright of all source code included in this Prevayler distribution is
  held by Klaus Wuestefeld, except the files that specifically state otherwise.
  All rights are reserved. "PREVAYLER" is a trademark of Klaus Wuestefeld.


  BSD License:

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
  - Neither the name of Prevayler nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.


  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
*/

package org.prevayler.implementation;

import java.io.*;

/**
   A FileOutputStream that counts the number of bytes written and forces all
   buffers to synchronize with the underlying device when flushed.
*/
final class DelegatingByteCountStream extends OutputStream {
    private OutputStream theBuffer;
    private FileOutputStream theFile;


    DelegatingByteCountStream(File file, int aBufferSize)
        throws IOException {

        // System.err.println("Buffer size: " + aBufferSize);

        System.err.println("Open: " + file);

        theFile = new FileOutputStream(file);

        if (aBufferSize > 0)
            theBuffer = new BufferedOutputStream(theFile, aBufferSize);
        else
            theBuffer = null;
	}

    public void close() throws IOException {
        // System.err.println("Close");

        flush();
        getTopStream().close();
    }

    public void flush() throws IOException {
        // System.err.println("Flush");

        /*
          "The flush method of OutputStream does nothing." - JDK1.3 API
          documentation. I'm calling it just in case it starts doing
          something in a future version of FileOutputStream or
          OutputStream.
        */
        getTopStream().flush();

        /*
          As we're JDK 1.4, this saves a bit of time because we don't
          bother flushing meta data (we only care about flushing content)
        */
        theFile.getChannel().force(false);

        // Original
        // getFD().sync();  //"Force all system buffers to synchronize with the underlying device." - JDK1.3 API documentation.
    }

    private OutputStream getTopStream() {
        if (theBuffer == null)
            return theFile;
        else
            return theBuffer;
    }

    public void write(byte[] b) throws IOException {
        getTopStream().write(b);
        bytesWritten += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        getTopStream().write(b, off, len);
        bytesWritten += len;
    }

    public void write(int b) throws IOException {
        getTopStream().write(b);
        ++bytesWritten;
    }

    public long bytesWritten() {
        return bytesWritten;
	}

    private long bytesWritten;
}
