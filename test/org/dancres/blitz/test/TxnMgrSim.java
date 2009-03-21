package org.dancres.blitz.test;

import java.io.*;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.StringTokenizer;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;
import net.jini.core.lease.*;
import net.jini.space.*;
import net.jini.core.entry.*;

/**
   Provides support for command-line driving of the Space for the purposes
   of testing transactional behaviour, recovery etc.

   @todo Add takeIfExists and readIfExists - useful for testing of txn locks
   Would require us to be able to load and create instances of classes other
   than just dummy, unless we do it with a null template which probably isn't
   enough.  Maybe we should just write dump tools.
 */
public class TxnMgrSim extends UnicastRemoteObject
    implements TransactionManager {

    private JavaSpace theSpace;
    private BufferedReader theInput =
        new BufferedReader(new InputStreamReader(System.in));

    // Map from variable name to variable value (an object)
    private HashMap theVariables = new HashMap();

    // Map from id to TxnData - used for TransactionParticipant interaction
    private HashMap theActiveTransactions = new HashMap();

    private TxnMgrSim(JavaSpace aSpace) throws RemoteException {
        super();
        theSpace = aSpace;
    }

    private void go() {
        System.out.println("Starting....");
        boolean isExiting = false;

        while (!isExiting) {
            try {
                System.out.print("> ");
                String myLine = prompt(theInput);
                StringTokenizer myTokens = new StringTokenizer(myLine, ", ");
                String myCommand = myTokens.nextToken();

                if (myCommand.equals("quit")) {
                    isExiting = true;
                    continue;
                } else if (myCommand.equals("n")) {
                    String myVar = myTokens.nextToken();

                    TxnData myRep = TxnData.newTxnData();
                    myRep.create(this);

                    theActiveTransactions.put(new Long(myRep.getId()),
                                              myRep);
                    theVariables.put(myVar, myRep);

                    System.out.println("Created txn with id: " +
                                       myRep.getId());
                } else if (myCommand.equals("p")) {
                    String myTxnVar = myTokens.nextToken();

                    TxnData myTxn = getTxnData(myTxnVar);

                    System.out.println("Preparing: " + myTxn.getId());

                    int myAnswer = 
                        myTxn.getParticipant().prepare(this, myTxn.getId());

                    System.out.println("Prepare returned: " + myAnswer);

                } else if (myCommand.equals("c")) {
                    String myTxnVar = myTokens.nextToken();

                    TxnData myTxn = getTxnData(myTxnVar);

                    System.out.println("Commiting: " + myTxn.getId());

                    myTxn.getParticipant().commit(this, myTxn.getId());

                    System.out.println("Commited");
                    clearTxn(myTxnVar);
                } else if (myCommand.equals("a")) {
                    String myTxnVar = myTokens.nextToken();

                    TxnData myTxn = getTxnData(myTxnVar);

                    System.out.println("Aborting: " + myTxn.getId());

                    myTxn.getParticipant().abort(this, myTxn.getId());

                    System.out.println("Aborted");
                    clearTxn(myTxnVar);
                } else if (myCommand.equals("t")) {
                    String myTxnVar = myTokens.nextToken();
                    String myTemplate = myTokens.nextToken();
                    String myWait = myTokens.nextToken();

                    Entry myResult =
                        theSpace.take(new TestEntry(myTemplate),
                                      getTxn(myTxnVar),
                                      Long.parseLong(myWait));

                    if (myResult == null)
                        System.out.println("took nothing");
                    else
                        System.out.println("took: " + myResult);
                } else if (myCommand.equals("r")) {
                    String myTxnVar = myTokens.nextToken();
                    String myTemplate = myTokens.nextToken();
                    String myWait = myTokens.nextToken();

                    Entry myResult =
                        theSpace.read(new TestEntry(myTemplate),
                                      getTxn(myTxnVar),
                                      Long.parseLong(myWait));

                    if (myResult == null)
                        System.out.println("read nothing");
                    else
                        System.out.println("read: " + myResult);
                } else if (myCommand.equals("w")) {
                    String myTxnVar = myTokens.nextToken();
                    String myEntryVal = myTokens.nextToken();
                    String myLease = myTokens.nextToken();

                    Lease myResult =
                        theSpace.write(new TestEntry(myEntryVal),
                                       getTxn(myTxnVar),
                                       Long.parseLong(myLease));
                }

            } catch (Exception anE) {
                System.err.println("Got exception in console");
                anE.printStackTrace(System.err);
            }
        }

        System.out.println("Done");
        System.exit(0);
    }

    private void clearTxn(String aVarName) throws Exception {
        TxnData myRep = getTxnData(aVarName);
        theVariables.remove(aVarName);
        theActiveTransactions.remove(new Long(myRep.getId()));
    }

    private ServerTransaction getTxn(String aVarName) throws Exception {
        if (aVarName.equals("null"))
            return null;

        TxnData myTxn = (TxnData) theVariables.get(aVarName);

        if (myTxn == null)
            throw new Exception("No such txn variable");

        return myTxn.get();
    }

    private TxnData getTxnData(String aVarName) throws Exception {
        TxnData myTxn = (TxnData) theVariables.get(aVarName);

        if (myTxn == null)
            throw new Exception("No such txn variable");

        return myTxn;
    }

    public String prompt(BufferedReader aReader) throws IOException {
        return aReader.readLine();
    }

    public Created create(long lease) throws LeaseDeniedException,
                                             RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 
    public void join(long id, TransactionParticipant part, long crashCount)
       	throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException {

        TxnData myRep = (TxnData) theActiveTransactions.get(new Long(id));

        if (myRep == null) {
            System.out.println("Participant: " + part + " attempted to join non-existent txn: " + id);
            throw new UnknownTransactionException();
        } else {
            System.out.println("Participant: " + part + " joining txn: " + id +
                               " with crash count: " + crashCount);
            myRep.join(part, crashCount);
            System.out.println("Participant: " + part + " joined txn: " + id +
                               " with crash count: " + crashCount);
        }
    }
 

    public int getState(long id) throws UnknownTransactionException,
                                        RemoteException {

        TxnData myRep = (TxnData) theActiveTransactions.get(new Long(id));

        if (myRep == null) {
            System.out.println("Participant did getState for non-existent transaction: " + id);
            throw new UnknownTransactionException();
        } else {
            System.out.println();
            System.out.print("getState for: " + id + " Return what state?");

            try {
                return Integer.parseInt(prompt(theInput));
            } catch (IOException anIOE) {
                throw new RemoteException();
            }
        }
    }
 
    public void commit(long id)
        throws UnknownTransactionException, CannotCommitException,
               RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 
    public void commit(long id, long waitFor)
        throws UnknownTransactionException, CannotCommitException,
               TimeoutExpiredException, RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 

    public void abort(long id)
        throws UnknownTransactionException, CannotAbortException,
               RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 
    public void abort(long id, long waitFor)
        throws UnknownTransactionException, CannotAbortException,
               TimeoutExpiredException, RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 

    /**
       Accepts a space name to lookup using SpaceFactory

       @todo Fix this to do a space lookup!!!
    */
    public static void main(String args[]) {

        try {
            // JavaSpace mySpace = SpaceFactory.getSpace(args[0]);
            JavaSpace mySpace = null;

            new TxnMgrSim(mySpace).go();

        } catch (Exception anE) {
            System.err.println("Failed to start TxnConsole");
            anE.printStackTrace(System.err);
        }
    }
}
