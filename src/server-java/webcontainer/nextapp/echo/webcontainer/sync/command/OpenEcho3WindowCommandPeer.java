/* 
 * This file is part of the Echo Web Application Framework (hereinafter "Echo").
 * Copyright (C) 2002-2009 NextApp, Inc.
 *
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */

package nextapp.echo.webcontainer.sync.command;

import nextapp.echo.app.Command;
import nextapp.echo.app.Window;
import nextapp.echo.app.command.OpenEcho3WindowCommand;
import nextapp.echo.app.util.Context;
import nextapp.echo.webcontainer.AbstractCommandSynchronizePeer;
import nextapp.echo.webcontainer.ServerMessage;
import nextapp.echo.webcontainer.Service;
import nextapp.echo.webcontainer.WebContainerServlet;
import nextapp.echo.webcontainer.service.JavaScriptService;

/**
 * Synchronization peer for <code>BrowserOpenWindowCommand</code>.
 */
public class OpenEcho3WindowCommandPeer 
extends AbstractCommandSynchronizePeer {
    
    /** The associated client-side JavaScript module <code>Service</code>. */
    private static final Service BROWSER_OPEN_WINDOW_SERVICE = JavaScriptService.forResource("Echo.OpenEcho3Window", 
            "nextapp/echo/webcontainer/resource/RemoteClient.OpenEcho3Window.js");
    
    static {
        WebContainerServlet.getServiceRegistry().add(BROWSER_OPEN_WINDOW_SERVICE);
    }

    /**
     * Default constructor.
     */
    public OpenEcho3WindowCommandPeer() {
        super();
        addProperty("uri", new AbstractCommandSynchronizePeer.PropertyPeer() {
            public Object getProperty(Context context, Command command) {
            	Window w = ((OpenEcho3WindowCommand) command).getWindow();
            	
                return WebContainerServlet.getActiveConnection().getRequest().getRequestURL() 
                    + "?sid=" 
                    + WebContainerServlet.SERVICE_ID_NEW_WINDOW 
                    + "&wid=" 
                    + w.getId()
                    + "&uiid=" + WebContainerServlet.getActiveConnection().getUserInstance().getClientWindowId();
            }
        });
        addProperty("name", new AbstractCommandSynchronizePeer.PropertyPeer() {
            public Object getProperty(Context context, Command command) {
                return ((OpenEcho3WindowCommand) command).getWindow().getId();
            }
        });
        addProperty("features", new AbstractCommandSynchronizePeer.PropertyPeer() {
            public Object getProperty(Context context, Command command) {
            
                Window w = ((OpenEcho3WindowCommand) command).getWindow();
                
                JsFeatureBuilder builder = new JsFeatureBuilder();
                builder
                    .createFeature("directories", w.isDirectories())
                    .createFeature("toolbar", w.isToolbar())
                    .createFeature("status", w.isStatus())
                    .createFeature("address", w.isLocation())
                    .createFeature("menubar", w.isMenubar());
                
                return builder.toString();
            }
        });
    }
    
    private static class JsFeatureBuilder {
        
        private StringBuilder builder = new StringBuilder();
        
        private JsFeatureBuilder createFeature(String feature, boolean isOn) {
            if (builder.length() > 0) builder.append(',');
            builder.append(feature);
            builder.append('=');
            builder.append(isOn ? "yes" : "no");
            return this;
        }
        public String toString() {
            return builder.toString();
        }
    }
    
    /**
     * @see nextapp.echo.webcontainer.CommandSynchronizePeer#getCommandClass()
     */
    public Class getCommandClass() {
        return OpenEcho3WindowCommand.class;
    }
    
    /**
     * @see nextapp.echo.webcontainer.AbstractCommandSynchronizePeer#init(nextapp.echo.app.util.Context)
     */
    public void init(Context context) {
        ServerMessage serverMessage = (ServerMessage) context.get(ServerMessage.class);
        serverMessage.addLibrary(BROWSER_OPEN_WINDOW_SERVICE.getId());
    }
}
