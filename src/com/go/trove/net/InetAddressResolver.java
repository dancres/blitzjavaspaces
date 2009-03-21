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
import java.util.*;
import java.lang.ref.*;
import com.go.trove.util.SoftHashMap;

/******************************************************************************
 * Allows network host names to resolve to inet addresses via event
 * notification.
 * <p>
 * Note: This class makes use of the java.util.Timer class, available only in
 * Java 2, version 1.3. 
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 1.1 $-->, <!--$$JustDate:--> 01/01/23 <!-- $-->
 */
public class InetAddressResolver {
    // Wait for 10 minutes before trying to resolve again.
    private static final long RESOLVE_PERIOD = 10 * 60 * 1000;
    private static Timer cResolveTimer;

    private static Map cResolvers = new SoftHashMap();

    private static Timer getResolveTimer() {
        Timer timer = cResolveTimer;
        if (timer == null) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                public void run() {
                    Thread.currentThread().setName("InetAddressResolver");
                }
            }, 0);
            cResolveTimer = timer;
        }
        return timer;
    }

    /**
     * Resolve the host name into InetAddresses and listen for changes.
     * The caller must save a reference to the resolver to prevent it from
     * being reclaimed by the garbage collector.
     *
     * @param host host to resolve into InetAddresses
     * @param listener listens for InetAddresses
     */    
    public static InetAddressResolver listenFor(String host,
                                                InetAddressListener listener) {
        synchronized (cResolvers) {
            InetAddressResolver resolver = 
                (InetAddressResolver)cResolvers.get(host);
            if (resolver == null) {
                resolver = new InetAddressResolver(host);
                cResolvers.put(host, resolver);
            }
            resolver.addListener(listener);
            return resolver;
        }
    }

    private final String mHost;

    private List mListeners = new ArrayList();
    
    // Is either an instance of UnknownHostException or InetAddress[].
    private Object mResolutionResults;

    private InetAddressResolver(String host) {
        mHost = host;

        // Initial delay is random so that requests against other hosts are
        // staggered.
        long delay = (long)(Math.random() * RESOLVE_PERIOD);
        getResolveTimer().schedule(new Resolver(this), delay, RESOLVE_PERIOD);
    }

    private synchronized void addListener(InetAddressListener listener) {
        mListeners.add(listener);
        if (!resolveAddresses()) {
            // Ensure that new listener is called the first time.
            notifyListener(listener);
        }
    }

    private synchronized void notifyListener(InetAddressListener listener) {
        if (mResolutionResults instanceof UnknownHostException) {
            listener.unknown((UnknownHostException)mResolutionResults);
        }
        else {
            InetAddress[] addresses = (InetAddress[])mResolutionResults;
            addresses = (InetAddress[])addresses.clone();
            listener.resolved(addresses);
        }
    }

    // Returns true if anything changed from the last time this was invoked.
    private synchronized boolean resolveAddresses() {
        boolean changed;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(mHost);
            if (mResolutionResults instanceof UnknownHostException) {
                changed = true;
            }
            else {
                // Results may be ordered differently, so keep them sorted.
                Arrays.sort(addresses, new Comparator() {
                    public int compare(Object a, Object b) {
                        return ((InetAddress)a).getHostAddress()
                            .compareTo(((InetAddress)b).getHostAddress());
                    }
                });

                changed = !Arrays.equals
                    (addresses, (InetAddress[])mResolutionResults);
            }
            mResolutionResults = addresses;
        }
        catch (UnknownHostException e) {
            changed = !(mResolutionResults instanceof UnknownHostException);
            mResolutionResults = e;
        }

        if (changed) {
            int size = mListeners.size();
            for (int i=0; i<size; i++) {
                notifyListener((InetAddressListener)mListeners.get(i));
            }
        }

        return changed;
    }

    private class Resolver extends TimerTask {
        // Weakly references owner so that the timer won't prevent it from
        // being garbage collected.
        private final Reference mOwner;

        public Resolver(InetAddressResolver owner) {
            mOwner = new WeakReference(owner);
        }

        public void run() {
            InetAddressResolver owner = (InetAddressResolver)mOwner.get();
            if (owner == null) {
                cancel();
            }
            Thread t = Thread.currentThread();
            String originalName = t.getName();
            t.setName("InetAddressResolver:" + owner.mHost);
            try {
                owner.resolveAddresses();
            }
            finally {
                t.setName(originalName);
            }
        }
    }
}
