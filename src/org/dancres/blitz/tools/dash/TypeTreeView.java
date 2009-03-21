package org.dancres.blitz.tools.dash;

import javax.swing.ImageIcon; 
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing. JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;

import java.awt.BorderLayout;
import java.awt.Component;

import java.util.logging.Level;

class TypeTreeView extends JPanel{
    
    private DefaultMutableTreeNode _root=new DefaultMutableTreeNode("Entry");
    private JTree _tree;
    private DefaultTreeModel _model;
    
    private static ImageIcon _rootIcon;
    public static ImageIcon _entryIcon;
    private static ImageIcon _fieldIcon;
    
    static{
        try{
            loadImages();
        }catch(Exception ex){
            DashBoardFrame.theLogger.log(Level.SEVERE,
                    "Error loading images");
        }
    }   
                
    TypeTreeView(){
        setLayout(new BorderLayout());
        _tree=new JTree(_root);
        _tree.setCellRenderer(new TreeRenderer());
        _model=(DefaultTreeModel)_tree.getModel();
        add( new JScrollPane(_tree),BorderLayout.CENTER);
        
    }
    void update(String type,String [] fields){
        boolean typeExists=false;
        int nTypes=_root.getChildCount();
        for(int i=0;!typeExists && i<nTypes;i++){
            DefaultMutableTreeNode typeNode=
                (DefaultMutableTreeNode)_root.getChildAt(i);
            Object typeName=typeNode.getUserObject();
            
            if(typeName.equals(type)){
                typeExists=true;
                updateFields(typeNode,fields);
            }
        }
        if(!typeExists){
            DefaultMutableTreeNode typeNode=new DefaultMutableTreeNode(type);
            _model.insertNodeInto(typeNode,_root,nTypes);
            
            updateFields(typeNode,fields);
            
        }
        _tree.expandRow(0);
    }
    private void updateFields(DefaultMutableTreeNode typeNode,String [] fields){
    
        for(int i=0;i<fields.length;i++){
            boolean fieldExists=false;
            int nFields=typeNode.getChildCount();
            for(int j=0;!fieldExists && j<nFields;j++){
                DefaultMutableTreeNode fieldNode=(DefaultMutableTreeNode)typeNode.getChildAt(j);
                Object fieldName=fieldNode.getUserObject();
                fieldExists=fieldName.equals(fields[i]);
            }
            
            if(!fieldExists){
                DefaultMutableTreeNode fieldNode=new DefaultMutableTreeNode(fields[i]);
                _model.insertNodeInto(fieldNode,typeNode,
                                      typeNode.getChildCount());
            }
        }
        //TO DO:
        //if the blitz schema can change then we need to check that old fields are still in the tree
    }
    private class TreeRenderer  extends DefaultTreeCellRenderer{
        public Component getTreeCellRendererComponent(JTree tree,Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            
            JLabel lab =
                (JLabel)super.getTreeCellRendererComponent(tree, value, sel,
                                                           expanded, leaf, row,
                                                           hasFocus);
            if(row==0){
                lab.setIcon(_rootIcon);
            }else if(leaf){
                lab.setIcon(_fieldIcon);
            }else{
                lab.setIcon(_entryIcon);
            }
            return lab;
        }
    }
    private static void loadImages()
        throws Exception{
    
        ClassLoader cl=TypeTreeView.class.getClassLoader();
        _rootIcon=new ImageIcon(cl.getResource("org/dancres/blitz/tools/dash/images/instances.gif"));
        _entryIcon=new ImageIcon(cl.getResource("org/dancres/blitz/tools/dash/images/type.gif"));
        _fieldIcon=new ImageIcon(cl.getResource("org/dancres/blitz/tools/dash/images/field.gif"));
    }
}
