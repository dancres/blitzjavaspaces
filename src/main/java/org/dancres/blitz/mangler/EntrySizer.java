package org.dancres.blitz.mangler;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import net.jini.core.entry.Entry;

/**
   <p>Utility class to compute a rough indication of the number of bytes an
   Entry will consume once written to a Blitz JavaSpace instance (persistent
   or transient).</p>

   <p>Basic usage is to construct an instance of EntrySizer and then invoke
   computeSize for each Entry you're interested in.</p>
 */
public class EntrySizer {
    private EntryMangler theMangler = new EntryMangler();

    public EntrySizer() {
    }

    public int computeSize(Entry anEntry) throws IOException {
        MangledEntry myEntry = theMangler.mangle(anEntry);

        ByteArrayOutputStream myBAOS = new ByteArrayOutputStream();
        ObjectOutputStream myOOS = new ObjectOutputStream(myBAOS);

        myOOS.writeObject(myEntry);
        myOOS.close();

        return myBAOS.size();
    }
}