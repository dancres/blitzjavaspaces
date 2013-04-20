/* ====================================================================
 * Trove - Copyright (c) 1999-2001 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */	
package com.go.trove.util.plugin;

import com.go.trove.util.PropertyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin can reference other Plugins through the PluginContext. If a Plugin is
 * not immediately available the Plugin can register itself as a listener with 
 * the PluginContext so as to be notified when new Plugins are added to the 
 * PluginContext. When Plugins are fully initialized and want to make 
 * themselves available to other Plugins they should add themselves to the 
 * PluginContext.
 *
 * @author Scott Jappinen
 * @version <!--$$Revision: 1.1 $-->-<!--$$JustDate:--> 01/03/23 <!-- $-->
 */
public class PluginContext {
    
    private List mPluginListeners;
    private Map mPluginMap;
    
    public PluginContext() {
        mPluginListeners = new ArrayList();
        mPluginMap = new HashMap(7);
    }
   
    /**
     * Adds a PluginListener to the PluginContext. Plugins or anything
     * else that want to listen to PluginEvents should add themselves
     * to the PluginContext through this method.
     *
     * @param listener the PluginListener to be added.
     */
    public void addPluginListener(PluginListener listener) {
        if (!mPluginListeners.contains(listener)) {
            mPluginListeners.add(listener);
        }
    }
    
    /**
     * Adds a Plugin to the PluginContext. Plugins that want to make
     * themselves available to other Plugins should add themselves
     * to the PluginContext through this method. All PluginListeners
     * will be notified of the new addition.
     *
     * @param plugin the Plugin to be added.
     */
    public void addPlugin(Plugin plugin) {
        if (!mPluginMap.containsKey(plugin.getName())) {
            mPluginMap.put(plugin.getName(), plugin);
            PluginEvent event = new PluginEvent(this, plugin);
            firePluginAddedEvent(event);
        }
    }

    /**
     * Returns a Plugin by name.
     *
     * @param name the name of the Plugin.
     * @return Plugin the Plugin object.
     */
    public Plugin getPlugin(String name) {
        return (Plugin) mPluginMap.get(name);
    }
    
    /**
     * Returns a Map of all of the Plugins.
     *
     * @return Map the map of Plugins.
     */
    public Map getPlugins() {
        return new HashMap(mPluginMap);		
    }	
    
    /* Notifies all PluginListeners of a Plugin being added to this class.
     */
    protected void firePluginAddedEvent(PluginEvent event) {
        PluginListener[] listeners = new PluginListener[mPluginListeners.size()];
        listeners = (PluginListener[]) mPluginListeners.toArray(listeners);
        for (int i=0; i < listeners.length; i++) {			
            listeners[i].pluginAdded(event);
        }
    }
}
