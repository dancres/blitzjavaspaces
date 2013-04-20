package org.dancres.struct;

import java.io.Serializable;

public interface LinkedInstance extends Serializable {
    public void setNext(LinkedInstance aLinkedInstance);
    public LinkedInstance getNext();
    public void setPrev(LinkedInstance aLinkedInstance);
    public LinkedInstance getPrev();
}
