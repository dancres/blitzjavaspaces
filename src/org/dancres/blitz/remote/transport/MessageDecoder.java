package org.dancres.blitz.remote.transport;

import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

/**
 */
public class MessageDecoder extends CumulativeProtocolDecoder {
    private int maxObjectSize = MessageCodecFactory.DEFAULT_MAX_OBJECT_SIZE;

    /**
     * Creates a new instance with the {@link ClassLoader} of
     * the current thread.
     */
    public MessageDecoder() {
    }

    /**
     * Returns the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link org.apache.mina.common.BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     */
    public int getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link org.apache.mina.common.BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException(
                "maxObjectSize: " + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    protected boolean doDecode(IoSession session, ByteBuffer in,
                               ProtocolDecoderOutput out) throws Exception {
        if (!in.prefixedDataAvailable(4, maxObjectSize)) {
            return false;
        }

        /*
            Read length field
            Read conversation id
            Extract payload array
            Construct Message object and post to "out"
         */
        int myLength = in.getInt();
        int myConversationId = in.getInt();
        byte[] myPayload = new byte[myLength - 4];
        in.get(myPayload);

        out.write(new Message(myConversationId, myPayload));

        return true;
    }
}
