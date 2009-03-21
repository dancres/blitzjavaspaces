package org.dancres.blitz.remote.transport;

import java.io.Serializable;

/**
 */
public class PingMessage implements Serializable {
    private long _seqNum;
    private byte[] _data = new byte[1024];
    
    public PingMessage(long aSeqNum) {
        _seqNum = aSeqNum;
    }

    public long getSeqNum() {
        return _seqNum;
    }

    public String toString() {
        return "PM: " + _seqNum;
    }
}
