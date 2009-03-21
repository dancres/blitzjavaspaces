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

package com.go.trove.log;

import java.io.*;
import java.util.*;
import java.text.*;

/******************************************************************************
 * Opens up files to be used by an {@link IntervalLogStream}.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class FileLogStreamFactory implements IntervalLogStream.Factory {
    private File mDirectory;
    private DateFormat mDateFormat;
    private String mExtension;

    /**
     * Creates log files in the given directory. The names are created by
     * appending the name of the directory, a hyphen, a time stamp and 
     * an extension.
     * 
     * For example, if the directory is "/logs/MyApp", the format is 
     * "yyyyMMdd", and the extension is ".log", then a generated file might be:
     * "/logs/MyApp/MyApp-19990608.log".
     *
     * @param directory Directory to create log files in.
     * @param format DateFormat to use for creating new file names.
     * @param extension Extension to put at the end of new file name.
     */
    public FileLogStreamFactory(File directory, 
                                DateFormat format,
                                String extension) {

        if (directory == null) {
            throw new NullPointerException
                ("FileLogStreamFactory directory not specified");
        }

        try {
            mDirectory = new File(directory.getCanonicalPath());
        }
        catch (IOException e) {
            mDirectory = directory;
        }

        if (format == null) {
            throw new NullPointerException
                ("FileLogStreamFactory date format not specified");
        }

        mDateFormat = format;

        if (extension == null) {
            mExtension = "";
        }
        else {
            mExtension = extension;
        }
    }

    public OutputStream openOutputStream(Date date) throws IOException {
        if (!mDirectory.exists()) {
            if (!mDirectory.mkdirs()) {
                throw new IOException("Unable to create directory: \"" + 
                                      mDirectory + '"');
            }
        }
        
        String fileName = mDirectory.getName() + '-';
        synchronized (mDateFormat) {
            fileName += mDateFormat.format(date);
        }
        
        File file = new File(mDirectory, fileName + mExtension);
        return new FileOutputStream(file.getPath(), true);
    }
}

