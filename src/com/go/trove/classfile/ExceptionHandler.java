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

package com.go.trove.classfile;

import java.io.*;

/******************************************************************************
 * This class corresponds to the exception_table structure as defined in
 * section 4.7.4 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class ExceptionHandler implements LocationRange {
    private Location mStart;
    private Location mEnd;
    private Location mCatch;
    private ConstantClassInfo mCatchType;
    
    /**
     * @param startLocation
     * @param endLocation
     * @param catchLocation
     * @param catchType if null, then catch every object.
     */
    public ExceptionHandler(Location startLocation,
                            Location endLocation,
                            Location catchLocation,
                            ConstantClassInfo catchType) {
        mStart = startLocation;
        mEnd = endLocation;
        mCatch = catchLocation;
        mCatchType = catchType;
    }
    
    public Location getStartLocation() {
        return mStart;
    }
    
    public Location getEndLocation() {
        return mEnd;
    }
    
    public Location getCatchLocation() {
        return mCatch;
    }
    
    /**
     * Returns null if every object is caught by this handler.
     */
    public ConstantClassInfo getCatchType() {
        return mCatchType;
    }

    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        LocationRange other = (LocationRange)obj;

        int result = getStartLocation().compareTo(other.getStartLocation());

        if (result == 0) {
            result = getEndLocation().compareTo(other.getEndLocation());
        }

        if (result == 0 && obj instanceof ExceptionHandler) {
            result = getCatchLocation().compareTo
                (((ExceptionHandler)other).getCatchLocation());
        }

        return result;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        int start_pc = getStartLocation().getLocation();
        int end_pc = getEndLocation().getLocation();
        int handler_pc = getCatchLocation().getLocation();
        int catch_type;
        ConstantClassInfo catchType = getCatchType();
        if (catchType == null) {
            catch_type = 0;
        }
        else {
            catch_type = catchType.getIndex();
        }

        check("exception start PC", start_pc);
        check("exception end PC", end_pc);
        check("exception handler PC", handler_pc);

        dout.writeShort(start_pc);
        dout.writeShort(end_pc);
        dout.writeShort(handler_pc);
        dout.writeShort(catch_type);
    }

    private void check(String type, int addr) throws RuntimeException {
        if (addr < 0 || addr > 65535) {
            throw new RuntimeException("Value for " + type + " out of " +
                                       "valid range: " + addr);

        }
    }

    static ExceptionHandler readFrom(ConstantPool cp,
                                     DataInput din) throws IOException {
        int start_pc = din.readUnsignedShort();
        int end_pc = din.readUnsignedShort();
        int handler_pc = din.readUnsignedShort();
        int catch_type = din.readUnsignedShort();

        ConstantClassInfo catchTypeConstant;
        if (catch_type == 0) {
            catchTypeConstant = null;
        }
        else {
            catchTypeConstant = (ConstantClassInfo)cp.getConstant(catch_type);
        }

        return new ExceptionHandler(new FixedLocation(start_pc),
                                    new FixedLocation(end_pc),
                                    new FixedLocation(handler_pc),
                                    catchTypeConstant);
    }
}
