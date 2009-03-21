package org.dancres.blitz.remote.nio;

import org.dancres.blitz.SpaceImpl;

/**
 */
public class DispatcherFactoryImpl implements DispatcherFactory {

    private SpaceImpl _space;

    public DispatcherFactoryImpl(SpaceImpl aSpace) {
        _space = aSpace;
    }

    public Dispatcher newDispatcher() {
        return new DispatcherImpl(_space);
    }
}
