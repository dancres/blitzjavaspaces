package org.dancres.blitz.serviceui;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.entry.Name;

import net.jini.lookup.ui.MainUI;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.attribute.UIFactoryTypes;

import net.jini.core.lookup.ServiceItem;

import net.jini.core.entry.Entry;

import net.jini.admin.JoinAdmin;
import net.jini.admin.Administrable;

import net.jini.space.JavaSpace05;
import org.dancres.blitz.remote.StatsAdmin;
import org.dancres.blitz.tools.dash.DashBoardFrame;
import org.dancres.blitz.tools.dash.StartDashBoard;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.io.IOException;
import java.net.URL;
 
public class DashboardUI{
    static Logger theLogger =
        Logger.getLogger("org.dancres.blitz.serviceui.DashboardUI");

    public static UIDescriptor getUIDescriptor()
        throws IOException {
        
        UIDescriptor desc = new UIDescriptor();
        desc.role = MainUI.ROLE;
        desc.toolkit = JFrameFactory.TOOLKIT;
        desc.attributes=new HashSet();
        
        Set types = new HashSet();
        types.add(JFrameFactory.TYPE_NAME);
        types.add(JComponentFactory.TYPE_NAME);
        UIFactoryTypes factoryTypes=new UIFactoryTypes( types );
        
        desc.attributes.add(factoryTypes);
        
        desc.factory = new MarshalledObject(new Factory());
        return desc;
    }
    private static class Factory
        implements JFrameFactory, JComponentFactory{

        static final long serialVersionUID = -1750620586876278578L;

        //cache the current instance as it doesn't make any sense
        //have multiple dashboards connected to the same space
        private static DashBoardFrame _frame;
    	
        public JFrame getJFrame(Object roleObject){
            //if we are already running then return the
            //cached JFrame and bring it to the front
            if(_frame!=null){
                //TO DO:
                //Add a method to the Dashboard to bring all its child
                //frames to the front, by invoking toFront() on each child
                //e.g _frame.showWindows();
   			 		
                _frame.toFront();
                return _frame;
            }
            try{
                ServiceItem si=(ServiceItem)roleObject;
   			 		  			 		
                Administrable admin=(Administrable)si.service;
                StatsAdmin sa=(StatsAdmin)admin.getAdmin();
                JoinAdmin ja=(JoinAdmin)admin.getAdmin();
                
                String title=StartDashBoard.VER+" ["+getSpaceName(ja)+"]";
                boolean closeOnExit=false; //don't call System.exit()
                _frame = new DashBoardFrame(title, (JavaSpace05) si.service, sa,closeOnExit);
   			 		
                _frame.addWindowListener(new WindowAdapter(){
   			 			public void windowClosing(WindowEvent evt){
   			 				//set _frame to null so next time
   			 				//we create a new window
   			 				_frame=null;
   			 			}
   			 		});
                return _frame;
            }catch(RemoteException ex){
                ex.printStackTrace();
                JFrame f= new JFrame("Blitz Dashboard Exception");
   			 		
                f.getContentPane().add( new JLabel(""+ex));
                f.setSize(200,200);	
                f.setVisible(true);	
                return f;
            }
        }
        //JComponentFactory
        //return an html view
        public JComponent getJComponent(Object roleObject){
            try{
   			 		
                URL url=Factory.class.getResource("blitz.htm");
                JEditorPane htmlView=new JEditorPane(url);
                htmlView.setEditable(false);
                //set the AccessibleContext Name for this view
                //so the Inca X browser will display it
                JScrollPane sp=new JScrollPane(htmlView);
                sp.getAccessibleContext().setAccessibleName("Blitz dashboard");
                return sp;
            }catch(Exception ex){
                theLogger.log(Level.SEVERE, "Got exception", ex);
                return new JLabel(ex.toString());
            }
        }
        //get the space name from lookup attributes
        private String getSpaceName(JoinAdmin ja){
            try{
                
                Entry [] atts=ja.getLookupAttributes();
                for(int i=0;i<atts.length;i++){
                    if(atts[i] instanceof Name){
                        return ((Name)atts[i]).name;
                    }
                }
            }catch(Exception ex){
                theLogger.log(Level.SEVERE, "Dashboard failed to get spacename",
                        ex);
            }
            return "";
        } 
    }
}
