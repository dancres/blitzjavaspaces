package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

public class TakeSnapshot{
	
	/*
	* The JavaSpace snapshot() method provides an optimization by
	* pre-serializing an Entry's fields. 
	*
	* Try changing the value in nEntries to see how the optimization performs
	*
	* IMPORTANT NOTE: the more key fields you need to match on
	* the greater the performance benefit of using snapshot
	* for this example we are intentionally using a long string field
	* to illustrate this.
	* If you remove the "silly" field from the AlertEntry and re-run the tests
	* you should see that the gain from using snapshot is diminished.
	*		
	* Also try adding more fields to AlertEntry and assign them in both the
	* Entry written to the space and the Entry template used to take()
	*		
	*/
	
	
    public static void main(String [] args){
        try{
               	
        	System.out.println("Lookup JavaSpace...");
        	
            //first lookup a JavaSpace instance
            final long WAIT_FOR=5000L; 
            LocalSpace space=new LocalSpace(null);
            JavaSpace mySpace = space.getProxy();
            
			int nEntries=3000;
			
			Integer myID=new Integer(123);
			String msgKey="KEY_FIELD_VALUE";
			AlertEntry entry=new AlertEntry(msgKey,"a message",myID);	
			
			System.out.println("\n--- TEST 1 using a standard Entry template ---");
            System.out.println("Writing "+nEntries+" entries to the JavaSpace...");
	
           	for(int i=0;i<nEntries;i++){
           		mySpace.write(entry,null,Long.MAX_VALUE);
			}
						
			Entry tmpl=new AlertEntry(msgKey,null,myID);

			System.out.println("Taking entries...");
			
		    //Test 2: see how long it takes to do N takes
            long start=System.currentTimeMillis();
            for(int i=0;i<nEntries;i++){
            	mySpace.take(tmpl,null,0);
            }
            long end=System.currentTimeMillis();
            
            long t1Time=(end-start);
            
            System.out.println("Time taken to perform "+nEntries+" takes ="+t1Time+" millis\n");
            
            //Test 2: use a sanpshot
            System.out.println("--- TEST 2 using a snapshot Entry template ---");
            System.out.println("Writing "+nEntries+" entries to the JavaSpace...");
	
    	    for(int i=0;i<nEntries;i++){				
				mySpace.write(entry,null,Long.MAX_VALUE);
			}
		
            Entry snapshot=mySpace.snapshot(tmpl);
            
            start=System.currentTimeMillis();
            for(int i=0;i<nEntries;i++){
            	mySpace.take(snapshot,null,0);
            }
            end=System.currentTimeMillis();
            long t2Time=(end-start);
            
            System.out.println("\nSnapshot time taken to perform "+nEntries+" takes="+t2Time+" millis\n");
            
            long diff=t1Time-t2Time+1;
            
            System.out.println("Test result\nsnapshot is "+diff+" millis faster for "+nEntries+" takes\n");
            
            space.stop();
        }catch(Exception ex){
            ex.printStackTrace(System.err);
        }
    }
   
}
