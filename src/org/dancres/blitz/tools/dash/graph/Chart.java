package org.dancres.blitz.tools.dash.graph;

import org.dancres.blitz.tools.dash.ColorScheme;

import java.awt.*;
import java.text.*;
import javax.swing.*;

public class Chart extends JPanel
    implements ChartItemEnabler
{
    
    private class Data
    {
        double  points[];
        String  label;
        boolean isVisible=true;
        double [] pointFactor;
        String [] tags;
        int [] date;
        
        public int getSize() { return points.length;}
        public double getPoint(int p) { return points[p];}
        public double getFactor(int f) { return pointFactor[f];}
        public String getTag(int f) { return tags[f];}
    }
    private static final Color [] chartColor={ColorScheme.READ,
                                              ColorScheme.WRITE,
                                              ColorScheme.TAKE};
    
    //constraints
    private static final int MAX_DATA_SETS=chartColor.length;
    
    private int dsCounter=0;
    private Data [] dataSet= new Data[MAX_DATA_SETS];
    private int labelEvery=1;
    //
    //identifier mappings
    private static final int SOLID=0;
    private static final int OUTLINE=1;
    private static NumberFormat  formatter = NumberFormat.getInstance();
    
    private IdDrawer [] idDrawer= new IdDrawer[MAX_DATA_SETS];
    
    private int maxSize=0;
    private double maxValue=0.0;
    private double minValue=0.0;
    private int maxId; //index to largest dataset
    private int startAt=0; //for drawing to endAt
    private int endAt=-1; //reset in zoom
    private int [] xTagPosition;
    
    private Color axisColor=Color.black;
    private Color gridColor=Color.gray;
    private int yOffset=25;
    private boolean dirtyCache;
    private Dimension minSize=new Dimension(200,200);
    private Dimension currentSize=null;
    private Rectangle chartRect= new Rectangle();
    private Rectangle zoomRect;
    private boolean  isShown=false; //at least one dataset visiable

    //public methods
    public Chart()
    {
        idDrawer[0]=new SqDrawer(SOLID);
        idDrawer[1]=new XDrawer();
        idDrawer[2]=new RoundRectDrawer(SOLID);
        
        
    }

    synchronized public void addData(final String name,final double [] data,
                                     final String [] tags)
        throws ChartException
    {
        addData(name,data,tags,null);
    }

    synchronized public void addData(final String name,final double [] data,
                                     final String [] tags,final int [] dates)
        throws ChartException
    {
        if(dsCounter==MAX_DATA_SETS)
            throw new ChartException("Maximum number of data sets exceeded!");
        
        setDataAt(dsCounter++,name,data,tags,dates);
    }

    public void setDataAt(int index,final String name,final double [] data,
                          final String [] tags,final int [] dates)
        throws ChartException
    {
        dirtyCache=true; //flag that calcFactors must be called internally
        endAt=-1;
        
        Data d= new Data();
        d.points=data;
        d.label=name;
        d.tags=tags;
        if(dates!=null)
            d.date=dates;
        else
            {
                //set to defaults
                int np=data.length;
                d.date= new int[np];
                for(int i=0;i<np;i++)
                    d.date[i]=i;
            }
        d.pointFactor= new double[ tags.length ];
        dataSet[ index ]=d;
    }
    public void setLabelEvery(int every) {labelEvery=every;}

    public static int getMaxSupportedDataSets() {return MAX_DATA_SETS;}

    public Dimension getMinimumSize()
    {
        return minSize;
    }

    public Dimension getPreferredSize()
    {
        if(currentSize==null)
            currentSize= getSize();
        
        return currentSize;
    }

    public void setSize(Dimension d)
    {
        super.setSize(d);
        currentSize=d;
    }
    
    public void print(final Graphics g,final int yoffset)
    {
        int ty=yOffset;
        yOffset=yoffset;
        super.print(g);
        yOffset=ty;
    }

    public void enableData(final String name,final boolean yesno)
    {
        for( int i=0;i<dsCounter;i++)
            {
                if(dataSet[i].label.compareTo(name)==0)
                    {
                        dataSet[i].isVisible=yesno;
                        dirtyCache=true;
                        repaint();
                        return;
                    }
            }
    }

    public void paint(Graphics g)
    {
        Dimension dim=getSize();
        g.setColor(Color.white);
        g.fillRect(0,0,dim.width,dim.height);
        
        if(dirtyCache)
            {
                calcFactors();
                dirtyCache=false;
            }
        g.setFont( new Font("Dialog",Font.PLAIN,10) );
        
        if(dim.height<50) //to small
            {
                return;
            }
        int xoff=45;
        int yoff=yOffset; //set in print(), default==25
        int wid=dim.width-(2*xoff);
        int hi=dim.height-(2*yoff);
        int baseY=15;
        int legy=baseY; //title pos
        int legx=xoff;
        
        FontMetrics fm=g.getFontMetrics();
        
        isShown=false;
        boolean firstchart=true;
        int ns=dsCounter;
        double prevx=0;//last place vert grid line drawn
        double lastLabelPos=-1000;//make sure first label is always drawn
        //Draw dataset titles, then calc size of graphRect
        for(int i=0;i<ns;i++)
            {
                if(dataSet[i].isVisible)
                    {
                        g.setColor( chartColor[i] );
                
                        String dslabel=dataSet[i].label;
                        int strWidth=fm.stringWidth(dslabel);
                        if((strWidth+legx)>(wid-5) && firstchart==false) //CR
                            {
                                legx=xoff;legy+=15;
                            }
                        firstchart=false;
                        //draw identifier
                        idDrawer[i].draw(g,legx-2,legy-7);
                        //draw label
                        g.drawString(dslabel,legx+5,legy);
                
                        legx+=strWidth+12;
                    }
            }

        //now calc the drawable area for the chart
        // draw zoomable area
        yoff=legy+15;
        hi-=(legy-baseY)+5;
        
        if(zoomRect!=null)
            {
                g.setColor(Color.lightGray);
                g.fillRect(zoomRect.x,yoff,zoomRect.width,hi);
            }

        //border
        g.setColor(axisColor);
        g.drawRect(xoff,yoff,wid,hi);

        //cache for zooming
        chartRect.setBounds(xoff,yoff,wid,hi);

        //draw the points
        for(int i=0;i<ns;i++)
            {
                if(dataSet[i].isVisible)
                    {
                        g.setColor( chartColor[i] );
                        int labelWidth=fm.stringWidth(dataSet[i].getTag(0));
                
                        isShown=true;
                        int npoints=dataSet[i].getSize();
                        double lastx=0;//=xoff;
                        double lasty=0;//yoff+hi;
                        int labelCounter=labelEvery; //make sure first label is drawn
                
                        for(int j=startAt;j<npoints && j<=endAt;j++)
                            {
                                double val=dataSet[i].getPoint(j);
                                double xpos=xoff+(wid*dataSet[i].getFactor(j));
                                double yfactor=
                                    (val-minValue)/(maxValue-minValue);
                                double ypos=yoff+(hi*(1-yfactor));
                    
                                if(lastx==0 && lasty==0)
                                    {
                                        //set to current
                                        lastx=xpos;lasty=ypos;
                                    }
                                if(xpos>(prevx+25))
                                    {
                                        g.setColor(gridColor);
                                        g.drawLine((int) xpos,yoff,
                                                   (int) xpos,yoff+hi+2);
                                        g.setColor(axisColor);
                                        if(labelCounter%labelEvery==0 &&
                                           xpos>(lastLabelPos+labelWidth+10))
                                            {
                                                xTagPosition[j]=(int)xpos-5;
                                                g.drawString(dataSet[i].getTag(j),xTagPosition[j],yoff+hi+20);
                                                lastLabelPos=xpos;
                                            }
                                        else
                                            {
                                                xTagPosition[j]=-1; //invalid
                                            }
                                        g.setColor(chartColor[i]);
                                        prevx=xpos;
                                    }
                    
                                g.drawLine((int)lastx,(int)lasty,(int)xpos,
                                           (int)ypos);
                                //draw identifier
                                //g.drawRect((int)xpos-1,(int)ypos-1,2,2);
                                idDrawer[i].draw(g,(int)xpos-2,(int)ypos-2);
                                lastx=xpos;
                                lasty=ypos;
                                labelCounter++;
                            }
                
                    }
            
            }
        if(isShown)
            {
            
            
                double inc=(maxValue-minValue)/4;//number of labels (could be set by client)
                double label=maxValue;
                double y=yoff;
        
                //System.out.println("maxValue="+maxValue+"minValue="+minValue+" inc="+inc);
        
                String str=null;
                String lastStr=null;
            
                //count number of labels to display
                //added version 1.1
                java.util.ArrayList labels=new java.util.ArrayList();
            
                for(int yaxis=0;yaxis<5;yaxis++)
                    {
                
                        str=""+(int)label;//formatter.format(label);
                        int strLen=str.length();
                        for(int i=strLen;i<7;i++){
                            str=" "+str;
                        }
                        if(lastStr==null || lastStr.equals(str)==false){
                            labels.add(str);
                        }
                
                        lastStr=str;
                
                        label-=inc;
                    }
                int nLabels=labels.size();
                        
                for(int yaxis=0;yaxis<nLabels;yaxis++)
                    {
                        g.setColor( axisColor );
                        str=labels.get(yaxis).toString();
                        g.drawString( str,xoff-45,(int)y+6);
                        g.setColor( gridColor );
                        if(yaxis<nLabels-1){
                            g.drawLine(xoff-3,(int)y,xoff+wid,(int)y);  
                        }
                
                        y+=(hi/(nLabels-1.0));
                
                    }
            
            }
        else
            {
                g.drawString("no data",legx,legy);
            }
    }
    
    //Private impl
    synchronized private void calcFactors()
    {
        int tsize=0;
        double maxdate=0;
        double mindate=Double.MAX_VALUE;
        //reset MaxValue
        maxValue=0.0;
        maxSize=0;
        minValue=Double.MAX_VALUE;
        int endpos;
        int startpos;
        //find the largest dataset, and max dates
        for(int i=0;i<dsCounter;i++)
            {
                if(dataSet[i].isVisible)
                    {
                        tsize=dataSet[i].getSize();
                        if(tsize>maxSize)
                            {
                                maxSize=tsize;
                                maxId=i;
                            }
                        endpos= tsize-1;
                        if(endAt!=-1 && endAt<tsize)
                            endpos=endAt;
                
                        startpos=startAt;
                        if(startpos>=tsize)
                            startpos=endpos;
                
                        int md=dataSet[ i ].date[ endpos ];
                        int mind=dataSet[ i ].date[ startpos ];
                        maxdate=md>maxdate?md:maxdate;
                        mindate=mind<mindate?mind:mindate;
                    }
            }
        if(endAt==-1)//first time
            {
                endAt=maxSize;
            }
        //allocate xTagPos here - NOT in paint()
        xTagPosition= new int[maxSize];
        //calculate the factors
        for( int i=0;i<dsCounter;i++)
            {
                if(dataSet[i].isVisible)
                    {
                        int npoints=dataSet[i].getSize();
                        for(int j=startAt;j<npoints && j<=endAt;j++)
                            {
                                double date=dataSet[i].date[j];
                                dataSet[i].pointFactor[j]=
                                    (date-mindate)/(maxdate-mindate);
                                double val=dataSet[i].getPoint(j);
                                maxValue=maxValue>val ? maxValue : val;
                                minValue=val<minValue?val:minValue;
                            }
                    }
            }
        maxValue=adjustMax(maxValue);
        minValue=adjustMin(minValue);
    
    }

    private double adjustMax(double d)
    {
        double x=d;
        if(x>0)
            x+=0.05;
        else
            x-=0.05;
        
        return Math.rint(x*10)/10;
    }

    private double adjustMin(double d)
    {
        double x=d;
        x-=0.05;
        return Math.rint(x*10)/10;
    }

    //private impl classes internal double-dispatch
    private interface IdDrawer
    {
        public void draw(final Graphics g,final int xpos,final int ypos);
    }
    
    private class SqDrawer
        implements IdDrawer
    {
        private int drawMode;
        
        public SqDrawer(final int mode)
        {
            drawMode=mode;
        }
        public void draw(final Graphics g,final int xpos,final int ypos)
        {
            if(drawMode==SOLID)
                g.fillRect(xpos+1,ypos,4,4);
            else
                g.drawRect(xpos,ypos,4,4);
        }
    }

    private class RoundRectDrawer
        implements IdDrawer
    {
        private int drawMode;
        
        public RoundRectDrawer(final int mode)
        {
            drawMode=mode;
        }
        public void draw(final Graphics g,final int xpos,final int ypos)
        {
            if(drawMode==SOLID)
                g.fillRoundRect(xpos+1,ypos,4,4,4,4);
            else
                g.drawRoundRect(xpos+1,ypos,4,4,4,4);
        }
    }

    private class XDrawer
        implements IdDrawer
    {
        public XDrawer()
        {
        }
        public void draw(final Graphics g,final int xpos,final int ypos)
        {
            int os=2;
            
            g.drawLine(xpos-2+os,ypos-2+os,xpos+2+os,ypos+2+os);
            g.drawLine(xpos+2+os,ypos-2+os,xpos-2+os,ypos+2+os);
        }
    }
}

