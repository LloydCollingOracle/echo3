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

package nextapp.echo.app;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nextapp.echo.app.command.BrowserCloseWindowCommand;
import nextapp.echo.app.command.OpenEcho3WindowCommand;
import nextapp.echo.app.util.Uid;

/**
 * A single user-instance of an Echo application.
 */
public abstract class ApplicationInstance implements Serializable {
    
    /** Serial Version UID. */
    private static final long serialVersionUID = 20070101L;

    /** The name and version of the Echo API in use. */
    public static final String ID_STRING = "Echo v3.1";

    public static final String LOCALE_CHANGED_PROPERTY = "locale";
    public static final String STYLE_SHEET_CHANGED_PROPERTY = "styleSheet";
    public static final String WINDOWS_CHANGED_PROPERTY = "windows";
    
    /** 
     * A <code>ThreadLocal</code> reference to the 
     * <code>ApplicationInstance</code> relevant to the current thread.
     */ 
    private static final ThreadLocal activeInstance = new ThreadLocal();
    
    /**
     * Generates a system-level identifier (an identifier which is unique to all
     * <code>ApplicationInstance</code>s).
     * 
     * @return the generated identifier
     * @see #generateId()
     */
    public static final String generateSystemId() {
        return Uid.generateUidString();
    }
    
    /**
     * Returns a reference to the <code>ApplicationInstance</code> that is 
     * relevant to the current thread, or null if no instance is relevant.
     * 
     * @return the relevant <code>ApplicationInstance</code>
     */
    public static final ApplicationInstance getActive() {
        return (ApplicationInstance) activeInstance.get();
    }

    /**
     * Sets the <code>ApplicationInstance</code> that is relevant to the 
     * current thread.  This method should be invoked with a null
     * argument when the previously set <code>ApplicationInstance</code> is 
     * no longer relevant.
     * <p>
     * <b>This method should only be invoked by the application container.</b>  
     * 
     * @param applicationInstance the relevant <code>ApplicationInstance</code>
     */
    public static final void setActive(ApplicationInstance applicationInstance) {
        activeInstance.set(applicationInstance);
    }

    /**
     * The default <code>Locale</code> of the application.
     * This <code>Locale</code> will be inherited by <code>Component</code>s. 
     */
    private Locale locale;
    
    /** 
     * The default <code>LayoutDirection</code> of the application, derived 
     * from the application's <code>Locale</code>.
     * This <code>LayoutDirection</code> will be inherited by 
     * <code>Component</code>s. 
     */
    private LayoutDirection layoutDirection;

    /** 
     * Contextual data.
     * @see #getContextProperty(java.lang.String)
     */
    private Map context;
    
    /**
     * Fires property change events for the instance object.
     */
    private PropertyChangeSupport propertyChangeSupport;
    
    /**
     * The active top-level <code>Window</code> instances.
     */
    private Window[] activeWindows;
        
    /**
     * The top-level windows that will close on the next interaction
     */
    private Window[] closingWindows;
    
    /**
     * The <code>StyleSheet</code> used by the application.
     */
    private StyleSheet styleSheet;
        
    /**
     * Whether the application automatically copes with asynchronous window updates
     * as default in a multi-window application
     */
    private boolean allowAsyncWindowUpdates = true;
    
    /**
     * The system-generated id of the default window
     */
    private String defaultWindowId = null;
            
    /**
     * Flag indicating whether the application has been disposed, i.e., whether <code>ApplicationInstance.dispose()</code>
     * has been invoked.
     */
    private volatile boolean disposed = false;
    
