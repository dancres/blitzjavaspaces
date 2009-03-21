package org.dancres.blitz.notify;

import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.oid.OID;

/**
   <p>The event types break down into two classes - those that represent
   an invisible/visible transition and those that represent an
   unavailable/available transition.  Here's a summary in table form:</p>

<table summary="Visibility and Availability Transitions" border="0" cellspacing="2" cellpadding="1">
<tr align="center" valign="middle"><th></th><th>Invisible/Visible</th><th>Unavailable/Available</th>
</tr>
<tr align="center" valign="middle"><th>ENTRY_WRITE</th>
	<td>X</td>
	<td>X</td>
</tr>
<tr align="center" valign="middle"><th>ENTRY_WRITTEN</th>
	<td>X</td>
	<td>X</td>
</tr>
<tr align="center" valign="middle"><th>ENTRY_VISIBLE</th>
	<td>X</td>
	<td>X</td>
</tr>
<tr align="center" valign="middle"><th>ENTRY_NOT_CONFLICTED</th>
	<td>O</td>
	<td>X</td>
</tr>
</table>

   @see org.dancres.blitz.notify.EventQueue
 */
public class QueueEvent {
    /**
       Context object is null
     */
    public static final int TRANSACTION_ENDED = 1;

    /**
       Context object is a MangledEntry plus OID -
       this event is triggered when a transaction initially writes a new Entry
     */
    public static final int ENTRY_WRITE = 2;

    /**
       Context object is a MangledEntry plus OID -
       this event is triggered when a transaction commits a write.
     */
    public static final int ENTRY_WRITTEN = 3;

    /**
       Context object is a MangledEntry plus OID - this event is triggered in
       the case where a take has been aborted and thus an Entry has been deemed
       visible again.
     */
    public static final int ENTRY_VISIBLE = 4;

    /**
       Context object is a MangledEntry plus OID - this event is triggered in
       the case where an Entry was previously conflicted with a collection of
       read locks and has since been made available for a take again.
       Note, if the Entry was write locked, we don't generate this event
       as it'll result from the ENTRY_WRITTEN event.  Note also we don't
       generate this event if the Entry is marked as taken/deleted (obviously)
     */
    public static final int ENTRY_NOT_CONFLICTED = 5;

    private int theType;
    private TxnState theTxn;
    private Context theContext;

    public QueueEvent(int aType, TxnState aTxn, Context aContext) {
        theType = aType;
        theTxn = aTxn;
        theContext = aContext;
    }

    public int getType() {
        return theType;
    }

    public TxnState getTxn() {
        return theTxn;
    }

    public Context getContext() {
        return theContext;
    }

    public static class Context {
        private MangledEntry _entry;
        private OID _oid;

        public Context(MangledEntry anEntry, OID anOID) {
            _oid = anOID;
            _entry = anEntry;
        }

        public MangledEntry getEntry() {
            return _entry;
        }

        public OID getOID() {
            return _oid;
        }
    }
}
