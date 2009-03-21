package org.dancres.blitz.test;

import net.jini.entry.AbstractEntry;

/**
 */
public class BlockEntry extends AbstractEntry {
    public String name_;

    public BlockEntry() {
    }

    public BlockEntry(String name) {
        name_ = name;
    }
}
