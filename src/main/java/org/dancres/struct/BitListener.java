package org.dancres.struct;

public interface BitListener {
    /**
       @return <code>true</code> if iteration can stop
     */
    public boolean active(int aPos);

    /**
       @return <code>true</code> if iteration can stop
     */
    public boolean inactive(int aPos);
}