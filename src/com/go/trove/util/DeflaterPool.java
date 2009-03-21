/* ====================================================================
 * Trove - Copyright (c) 1997-2001 Walt Disney Internet Group
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

package com.go.trove.util;

import java.util.*;

/******************************************************************************
 * Since {@link Deflater Deflaters} can be expensive to allocate, re-use them
 * with this pool.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/06/14 <!-- $-->
 */
public class DeflaterPool {
    private static final int LIMIT;

    private static List cPool;
    private static List cNoWrapPool;

    static {
        LIMIT = Integer.getInteger("com.go.trove.util.DeflaterPool.LIMIT", 8).intValue();
    }

    public static synchronized void clear() {
        if (cPool != null) {
            Iterator it = cPool.iterator();
            while (it.hasNext()) {
                ((Deflater)it.next()).end();
            }
            cPool = null;
        }
        if (cNoWrapPool != null) {
            Iterator it = cNoWrapPool.iterator();
            while (it.hasNext()) {
                ((Deflater)it.next()).end();
            }
            cNoWrapPool = null;
        }
    }

    public static synchronized Deflater get(int level, boolean nowrap) {
        List pool;

        if (nowrap) {
            if ((pool = cNoWrapPool) == null) {
                return new Deflater(level, true);
            }
        }
        else {
            if ((pool = cPool) == null) {
                return new Deflater(level, false);
            }
        }

        if (pool.isEmpty()) {
            return new Deflater(level, nowrap);
        }

        Deflater d = (Deflater)pool.remove(pool.size() - 1);
        d.setLevel(level);
        d.setStrategy(Deflater.DEFAULT_STRATEGY);
        return d;
    }

    public static Deflater get(int level) {
        return get(level, false);
    }

    public static Deflater get() {
        return get(Deflater.DEFAULT_COMPRESSION, false);
    }

    public static synchronized void put(Deflater d) {
        d.reset();
        List pool;

        if (d.isNoWrap()) {
            if ((pool = cNoWrapPool) == null) {
                pool = cNoWrapPool = new ArrayList(LIMIT);
            }
        }
        else {
            if ((pool = cPool) == null) {
                pool = cPool = new ArrayList(LIMIT);
            }
        }

        if (pool.size() < LIMIT) {
            pool.add(d);
        }
        else {
            d.end();
        }
    }
}
