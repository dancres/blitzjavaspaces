package org.dancres.blitz.cache;

public interface Cache {
    public int getSize();
    public void add(CacheListener aListener);
}
