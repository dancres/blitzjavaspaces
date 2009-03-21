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

import java.util.Iterator;
import java.util.Set;

/**
 * This class is responsible for creating plugins based from a 
 * configuration block.
 *
 * @author Scott Jappinen
 * @version <!--$$Revision: 1.1 $-->-<!--$$JustDate:--> 01/03/23 <!-- $-->
 */
public class PluginFactory {

    private static final String cClassKey = "class";
    private static final String cInitKey = "init";
    private static final String cPluginsKey = "plugins";	
    
    public static final Plugin createPlugin(String name, PluginFactoryConfig config) 
        throws PluginFactoryException 
    {
        Plugin result;
        String className = config.getProperties().getString(cClassKey);
        PropertyMap props = config.getProperties().subMap(cInitKey);
        try {
            Class clazz = Class.forName(className);
            result = (Plugin) clazz.newInstance();
            PluginConfig pluginConfig = new PluginConfigSupport
                (props, config.getLog(), config.getPluginContext(), name);
            result.init(pluginConfig);
        } catch (PluginException e) {
            throw new PluginFactoryException(e);
        } catch (ClassNotFoundException e) {
            throw new PluginFactoryException(e);
        } catch (InstantiationException e) {
            throw new PluginFactoryException(e);
        } catch (IllegalAccessException e) {
            throw new PluginFactoryException(e);
        }
        return result;
    }
    
    public static final Plugin[] createPlugins(PluginFactoryConfig config) 
        throws PluginFactoryException 
    {
        Plugin[] result;
        PropertyMap properties = config.getProperties().subMap(cPluginsKey);
        Set keySet = properties.subMapKeySet();		
        result = new Plugin[keySet.size()];			
        Iterator iterator = keySet.iterator();
        for (int i=0; iterator.hasNext(); i++) {
            String name = (String) iterator.next();
            PropertyMap initProps = properties.subMap(name);
            PluginFactoryConfig conf = new PluginFactoryConfigSupport
                (initProps, config.getLog(), config.getPluginContext());
            result[i] = createPlugin(name, conf);
        }
        return result;
    }	

}

