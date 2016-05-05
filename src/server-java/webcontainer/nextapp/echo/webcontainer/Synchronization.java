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

package nextapp.echo.webcontainer;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import nextapp.echo.app.ApplicationInstance;
import nextapp.echo.app.Command;
import nextapp.echo.app.Window;
import nextapp.echo.app.update.ServerUpdateManager;
import nextapp.echo.app.util.Log;
import nextapp.echo.webcontainer.util.XmlRequestParser.InvalidXmlException;

/**
 * The high-level object which encapsulates the core of the client-server synchronization process for 
 * server-side applications.
 */
public class Synchronization 
implements SynchronizationState {

    /** The <code>Connection</code> being processed. */
    private Connection conn;
    
    /** The relevant <code>UserInstance</code>. */
    private UserInstance userInstance;
    
    /** Flag indicating whether synchronization is coming from an out-of-sync client. */
    private boolean outOfSync = false;
    
    private InputProcessor inputProcessor;

    /**
     * Creates a new <code>Synchronization</code>.
     * 
     * @param conn the synchronization <code>Connection</code> 
     */
    public Synchronization(Connection conn) 
    throws IOException {
        super();
        this.conn = conn;
    }
    
    /**
     * @see nextapp.echo.webcontainer.SynchronizationState#isOutOfSync()
     */
    public boolean isOutOfSync() {
        return outOfSync;
    }
    
    /**
     * @see nextapp.echo.webcontainer.SynchronizationState#setOutOfSync()
     */
    public void setOutOfSync() {
        outOfSync = true;
    }

    /**
     * Processes input from the connection and renders output.
     * 
     * Performs the following operations:
     * <ul>
     *  <li>Initializes the <code>UserInstance</code> if it is new.</li>
     *  <li>Activates the <code>ApplicationInstance</code>.</li>
     *  <li>Processes input to the connection using an <code>InputProcessor</code>.</li>
     *  <li>Generates output for the connection using an <code>OutputProcessor</code>.</li>
     *  <li>Purges updates from the <code>UpdateManager</code> (which were processed by the <code>OutputProcessor</code>.</li>
     *  <li>Deactivates the <code>ApplicationInstance</code>.</li>
     * </ul>
     * 
     * @throws IOException
     */
    public void process() 
    throws IOException {
        try {
            inputProcessor = createInputProcessor(conn);
        } catch (InvalidXmlException ex) {
            // Invalid request made.
            Log.log("Invalid XML Received, returning 400/Bad Request.", ex);
            conn.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid XML");
            return;
        }
        
        userInstance = conn.getUserInstance(inputProcessor.getWindowId(), inputProcessor.getInitId());

        synchronized(userInstance) {
            boolean initRequired = !userInstance.isInitialized();
            
            if (initRequired) {
                // Initialize user instance.
                userInstance.initHTTP(conn);
            }

            userInstance.setActive(true);
            userInstance.prepareApplicationInstance(inputProcessor.getApplicationWindowId());
            try {
                // Process client input.
                inputProcessor.process();
                Window.getActive().updateLastUpdateTime();
                
                // Manage render states.
                if (Window.getActive().getUpdateManager().getServerUpdateManager().isFullRefreshRequired()) {
                	Window.getActive().clearRenderStates();
                } else {
                	Window.getActive().purgeRenderStates();
                }
                
                // Render updates.
                OutputProcessor outputProcessor = createOutputProcessor();
                outputProcessor.process();
                
                Window [] ws = ApplicationInstance.getActive().getWindows();
                for (int i = 0; i < ws.length; i++) {
                    if (ws[i] != Window.getActive()) {
                        ServerUpdateManager sum = ws[i].getUpdateManager().getServerUpdateManager();
                        Command[] commands = sum.getCommands();
                        if (!sum.isEmpty() 
                              || (commands != null && commands.length > 0)) {
                            ws[i].getUpdateManager().applyAsyncUpdates();
                        }
                    }
                }

                // if this window is closing, de-reference it so we don't leak memory
                ApplicationInstance.getActive().removeIfClosing(Window.getActive());
                
                // Purge updates.
                Window.getActive().getUpdateManager().purge();
            } finally {
            	Window.setActive(null);
                userInstance.setActive(false);
            }
        }
    }
    
    protected InputProcessor createInputProcessor(Connection conn) throws IOException {
        return new InputProcessor(this, conn);
    }
    
    protected OutputProcessor createOutputProcessor() {
        return new OutputProcessor(this, conn);
    }
}
