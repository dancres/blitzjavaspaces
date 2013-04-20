/*
*  This software is intended to be used for educational purposes only.
*
*  We make no representations or warranties about the
*  suitability of the software.
*
*  Any feedback relating to this software or the book can
*  be sent to jsip@jsip.info
*
*  For updates to this and related examples visit www.jsip.info
*
*  JavaSpaces in Practice
*  by Philip Bishop & Nigel Warren
*  Addison Wesley; ISBN: 0321112318
*
*/

package org.dancres.blitz.test.queue;

import java.rmi.RemoteException;

import java.util.Iterator;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;

import net.jini.core.transaction.server.TransactionManager;

import net.jini.space.JavaSpace;

public class ExpandableArray{

    private static final long POLL_TIME = 500;

    private JavaSpace _space;
	
	private TransactionManager _txnMgr;
	
	private String _name;
	
	/**
	* Simple constuctor for testing without Txn mgr
	*/
	
	public ExpandableArray(String name,JavaSpace space){
		this(name,space,null);
	}
	/**
	* Robust constuctor for testing with Txn mgr
	*/
	
	public ExpandableArray(String name,JavaSpace space,TransactionManager txnMgr){
		_name=name;
		_space=space;
		_txnMgr=txnMgr;
	}
	/**
	* Create the array
	*/
	
	public void create()
		throws RemoteException{
		
		try{
			Transaction txn=createTxn();
			
			ArrayMetaData head=new ArrayMetaData(_name,ArrayMetaData.HEAD,0);
			ArrayMetaData tail=new ArrayMetaData(_name,ArrayMetaData.TAIL,0);
			
			//assume lease is granted
			//for correctness use a lease renewal manager
			_space.write(head,txn,Lease.FOREVER);
			_space.write(tail,txn,Lease.FOREVER);
			
			if(txn!=null){
				txn.commit();
			}

		}catch(Exception ex){
			throw new RemoteException("create failed",ex);
		}
	}
	/**
	* Delete the array and all elements
	*/
	public void delete()
		throws RemoteException{
		
		try{
            Transaction txn = createTxn();

            ArrayMetaData head=new ArrayMetaData(_name,ArrayMetaData.HEAD);
			ArrayMetaData tail=new ArrayMetaData(_name,ArrayMetaData.TAIL);
			
			_space.take(head,txn,POLL_TIME);
			_space.take(tail,txn,POLL_TIME);
			
			ArrayElement tmpl=new ArrayElement(_name);
			//debug
			int remCount=0;
			while(_space.take(tmpl,txn,POLL_TIME)!=null){
				remCount++;
			}
			
			if(txn!=null){
				txn.commit();
			}

		}catch(Exception ex){
			throw new RemoteException("create failed",ex);
		}
	}
	/**
	* Append an element to the array
	*/
	public void add(java.io.Serializable data)
		throws RemoteException{
		
		try{
			Transaction txn=createTxn();
			
			ArrayMetaData tmpl=new ArrayMetaData(_name,ArrayMetaData.TAIL);
			
			ArrayMetaData tail=(ArrayMetaData)_space.take(tmpl,txn,POLL_TIME);
			while(tail==null){
				tail=(ArrayMetaData)_space.take(tmpl,txn,POLL_TIME);
			}
			Integer index=tail.getIndex();
			tail.increment();
			ArrayElement e=new ArrayElement(_name,index,data);
			
			//assume lease is granted
			//for correctness use a lease renewal manager
			_space.write(e,txn,Lease.FOREVER);
			_space.write(tail,txn,Lease.FOREVER);
			
			if(txn!=null){
				txn.commit();
			}

		}catch(Exception ex){
			throw new RemoteException("add failed",ex);
		}
	}

    public boolean peek() throws RemoteException {
        try {
            Transaction txn = createTxn();

            ArrayMetaData headTmpl =
                new ArrayMetaData(_name, ArrayMetaData.HEAD);

            ArrayMetaData head =
                (ArrayMetaData) _space.read(headTmpl, txn, POLL_TIME);

            while (head == null) {
                head = (ArrayMetaData) _space.read(headTmpl, txn, POLL_TIME);
            }

            ArrayMetaData tailTmpl =
                new ArrayMetaData(_name, ArrayMetaData.TAIL);

            ArrayMetaData tail =
                (ArrayMetaData) _space.read(tailTmpl, txn, POLL_TIME);

            while (tail == null) {
                tail = (ArrayMetaData) _space.read(tailTmpl, txn, POLL_TIME);
            }

            if (txn != null)
                txn.commit();

            return (head.getIndex() != tail.getIndex());

        } catch (Exception anE) {
            throw new RemoteException("Failed to pop", anE);
        }
    }

    public java.io.Serializable pop() throws RemoteException {
        try {
            Transaction txn = createTxn();

            ArrayMetaData tmpl = new ArrayMetaData(_name, ArrayMetaData.HEAD);
            ArrayMetaData head = (ArrayMetaData) _space.take(tmpl, txn, POLL_TIME);

            while (head == null) {
                head = (ArrayMetaData) _space.take(tmpl, txn, POLL_TIME);
            }

            Integer myIndex = head.getIndex();

            ArrayElement valueTmpl = new ArrayElement(_name, myIndex);

            ArrayElement myNext = (ArrayElement)
                _space.take(valueTmpl, txn, POLL_TIME);

            while (myNext == null) {
                myNext = (ArrayElement) _space.take(valueTmpl, txn, POLL_TIME);
            }

            head.increment();

            _space.write(head, txn, Lease.FOREVER);

            if (txn != null) {
                txn.commit();
            }

            return myNext;
        } catch (Exception anE) {
            throw new RemoteException("Failed to pop", anE);
        }
    }

    /**
	* Return an iterator over the array
	* @param timeout max time to wait for next entry
	*/
	public Iterator iterator(long timeout)
		throws RemoteException{
		
		try{
			
			ArrayMetaData tmpl=new ArrayMetaData(_name,ArrayMetaData.HEAD);
			ArrayMetaData head=(ArrayMetaData)_space.read(tmpl,null,POLL_TIME);
			while(head==null){
				head=(ArrayMetaData)_space.read(tmpl,null,POLL_TIME);
			}
			Integer startIndex=head.getIndex();
			return new IteratorImpl(timeout,startIndex);
			
		}catch(Exception ex){
			throw new RemoteException("failed to create iterator",ex);
		}
		
	}
	private class IteratorImpl
	implements Iterator{
		
		private long _timeout;
		private Integer _index;
		private Object _next;
		
		public IteratorImpl(long timeout,Integer index){
			
			_timeout=timeout;
			_index=index;
		}
		
		public boolean hasNext(){
			try{
				
				ArrayElement tmpl=new ArrayElement(_name,_index);
				_index=new Integer(_index.intValue()+1);
				
				_next=_space.read(tmpl,null,_timeout);
				
				return _next!=null;
			}catch(Exception ex){
				
				return false;
			}
		}
		public Object next(){
			return ((ArrayElement) _next)._data;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	private Transaction createTxn()
		throws LeaseDeniedException, RemoteException{
		
		if(_txnMgr==null){
			return null;
		}
		//here we pick a default timeout of 1 minute
		//this could be a parameter or field
		Transaction.Created txnC=TransactionFactory.create(_txnMgr,60000);
		return txnC.transaction;
		
	}
}
