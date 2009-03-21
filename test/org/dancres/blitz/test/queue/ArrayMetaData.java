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

public class ArrayMetaData
    implements net.jini.core.entry.Entry{

    public String _name;
    public Integer _index;
    public Integer _type;
    
    public static final Integer HEAD=new Integer(0);
    public static final Integer TAIL=new Integer(1);
    
    //required no-args constructor
    public ArrayMetaData(){
        
    }
    //for matching
    public ArrayMetaData(String name,Integer type){
        _name=name;
        checkType(type);
        _type=type;
    }
    //for creation
    public ArrayMetaData(String name,Integer type,int index){
        this(name,type);
        _index=new Integer(index);
    }
    //increment
    public void increment(){
        _index=
          new Integer(_index.intValue()+1);
    }
    public Integer getIndex(){
        return _index;
    }
    private void checkType(Integer type){
        if(type.equals(HEAD)==false 
            && type.equals(TAIL)==false){
       
            throw new IllegalArgumentException("Invalid type");
       }
    }
}