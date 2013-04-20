package org.dancres.blitz.tools.dash;

/*
*  This source code has been to contributed to the Blitz project by Inca X
*  and is based on the Outrigger Space Browser functionality in the
*  Inca X service browser.
*  http://www.incax.com/service-browser.htm
*/

import com.sun.jini.outrigger.AdminIterator;
import com.sun.jini.outrigger.JavaSpaceAdmin;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.lang.reflect.Field;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.table.AbstractTableModel;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.core.transaction.TransactionException;

public class OutriggerViewer extends JPanel
implements Runnable{
    
    /*
    * Reference to the JavaSpaceAdmin proxy
    */
    private JavaSpaceAdmin javaSpaceAdmin;
    /*
    * inner class that extends AbstractTableModel
    */
    private EntryCountTableModel entryCountTableModel =new EntryCountTableModel();
    /*
    * refreshRate
    * how often to poll the admin proxy.
    */
    private long refreshRate=5000;
    /*
    * paused
    * if set by the GUI then polling is suspended
    */
    private boolean paused=false;
    
    private final String UNUSABLE_ENTRY="<unusable entry>";
    
    //private static int maxEntries=100;
    
    private Thread _updater;
    private JEditorPane htmlView=new JEditorPane();
    private JScrollPane introView=new JScrollPane(htmlView);
    private boolean firstTime=true;
    private JDialog parent;
    private ActionListener updateAction;
    private boolean entriesDeleted;
    /*
    * Construct the OutriggerView
    */
    public OutriggerViewer(JDialog comp,JavaSpaceAdmin jsa){
        javaSpaceAdmin=jsa;
        
        // System.out.println("JavaSpaceAdmin="+javaSpaceAdmin);
        
        parent=comp;
        initUI();
    }
    /**
    *  Initialize the GUI and start the monitor thread
    *  which polls the JavaSpaceAdmin for the entry iterator
    */
    private void initUI(){
        setLayout(new BorderLayout());
        
        add( createControls(),BorderLayout.SOUTH);
        
    }
    
    public void removeNotify(){
        super.removeNotify();
        if(_updater!=null){
            _updater.interrupt();
        }
    }
    
    /*
    * Poll the JavaSpaceAdmin proxy every <code>refreshRate</code> millis
    * If we get an Exception, drop out of the loop and report it
    * The most likely cause of an Exception occuring will be down to
    * a retrieved entry not having a valid codebase set for it
    *
    * If the client application that is writing the entries to the space
    * is being run from within the Inca X environment, makes sure you have
    * set a codebase alias, but right clicking on the project icon and selecting
    * "Add alias to webserver" and then selecting "Yes" when prompted.
    */
    public void run(){
        // System.out.println("Starting Outrigger reader thread");
        
        while(!_updater.isInterrupted()){
            try{
                
                doUpdates();
                
                Thread.sleep(refreshRate);
                
            }catch(InterruptedException ex){
                // System.out.println("Stopping Outrigger reader thread");
                _updater=null;
                return;
            }catch(Exception ex){
                //drop out here if we get an exception,
                //likely causes are UnusableEntryExceptions
                //because the client program that wrote the entry
                //didn't set a codebase (or set a wrong one) or the HTTPD of the actual
                //codebase isn't working
                //JOptionPane.showMessageDialog(this,ex);
                ex.printStackTrace();
                _updater=null;
                return;
            }
        }
        
    }
    /**
    * Request all the entries currently in the space.
    * @throws  TransactionException,RemoteException,UnusableEntryException
    */
    public void doUpdates()
        throws TransactionException,UnusableEntryException,RemoteException{
        
        if(firstTime){
            firstTime=false;
            JTable table=new JTable(entryCountTableModel);
            table.setToolTipText("Double click row to view values");
            JScrollPane scrollPane=new JScrollPane(table);
            remove(introView);
            add(scrollPane,BorderLayout.CENTER);
            invalidate();
            getParent().validate();
            addMouseListener(table);
            
        }
        //here we're asking for all the entries in the space
        AdminIterator iter=javaSpaceAdmin.contents(null/*Entry template*/,null/*Transaction*/);
        
        List list=new ArrayList();
        Map entryMap=new HashMap();
        int ueCount=0;
        int got=0;
        while(true){
            try{
                Entry e=iter.next();
                if(e==null)/* || got>=maxEntries)*/{
                    break;
                }
                got++;
                Class entryClass=e.getClass();
                
                String entryClassName=entryClass.getName();
                
                Object [] data=(Object[])entryMap.get(entryClassName);
                if(data==null){
                    Object template=null;
                    try{
                        template=entryClass.newInstance();
                        
                    }catch(Exception ex){
                        System.out.println(ex);
                    }
                    entryMap.put(entryClassName,new Object[]{entryClassName,new Integer(1),template});
                }else{
                    Integer count=(Integer)data[1];
                    data[1]=new Integer(count.intValue()+1);
                }
                
            }catch(UnusableEntryException uee){
                //add as UnusableEnrty
                ueCount++;
            }
        }
        
        iter.close();
        
        if(ueCount>0){
            entryMap.put(UNUSABLE_ENTRY,new Object[]{UNUSABLE_ENTRY,new Integer(ueCount),null});
        }
        //get the object counts
        Collection col=entryMap.values();
        List counterList=new ArrayList();
        counterList.addAll(col);
        
        entryCountTableModel.update(counterList);
    }
    /*
    * Create the buttons that get displayed at the bottom of the GUI
    */
    private JComponent createControls(){
        final JPanel panel=new JPanel();
        
        final JButton update=new JButton("Get entries");
        updateAction=new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                
                //maxEntries=Integer.parseInt(max.getText().trim());
                Thread t=new Thread(){
                    public void run(){
                        try{
                            update.setEnabled(false);
                            
                            ArrayList tmp=new ArrayList();
                            tmp.add( new Object[]{"Loading...",""});
                            entryCountTableModel.update(tmp);
                            doUpdates();
                            
                        }catch(Exception ex){
                            entryCountTableModel.update(new ArrayList());
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(panel,ex);
                        }finally{
                            update.setEnabled(true);
                            update.setText("Refresh");
                        }
                    }
                };
                t.start();
                
            }
        };
        update.addActionListener(updateAction );
        panel.add(update);
        return panel;
    }
    /*
    * Table model for the entry data retrieved in doUpdate()
    */
    private class EntryCountTableModel extends AbstractTableModel{
        private List _data=new ArrayList();
        
        private String []_cols=new String[]{"Entry Type","Instance Count"};
        
        public int getRowCount(){
            return _data.size();
        }
        public int getColumnCount(){
            return 2;
        }
        public String getColumnName(int col){
            return _cols[col];
        }
        public Object getValueAt(int r,int c){
            Object [] rowData=(Object[])_data.get(r);
            
            return rowData[c];
        }
        public void update(List data){
            _data=data;
            fireTableDataChanged();
        }
    }
    private void addMouseListener(final JTable table){
        table.addMouseListener( new MouseAdapter(){
            public void mouseClicked(MouseEvent evt){
                if(evt.getClickCount()!=2){
                    return;
                }
                int sel=table.getSelectedRow();
                //disable if running updates
                if(sel==-1 || _updater!=null){
                    return;
                }
                showEntryBrowser(sel);
            }
        });
    }
    private void showEntryBrowser(int row){
        try{
            Object tmpl=entryCountTableModel.getValueAt(row,2);
            
            if(tmpl==null){
                return;//unusable entry count
            }
            final AdminIterator iter=javaSpaceAdmin.contents((Entry)tmpl,null);
            
            entriesDeleted=false;
            Frame frame=JOptionPane.getFrameForComponent(parent);
            final String title=tmpl.getClass().getName();
            final JDialog dlg=new JDialog(frame,title,true);
            final WindowListener wl=new WindowAdapter(){
                public void windowClosing(WindowEvent evt){
                    try{
                        iter.close();
                        dlg.dispose();
                        if(entriesDeleted){
                            updateAction.actionPerformed(null);
                        }
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            };
            dlg.addWindowListener(wl);
            
            JComponent view=createEntryPanel(iter,wl);
            
            dlg.getContentPane().add(view,BorderLayout.CENTER);
            dlg.setSize(400,300);
            Rectangle bounds=parent.getBounds();
            dlg.setLocation(bounds.x+20,bounds.y+20);
            dlg.setVisible(true);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,ex);
        }
    }
    private class EntryPropsTable extends AbstractTableModel{
        
        private ArrayList _data=new ArrayList();
        
        EntryPropsTable(Object entry){
            parseEntry(entry);
        }
        
        private String []_cols=new String[]{"Type","Name","Value"};
        
        public int getRowCount(){
            return _data.size();
        }
        public int getColumnCount(){
            return 3;
        }
        public String getColumnName(int col){
            return _cols[col];
        }
        public Object getValueAt(int r,int c){
            Object [] rowData=(Object[])_data.get(r);
            
            return rowData[c];
        }
        public void update(Object nextEntry){
            _data=new ArrayList();
            parseEntry(nextEntry);
            fireTableDataChanged();
        }
        private void parseEntry(Object entry){
            try{
                Class ec=entry.getClass();
                Field [] f=ec.getFields();
                for(int i=0;i<f.length;i++){
                    Object [] fData={
                        f[i].getType().getName(),
                        f[i].getName(),
                        f[i].get(entry)
                    };
                    _data.add(fData);
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
        public void clear(){
            _data=new ArrayList();
            
            fireTableDataChanged();
        }
    }
    private JComponent createEntryPanel(final AdminIterator iter,final WindowListener wl)
        throws Exception{
        final JPanel jp=new JPanel();
        jp.setLayout( new BorderLayout() );
        
        final EntryPropsTable model=new EntryPropsTable(iter.next());
        
        JTable jt=new JTable(model);
        
        jp.add(new JScrollPane(jt),BorderLayout.CENTER);
        //add ctrls
        final JButton next=new JButton(" Next ");
        final JButton del =new JButton("Delete");
        final JButton close =new JButton("Close ");
        final ActionListener nextAl=new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                try{
                    
                    Object nextEntry=iter.next();
                    if(nextEntry==null){
                        next.setEnabled(false);
                        del.setEnabled(false);
                        model.clear();
                    }else{
                        model.update(nextEntry);
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        };
        next.addActionListener(nextAl);
        
        del.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                try{
                    int ok=JOptionPane.showConfirmDialog(jp,"Are you sure you want to delete this entry?"
                    ,"Delete Entry",
                    JOptionPane.YES_NO_OPTION);
                    if(ok==JOptionPane.YES_OPTION){
                        iter.delete();
                        entriesDeleted=true;
                    }
                    nextAl.actionPerformed(null);
                    
                }catch(Exception ex){
                    JOptionPane.showMessageDialog(jp,ex);
                }
            }
        });
        close.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                wl.windowClosing(null);
            }
        });
        JPanel ctrls=new JPanel();
        
        ctrls.add(del);
        ctrls.add(next);
        ctrls.add(close);
        jp.add(ctrls,BorderLayout.SOUTH);
        return jp;
    }
    
}