    /** 
     * Creates an <code>ApplicationInstance</code>. 
     */
    public ApplicationInstance() {
        super();
        
        locale = Locale.getDefault();
        layoutDirection = LayoutDirection.forLocale(locale);
        
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    /**
     * Invoked after the application has been passivated (such that its state may
     * be persisted or moved amongst VMs) and is about to be reactivated.
     * Implementations must invoke <code>super.activate()</code>.
     */
    public void activate() {
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to receive notification of
     * application-level property changes.
     * 
     * @param l the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Invoked when the application is disposed and will not be used again.
     * Implementations must invoke <code>super.dispose()</code>.
     */
    public void dispose() {
        if (disposed) {
            throw new IllegalStateException("Attempt to invoke ApplicationInstance.dispose() on disposed instance.");
        }        
        try {
        	if (activeWindows == null)
	            return;
	        for (int i = 0; i < activeWindows.length; i++) {
	            activeWindows[i].doDispose();
	            activeWindows[i].register(null, null);
	        }
        } finally {
            disposed = true;
        }
    }

    /**
     * Initializes the <code>ApplicationInstance</code>. This method is
     * invoked by the application container.
     * 
     * @param parameters The parameters initially passed to the application
     * @param allowAsyncWindowUpdates Whether asynchronous updates will be 
     *                                automatically handled in a multiple window 
     *                                situation
     * @return the default <code>Window</code> of the application
     * @throws IllegalStateException in the event that the current thread is not
     *         permitted to update the state of the user interface
     */
    public final Window doInit(boolean allowAsyncWindowUpdates, String defaultWindowId) {
        if (this != activeInstance.get()) {
            throw new IllegalStateException(
                    "Attempt to update state of application user interface outside of user interface thread.");
        }
        this.allowAsyncWindowUpdates = allowAsyncWindowUpdates;
        this.defaultWindowId = defaultWindowId;
        Window window = init();
        addWindow(window);
        doValidation();
        return window;
    }

    /**
     * Validates all components registered with the application.
     */
    public final void doValidation() {
        for (int i = 0; i < activeWindows.length; i++) {
            doValidation(activeWindows[i]);
        }
    }
    
    /**
     * Validates a single component and then recursively validates its 
     * children.  This is the recursive support method for
     * the parameterless <code>doValidation()</code> method.
     *
     * @param c The component to be validated.
     * @see #doValidation()
     */
    private void doValidation(Component c) {
        c.validate();
        int size = c.getComponentCount();
        for (int index = 0; index < size; ++index) {
            doValidation(c.getComponent(index));
        }
    }
    
    /**
     * Reports a bound property change.
     *
     * @param propertyName the name of the changed property
     * @param oldValue the previous value of the property
     * @param newValue the present value of the property
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    /**
     * Returns the value of a contextual property.
     * Contextual properties are typically set by an application
     * container, e.g., the Web Container, in order to provide
     * container-specific information.  The property names of contextual
     * properties are provided within the application container
     * documentation when their use is required.
     * 
     * @param propertyName the name of the object
     * @return the object
     */
    public Object getContextProperty(String propertyName) {
        return context == null ? null : context.get(propertyName);
    }

    /**
     * Returns a window of the application.
     * 
     * @return the <code>Window</code>
     */
    public Window getWindow(int index) {
    	if (activeWindows == null)
    		return null;
        return activeWindows[index];
    }
    
    /**
     * Returns the application instance's default 
     * <code>LayoutDirection</code>.
     *
     * @return the <code>Locale</code>
     */
    public LayoutDirection getLayoutDirection() {
        return layoutDirection;
    }
    
    /**
     * Returns the application instance's default <code>Locale</code>.
     *
     * @return the <code>Locale</code>
     */
    public Locale getLocale() {
        return locale;
    }
    
    /**
     * Retrieves the style for the specified specified class of 
     * component / style name.
     * 
     * @param componentClass the component <code>Class</code>
     * @param styleName the component's specified style name
     * @return the appropriate application-wide style, or null
     *         if none exists
     */
    public Style getStyle(Class componentClass, String styleName) {
        if (styleSheet == null) {
            return null;
        } else {
            return styleSheet.getStyle(styleName, componentClass, true);
        }
    }
    
    /**
     * Returns the application-wide <code>StyleSheet</code>, if present.
     * 
     * @return the <code>StyleSheet</code>
     */
    public StyleSheet getStyleSheet() {
        return styleSheet;
    }
    
    /**
     * Invoked to initialize the default top-level window of the application.
     * The returned window must be visible.
     *
     * @return the default top-level window of the application
     */
    public abstract Window init();
    
    /**
     * Invoked before the application is passivated (such that its state may
     * be persisted or moved amongst VMs).
     * Implementations must invoke <code>super.passivate()</code>.
     */
    public void passivate() {
    }
    
    /**
     * Removes a <code>PropertyChangeListener</code> from receiving 
     * notification of application-level property changes.
     * 
     * @param l the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * Sets a contextual property.
     * 
     * @param propertyName the property name
     * @param propertyValue the property value
     * 
     * @see #getContextProperty(java.lang.String)
     */
    public void setContextProperty(String propertyName, Object propertyValue) {
        if (context == null) {
            context = new HashMap();
        }
        if (propertyValue == null) {
            context.remove(propertyName);
        } else {
            context.put(propertyName, propertyValue);
        }
    }
    
    protected void removeWindow(Window window) {
        // de-register the window and it's hierarchy
        window.register(null, null);
        
        // remove the window from the list of windows
        Window[] old = activeWindows;
        activeWindows = new Window[old.length - 1];
        for (int i  = 0, oldIndex = 0; i < activeWindows.length; i++) {
            if (old[oldIndex] == window)
                oldIndex++;
            activeWindows[i] = old[oldIndex++];
        }
        
        if (closingWindows == null) {
            closingWindows = new Window[] {window};
        } else {
            old = closingWindows;
            closingWindows = new Window[old.length + 1];
            for (int i  = 0; i <old.length; i++) {
                closingWindows[i] = old[i];
            }
            closingWindows[old.length] = window;
        }
        
        if (activeWindows.length == 1) {
            activeWindows[0].getUpdateManager().destroyAsyncUpdateQueue();
        }
    }
    
    /**
     * Adds a top-level window.
     * 
     * @param window the top-level window
     */
    protected void addWindow(Window window) {
        if (activeWindows == null) {
            activeWindows = new Window[] {window};
            window.register(this, window);
        } else {
            // add the window to the list of windows
            Window[] old = activeWindows;
            activeWindows = new Window[old.length + 1];
            for (int i  = 0; i <old.length; i++) {
                activeWindows[i] = old[i];
            }
            activeWindows[old.length] = window;
            // enqueue a command in the main window to open the new window
            activeWindows[0].enqueueCommand(
                    window.getOpenWindowCommand()
            );
            
            // if we're doing async updates, create update task queues if needed
            if (allowAsyncWindowUpdates) {
                if (old.length == 1) {
                    activeWindows[0].getUpdateManager().createAsyncUpdateQueue();
                }
                window.getUpdateManager().createAsyncUpdateQueue();
            }
            window.register(this, window);
        }
        firePropertyChange(WINDOWS_CHANGED_PROPERTY, null, window);
        window.doInit();
    }
    
    /**
     * Whether the specified window has been added to this ApplicationInstance
     * @param w The window to determine if it's been added
     * @return True if the given window has been added to the application, false otherwise
     */
    public boolean isWindowActive(Window w) {
        if (activeWindows == null)
            return false;
        for (int i = 0; i < activeWindows.length; i++) {
            if (activeWindows[i].getId().equals(w.getId()))
                return true;
        }
        return false;
    }
    
    /**
     * Sets the default locale of the application.
     * 
     * @param newValue the new locale
     */
    public void setLocale(Locale newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("ApplicationInstance Locale may not be null.");
        }
        Locale oldValue = locale;
        locale = newValue;
        layoutDirection = LayoutDirection.forLocale(locale);
        propertyChangeSupport.firePropertyChange(LOCALE_CHANGED_PROPERTY, oldValue, newValue);
        
        if (activeWindows == null)
            return;
        // Perform full refresh: container's synchronization peers may need to provide new localization resources to client.
        for (int i = 0; i < activeWindows.length; i++) {
            activeWindows[i].getUpdateManager().getServerUpdateManager().processFullRefresh();
        }
    }
    
    /**
     * Sets the <code>StyleSheet</code> of this 
     * <code>ApplicationInstance</code>.  <code>Component</code>s 
     * registered with this instance will retrieve
     * properties from the <code>StyleSheet</code>
     * when property values are not specified directly
     * in a <code>Component</code> or in its specified <code>Style</code>.
     * <p>
     * Note that setting the style sheet should be
     * done sparingly, given that doing so forces the entire
     * client state to be updated.  Generally style sheets should
     * only be reconfigured at application initialization and/or when
     * the user changes the visual theme of a theme-capable application.
     * 
     * @param newValue the new style sheet
     */
    public void setStyleSheet(StyleSheet newValue) {
        StyleSheet oldValue = styleSheet;
        this.styleSheet = newValue;
        firePropertyChange(STYLE_SHEET_CHANGED_PROPERTY, oldValue, newValue);
    }
    
    /**
     * Returns the top-level window whose id property matches the given value
     * @param windowId The id of the top-level window to find
     * @return The top-level window with the given id, or null if no matching window is found
     */
    public Window getWindow(String windowId) {
        if (activeWindows != null) {
            for (int i = 0; i < activeWindows.length; i++) {
                if (activeWindows[i].getId().equals(windowId))
                    return activeWindows[i];
            }
        }
        if (closingWindows != null) {
            for (int i = 0; i < closingWindows.length; i++) {
                if (closingWindows[i].getId().equals(windowId))
                    return closingWindows[i];
            }
        }
        return null;
     }
    
    /**
     * Used to open a new top-level window instance.
     * @param w The window to open
     */
    public void openWindow(Window w) {
        addWindow(w);
        doValidation();
    }
    
    /**
     * Returns the currently active windows of an application
     * @return
     */
    public Window[] getActiveWindows() {
        Window[] ret = new Window[activeWindows.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = activeWindows[i];
        }
        return ret;
    }

    /**
     * Returns the both active and inactive windows of an application
     * @return
     */
    public Window[] getWindows() {
        Window[] ret = null;
        if (closingWindows != null)
            ret = new Window[activeWindows.length + closingWindows.length];
        else
            ret = new Window[activeWindows.length];
        for (int i = 0; i < ret.length; i++) {
            if (i < activeWindows.length)
                ret[i] = activeWindows[i];
            else
                ret[i] = closingWindows[i - activeWindows.length];
        }
        return ret;
    }
    
    /**
     * Closes the given top-level window instance.
     * @param w The window to close.
     */
    public void closeWindow(Window w) {
        removeWindow(w);
        w.enqueueCommand(new BrowserCloseWindowCommand());
        doValidation();
    }

    /**
     * De-references the given window if it is in the list of closing windows
     * @param active The window to dereference
     */
    public void removeIfClosing(Window active) {
        if (closingWindows == null)
            return;
        if (closingWindows.length == 1) {
            if (closingWindows[0] == active) {
                closingWindows = new Window[0];
            }
        } else if (closingWindows.length > 1) {
            Window[] old = closingWindows;
            closingWindows = new Window[old.length - 1];
            int oldIndex = 0;
            for (int i = 0; i < closingWindows.length; i++) {
                if (old[oldIndex] == active) {
                    oldIndex++;
                }
                closingWindows[i] = old[oldIndex++];
            }
        }
    }

    public String getDefaultWindowId() {
        if (defaultWindowId == null) {
            if (activeWindows != null) {
                defaultWindowId = activeWindows[0].getId();
            } else {
                defaultWindowId = generateSystemId();
            }
        }
        return defaultWindowId;
    }

    public String generateWindowId() {
        if (activeWindows == null || activeWindows.length == 0) {
            return getDefaultWindowId();
        }
        return generateSystemId();
    }
}
