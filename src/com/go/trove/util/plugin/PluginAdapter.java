/**
 * PluginAdapter.java
 *
 * Copyright (c) 1999-2001 Disney Internet Group, Inc. All Rights Reserved.
 *
 * Original author: Scott Jappinen (scott.jappinen@dig.com)
 *
 *   $Author: dan $
 * $Revision: 1.1 $
 *     $Date: Mon, 13 Oct 2003 12:20:39 +0100 $
 */	
package com.go.trove.util.plugin;

/**
 *
 *
 * @author Scott Jappinen
 * @version <!--$$Revision: 1.1 $-->-<!--$$JustDate:--> 01/03/23 <!-- $-->
 */
public class PluginAdapter implements Plugin {

    /**
     * Initializes resources used by the Plugin.
     *
     * @param config the plugins's configuration object
     */	
    public void init(PluginConfig config) throws PluginException {}
    
    /**
     * Return the name of the Plugin.
     *
     * @return String the name of the plugin.
     */
    public String getName() {
        return null;
    }
    
    /**
     * Called by the host container when the plugin is no longer needed.
     */	
    public void destroy() {}

    /** 
     * This method is invoked whenever a Plugin has added itself to the PluginContext.
     *
     * @param event a PluginEvent event object.
     */
    public void pluginAdded(PluginEvent event) {}
    
}

