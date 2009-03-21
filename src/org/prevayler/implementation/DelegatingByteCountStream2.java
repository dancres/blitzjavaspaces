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

import org.dancres.blitz.txn.TxnManager;

import java.io.*;
import java.util.logging.Level;

/**
   A FileOutputStream that counts the number of bytes written and forces all
   buffers to synchronize with the underlying device when flushed.
*/
final class DelegatingByteCountStream2 extends OutputStream {
    private ByteArrayOutputStream theBuffer = new ByteArrayOutputStream();
    private RandomAccessFile theFile;
    private int theBufferSize;

    DelegatingByteCountStream2(File file, int aBufferSize)
        throws IOException {

        // System.err.println("Buffer size: " + aBufferSize);

        // TxnManager.theLogger.log(Level.SEVERE, "Open: " + file);

        theFile = new RandomAccessFile(file, "rwd");

        theBufferSize = aBufferSize;
    }

    public void close() throws IOException {
        // TxnManager.theLogger.log(Level.SEVERE, "Close log");

        flush();
        theFile.close();
    }

    public void flush() throws IOException {
        // System.err.println("Flush");

        byte[] myBytes = theBuffer.toByteArray();
        theBuffer.reset();

        // File was opened in "rwd" mode so we need do no more than write
        //
        theFile.write(myBytes);
    }

    public void write(byte[] b) throws IOException {
        theBuffer.write(b);
        bytesWritten += b.length;

        checkBuffer();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        theBuffer.write(b, off, len);
        bytesWritten += len;

        checkBuffer();
    }

    public void write(int b) throws IOException {
        theBuffer.write(b);
        ++bytesWritten;

        checkBuffer();
    }

    private void checkBuffer() throws IOException {
        if (theBuffer.size() > theBufferSize)
            flush();
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    private long bytesWritten;
}
