package org.dancres.blitz.remote.user;

import net.jini.space.JavaSpace;

/**
 * ColocatedAgent's are executed at startup, just prior to the space proxy being
 * published via JoinManager.  This allows a developer to, for example, ensure a
 * space is initialized with a set of tokens for locks etc. <b>DO NOT</b>
 * block this method.  If you wish to have the Agent be long-running, <code>
 * init</code> should spawn a thread.
 */
public interface ColocatedAgent {
    public void init(JavaSpace aSpace) throws Exception;
}
