package org.dancres.blitz.oid;

import java.io.Serializable;

import org.dancres.blitz.cache.Identifier;

/**
   Opaque identifier for a particular entry within an EntryRepository instance.
   This class understands how a zone id and oid map to an on-disk
   representation.  No other class needs this information it can use methods
   provided here.
*/
public interface OID extends Serializable, Identifier {
    public int getZoneId();

    public long getId();
}
