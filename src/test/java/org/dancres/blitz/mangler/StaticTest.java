package org.dancres.blitz.mangler;

import net.jini.core.entry.Entry;

public class StaticTest {
    private void test(Entry anEntry) {
        try {
            EntryMangler myMangler = new EntryMangler();
            MangledEntry myEntry = myMangler.mangle(anEntry);
            
            myEntry.dump(System.out);

            Entry myNewEntry = myMangler.unMangle(myEntry);

            System.out.println("Unpacked result....");
            System.out.println(myNewEntry);
        } catch (Exception anE) {
            System.err.println("Failed");
            anE.printStackTrace(System.err);
        }
    }

    public static void main(String args[]) {
        new StaticTest().test(new ArrayMetaData("name", ArrayMetaData.HEAD,
                                                12));
    }

    public static class ArrayMetaData
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
        public String toString() {
            return super.toString() + ", " + _name + ", " + _index + ", " +
                _type;
        }
    }
}
