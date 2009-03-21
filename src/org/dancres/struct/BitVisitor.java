package org.dancres.struct;

/**
   An object to take a bit array and iterate over them reporting offset of
   each set bit.
*/
public interface BitVisitor {
    /**
       @return -1 when no bits are left
     */
    public int getNext();
}