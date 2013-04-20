package org.dancres.blitz.tools.dash.graph;

public class ChartException extends Exception{
    public ChartException(String reason){
        super(reason);
    }
    public ChartException(String reason,Throwable cause){
        super(reason,cause);
    }
}
