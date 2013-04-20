package org.dancres.blitz.test;

import net.jini.core.entry.Entry;

public class AlertEntry
	implements Entry{

	public String key;
	public String message;
	public Integer ID;
	//this field is here only to illustrate snapshot
	//the more fields you add you should see a bigger
	//performance gain from using snapshot
	public String [] silly={
		"This","is","an","example","to","show","that",
		"large","entries","perform","better","with","snapshot"
	};
	
	public AlertEntry(){
		//required public no-args ctor
	}	
	public AlertEntry(String aKey,String aMsg,Integer aID){
		key=aKey;
		message=aMsg;
		ID=aID;
	}
	public String toString(){
		return getClass()+"key="+key+" message="+message+" ID="+ID;
	}
}
