package org.dancres.blitz.remote.transport;

import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;
import org.apache.mina.filter.codec.serialization.ObjectSerializationDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;

/**
 */
public class MessageCodecFactory implements ProtocolCodecFactory {
    static final int DEFAULT_MAX_OBJECT_SIZE = 1048576;

    private final MessageEncoder encoder;
    private final MessageDecoder decoder;

    /**
     * Creates a new instance with the {@link ClassLoader} of
     * the current thread.
     */
    public MessageCodecFactory() {
        encoder = new MessageEncoder();
        decoder = new MessageDecoder();
    }

    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    /**
     * Returns the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * <p/>
     * This method does the same job with {@link ObjectSerializationEncoder#getMaxObjectSize()}.
     */
    public int getEncoderMaxObjectSize() {
        return encoder.getMaxObjectSize();
    }

    /**
     * Sets the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * <p/>
     * This method does the same job with {@link ObjectSerializationEncoder#setMaxObjectSize(int)}.
     */
    public void setEncoderMaxObjectSize(int maxObjectSize) {
        encoder.setMaxObjectSize(maxObjectSize);
    }

    /**
     * Returns the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, the
     * decoder will throw a {@link org.apache.mina.common.BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     * <p/>
     * This method does the same job with {@link ObjectSerializationDecoder#getMaxObjectSize()}.
     */
    public int getDecoderMaxObjectSize() {
        return decoder.getMaxObjectSize();
    }

    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, the
     * decoder will throw a {@link org.apache.mina.common.BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     * <p/>
     * This method does the same job with {@link ObjectSerializationDecoder#setMaxObjectSize(int)}.
     */
    public void setDecoderMaxObjectSize(int maxObjectSize) {
        decoder.setMaxObjectSize(maxObjectSize);
    }
}
