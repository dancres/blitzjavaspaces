package org.dancres.blitz.entry;

class StorageFactory {
    static Storage getStorage(String aType) {
        if (aType.equals(EntryRepository.ROOT_TYPE))
            return new RootStorage();
        else
            return new EntryStorage(aType);
    }
}
