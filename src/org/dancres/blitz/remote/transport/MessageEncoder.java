package org.dancres.blitz.remote.transport;

import java.io.Serializable;
import java.io.NotSerializableException;
import java.io.IOException;

import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

/**
 */
public class MessageEncoder extends ProtocolEncoderAdapter {
    private int maxObjectSize = MessageCodecFactory.DEFAULT_MAX_OBJECT_SIZE;

    /**
     * Creates a new instance.
     */
    public MessageEncoder() {
    }

    /**
     * Returns the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, this encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public int getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, this encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException(
                "maxObjectSize: " + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    public void encode(IoSession session, Object message,
                       ProtocolEncoderOutput out) throws Exception {
        if (!(message instanceof Message)) {
            throw new IOException("I'm only good for Messages");
        }


        Message myMessage = (Message) message;

        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.setAutoExpand(true);

        /*
            Ensure the message payload is within size limit
            Write length field to buffer,
            Write conversationId and
            payload array.
            Post bytebuffer to "out"
         */
        if ((myMessage.getPayload().length + 8) > maxObjectSize)
            throw new IOException("Message is too large");

        buf.putInt(4 + myMessage.getPayload().length);
        buf.putInt(myMessage.getConversationId());
        buf.put(myMessage.getPayload());

        buf.flip();
        out.write(buf);
    }
}
