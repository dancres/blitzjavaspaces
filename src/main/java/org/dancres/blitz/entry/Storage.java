package org.dancres.blitz.entry;

import java.io.IOException;
import java.util.Set;

import org.dancres.blitz.arc.BackingStore;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.MangledField;

import org.dancres.blitz.oid.OID;

/**
   Provides primitive methods for the manipulation of the storage mechanism.
   Caching etc. is a higher-level responsibility.  This interface can be
   used to write, update and remove data.  <P>

   It has basic understanding of what it stores in that it knows how
   to index, how to store subtype information and the storage patterns used
   for bucketing and other settings. <P>

   Basic lifecycle is to create storage element and then call didntExist.
   If it returns true, one should set the indexing information using setFields.
   Note that, if indexing is disabled, didntExist will always return true
   and there's no requirement to call setFields. <P>
*/
public interface Storage extends BackingStore {
    /**
       @return <code>true</code> if initialization succeeded in accordance
       with the mustExist flag.  i.e. If mustExist is true and the databases
       couldn't be opened one will receive <code>false</code> NOT an
       exception.
     */
    public boolean init(boolean mustExist) throws IOException;

    /**
       Called to setup schema information, create indexes etc - should only
       be called once in response to <code>true</code> being returned from
       <code>didntExist()</code>
     */
    public void setFields(MangledField[] aSetOfFields) throws IOException;

    /**
       Indicates if we were created for the first time as the result of
       a call to <code>EntryRepositoryFactory.get()</code>.  Note that this
       flag is reset after <code>setFields()</code> is called.  Thus, even
       if an EntryRepository has been informed of children, it will still
       return <code>true</code> until <code>setFields</code> is called.
     */
    public boolean noSchemaDefined();

    public void close() throws IOException;

    /**
       Storage instances support the concepts of:

       <ul>
       <li> Temporarily holding Entry's in-memory only - useful for
       transactional operations such as caching of writes until commit.</li>
       <li> Caching of dirty data destined for disk but not yet written.  This
       allows for better performance as writes may be performed asynchronously.
       </li>

       Before a disk search is performed via <code>find</code> this method
       should be invoked to check for state which may not have reached disk
       as yet.  If this is not done, out-of-date state may be returned.
     */
    public TupleLocator findCached(MangledEntry anEntry);

    /**
       Return a set of potential matches based on the passed template.  Note
       one must test each returned tuple for an exact match across ALL keys
       because this method is "speculative".  It will return likely matches
       it does not guarentee to have located an exact subset based on the
       passed template.  As a full match is necessary to avoid hash-collisions
       this shouldn't be a problem.

       @param anEntry a template to use to locate matches.

       @return TupleLocator instance of <code>null</code> if there are no
       possible matches.
     */
    public TupleLocator find(MangledEntry anEntry) throws IOException;

    /**
       Tells this Repository about a subtype which has just been created
       and would need to be searched if this type were the specified template.
     */
    public void addSubtype(String aType) throws IOException;
    public Set<String> getSubtypes();

    /**
       @return a new UID for an entry to be added to storage.
     */
    public OID getNextId() throws IOException;

    /**
       Causes Storage instance to scan for expired Entry instances which will
       be passed to <code>aReaper</code>
     */
    public void bringOutTheDead(EntryReaper aReaper) throws IOException;

    /**
       @return the type of Entry this storage element contains
     */
    public String getType();

    /**
       @return the number of Entry's of the type currently held on disk.  Note
       that because of Blitz's caching/logging behaviour, this count doesn't
       reflect the actual number of instances of this type.
     */
    public int getNumEntries() throws IOException;

    public void delete() throws IOException;
}
