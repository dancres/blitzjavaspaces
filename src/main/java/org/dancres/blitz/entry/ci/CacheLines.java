package org.dancres.blitz.entry.ci;

import org.dancres.blitz.entry.TupleLocator;
import org.dancres.blitz.entry.EntrySleeve;

public interface CacheLines {
    public String getName();
    public TupleLocator getIds(int aHashcode);
    public int getSize(int aHashcode);
    public int getSize();
    public void insert(EntrySleeve aSleeve);
    public void remove(EntrySleeve aSleeve);
}
