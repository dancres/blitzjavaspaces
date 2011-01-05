package org.dancres.blitz.entry;

import java.io.IOException;
import java.util.Set;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.config.EntryConstraints;

/**
   Entry's are separated by type with all instances of a particular type
   held in one EntryRepository instance.
 */
public interface EntryRepository {
    public static final String ROOT_TYPE = "java.lang.Object";

    public EntryConstraints getConstraints();

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

    /**
       Tells this Repository about a subtype which has just been created
       and would need to be search if this type were the specified template.
     */
    public void addSubtype(String aType) throws IOException;

    /**
       Return a list of all currently known subtypes
     */
    public Set<String> getSubtypes();

    /**
       Write returns it's result via the passed WriteEscort.  i.e.  If
       it returns without IOException, the write is deemed to have succeeded.
       The caller is responsible for recovering the appropriate information
       from the WriteEscort.

       @param anExpiry an absolute expiry time
     */
    public void write(MangledEntry anEntry, long anExpiry,
                      WriteEscort anEscort)
        throws IOException;

    /**
       Locate suitable matches for the passed template and offer them
       to the SearchVisitor.  Note that there is no return value from this
       method because the appropriate value is passed to the SearchVisitor
       via offer which it then accepts.  Thus the caller is expected to
       interrogate the SearchVisitor implementation to determine the outcome.

       @param aTemplate can be null or a wildcard MangledEntry indicating an
       Entry<class>* search or a MangledEntry with keys which results in an
       indexed search
     */
    public void find(MangledEntry aTemplate, SearchVisitor aVisitor)
        throws IOException;

    /**
       Under some circumstances, we wish to offer a visitor the chance to
       acquire a specific Entry identified by aOID.  This step must be
       managed by the Repository (as are finds) in order to handle such 
       issues as paging in from disk etc.

       @return <code>true</code> if an offer was made.  If an offer wasn't made
       it's due to the Entry no longer being available.  The caller would be
       best advised to stop the search at this point to save I/O.
     */
    public boolean find(SearchVisitor aVisitor, OID aOID, MangledEntry aPreload)
        throws IOException;

    public LongtermOffer getOffer(OID anOID) throws IOException;

        /**
         @return an OpInfo if the operation was successful, <code>null</code>
         otherwise
         */
    public boolean renew(OID aOID, long anExpiry) throws IOException;

    /**
       @return an OpInfo if the operation was successful, <code>null</code>
       otherwise
     */
    public boolean cancel(OID aOID) throws IOException;

    /**
       @return the Entry type held in this repository
     */
    public String getType();

    /**
       @return the number of Entry's stored on disk
     */
    public int getTotalStoredEntries() throws IOException;

    /**
       @return the number of Entry's stored in total including those uncommitted
       in cache
     */
    public int getTotalLiveEntries();
}
