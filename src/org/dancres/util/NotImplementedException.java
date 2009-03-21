package org.dancres.util;

/**
   This runtime exception may be thrown by a method to indicate that a
   particular feature (or indeed the whole method) is not yet implemented.
 */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException() {
		super();
    }

    public NotImplementedException(String aMessage) {
		super(aMessage);
    }
}
