package org.dancres.blitz.lease;

import java.io.Serializable;

/**
   A space-global unique identifier which can be renewed or cancelled via
   <code>LeaseHandlers</code> in such a manner that the caller need know
   nothing about the exact nature of that resource.
 */
public interface SpaceUID extends Serializable {
}
