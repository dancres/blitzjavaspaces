package org.dancres.blitz.remote.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Responsible for processing selector messages associated with a particular
 * endpoint (socket) as defined by a SelectionKey.  Uses a dispatcher to
 * actually process the data received (it may also invoke methods etc) and to
 * generate responses to be transmitted back through the endpoint.
 */
public class ControlBlock {
    private SelectionKey _key;
    private ArrayList _outputBuffers = new ArrayList();
    private Dispatcher _dispatcher;

    ControlBlock(SelectionKey aKey, Dispatcher aDispatcher) {
        _key = aKey;
        _dispatcher = aDispatcher;
    }

    /**
     * Processes the SelectionKey's events according to internal state machine
     * and dispatches work to threads accordingly
     */
    public void process() throws IOException {
        /*
            Determine what flags are tripped

            If write flag is set invoke a write using current list of
            buffers.  Scan buffers for now done and remove them from the
            list.  If there are no buffers, turn of the write interest flag
            in selector

            If read flag is set grab bytes and parse with state machine
            which will then figure out whether we are ready to process
            a request and dispatch it to the pool with a reference to us
            so's we can post the response.
        */
        SocketChannel myChannel = (SocketChannel) _key.channel();

        if (_key.isWritable()) {
            synchronized(_outputBuffers) {

                while (_outputBuffers.size() > 0) {
                    ByteBuffer myBuffer = (ByteBuffer) _outputBuffers.get(0);

                    myChannel.write(myBuffer);

                    if (myBuffer.hasRemaining()) {
                        break;
                    } else
                        _outputBuffers.remove(0);
                }

                if (_outputBuffers.size() == 0) {
                    _key.interestOps(_key.interestOps() ^ SelectionKey.OP_WRITE);
                }
            }
        }

        if (_key.isReadable()) {
            _dispatcher.process(this);
        }
    }

    public SocketChannel getChannel() {
        return (SocketChannel) _key.channel();
    }

    public void send(ByteBuffer[] aBuffers) {
        /*
            Lock current list of buffers add this one to the list

            If this is the first new set of buffers we need to set
            write interest on our selection key and then do
            _key.selector().wakeup() so we get events
        */
        boolean enableWrites;

        synchronized(_outputBuffers) {
            enableWrites = (_outputBuffers.size() == 0);

            for (int i = 0; i < aBuffers.length; i++) {
                _outputBuffers.add(aBuffers[i]);
            }
        }

        if (enableWrites) {
            if (_key.isValid()) {
                _key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
                _key.selector().wakeup();
            }
        }
    }
}
