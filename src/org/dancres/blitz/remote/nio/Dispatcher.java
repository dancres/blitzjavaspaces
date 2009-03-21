package org.dancres.blitz.remote.nio;

import java.io.IOException;

/**
 * This is a protocol specific class whilst the rest is framework.  ControlBlock
 * dispatches to an instance of one of these as obtained from a DispatcherFactory.
 * Thus message decoding, dispatching etc are all managed outside of ControlBlock
 * which simply ships data to/from a client.
 */
public interface Dispatcher {
    public void process(ControlBlock aBlock) throws IOException;
}
