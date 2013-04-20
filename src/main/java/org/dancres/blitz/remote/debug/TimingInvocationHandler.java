package org.dancres.blitz.remote.debug;

import java.lang.reflect.Method;

import net.jini.jeri.BasicInvocationHandler;
import net.jini.jeri.ObjectEndpoint;

import net.jini.core.constraint.MethodConstraints;

/**
   <p><code>TimingInvocationHandler</code>, dispatches all remote
   invocations as normal whilst reporting the time taken to execute the call.
   This can be used to locate "bottlenecks" in remote service configurations.
   </p>

   <p>NOTE: Using this handler in combination with clients which stipulate
   verification when preparing proxies is doomed to failure because there
   isn't currently a suitable <code>TrustVerifier</code>.  You could, of
   course, adopt the brute force approach of putting this code in all
   client classpaths.....</p>

   @todo Code up TrustVerifier for this InvocationHandler
 */
public class TimingInvocationHandler extends BasicInvocationHandler {
    public TimingInvocationHandler(ObjectEndpoint anEndpoint,
                                   MethodConstraints aServerConstraints) {
        super(anEndpoint, aServerConstraints);
    }

    public TimingInvocationHandler(BasicInvocationHandler aHandler,
                                   MethodConstraints aClientConstraints) {
        super(aHandler, aClientConstraints);
    }

    public Object invoke(Object aProxy, Method aMethod, Object[] anArgs)
        throws Throwable {
        long myStart = System.currentTimeMillis();

        Object myResult = super.invoke(aProxy, aMethod, anArgs);

        long myEnd = System.currentTimeMillis();

        System.err.println("Method: " + aMethod + ": " + (myEnd - myStart) +
                           " ms (" + myStart + ")");

        return myResult;
    }
}
