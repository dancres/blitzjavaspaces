package org.dancres.blitz.remote.transport;

import java.io.Serializable;

/**
 */
public class Message {
    private int _conversationId;
    private byte[] _payload;

    Message(int aConvId, byte[] aPayload) {
        _conversationId = aConvId;
        _payload = aPayload;
    }

    byte[] getPayload() {
        return _payload;
    }

    int getConversationId() {
        return _conversationId;
    }
}
