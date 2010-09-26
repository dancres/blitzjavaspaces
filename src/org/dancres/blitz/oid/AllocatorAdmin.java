package org.dancres.blitz.oid;

import java.io.IOException;

interface AllocatorAdmin extends Allocator {
    void delete() throws IOException;
    void discard();
}