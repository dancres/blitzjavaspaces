package org.dancres.blitz.config;

/**
   <p>Marker interface indicating a per-Entry type configuration variable.
   EntryConstraints are per Entry-type requirements to
   be asserted against such things as search behaviour.</p>

   <p>Per-Entry constraints are setup in the .config file as follows:
   </p>

   <ol>
   <li> Define a variable using the classname of the type,
   replaceing . or $ with _.
   </li>
   <li>The variable should then be initialized to an array of
   EntryConstraints specifying requirements.
   </li>
   </ol>

   <p>Here's an example that specifies a per-type cache size which overrides
   the global default specified in <code>entryReposCacheSize</code> and
   enables FIFO ordering for searches and writes of this class.</p>

   <pre>
   org_dancres_blitz_SpaceFifonessTest_TestEntry =
            new EntryConstraint[] {new CacheSize(1024), new Fifo()};
   </pre>
 */
public interface EntryConstraint {
}