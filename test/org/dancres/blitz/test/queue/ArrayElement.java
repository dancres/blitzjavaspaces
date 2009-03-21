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

import java.io.Serializable;

public class ArrayElement
  implements net.jini.core.entry.Entry{

    public String _name;
    public Integer _index;
    public Serializable _data;
    
    //required no-arg constructor
    public ArrayElement(){
    }
    //constructor for matching
    public ArrayElement(String name){
        this(name,null,null);
    }
    //constructor for matching
    public ArrayElement(String name,Integer index){
        _name=name;
        _index=index;
    }
    //constructor for creation
    public ArrayElement(String name,Integer index,Serializable data){
        this(name,index);
        _data=data;
    }
    
}
