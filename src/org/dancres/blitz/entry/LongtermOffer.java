package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.oid.OID;

/**
 * In cases where one wishes to repeatedly offer the same entry to a
 * SearchVisitor, a LongtermOffer can signficiantly reduce overhead and
 * improve cache utilization.
 */
public interface LongtermOffer {
    /**
     * @return the type of the entry this offer is associated with
     */
    public String getEntryType();
    public boolean offer(SearchVisitor aVisitor) throws IOException;
    public void release() throws IOException;
    public OID getOID();
}
