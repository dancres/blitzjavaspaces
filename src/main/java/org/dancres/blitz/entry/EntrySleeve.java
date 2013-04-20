package org.dancres.blitz.entry;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.lease.LeasedResource;

/**
   All MangledEntry instances written to Blitz are wrapped in an EntrySleeve.
   EntrySleeves are uniquely identified by their type and Uid.
 */
public interface EntrySleeve extends LeasedResource {
    public OID getOID();
    public String getType();
    public MangledEntry getEntry();
    public boolean isDeleted();
    public int getHashCodeForField(int anOffset);
}
