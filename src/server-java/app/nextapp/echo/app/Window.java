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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import nextapp.echo.app.command.OpenEcho3WindowCommand;
import nextapp.echo.app.update.ServerComponentUpdate;
import nextapp.echo.app.update.ServerUpdateManager;
import nextapp.echo.app.update.UpdateManager;

/**
 * A top-level window.
 */
public class Window extends Component {
    
    /** Serial Version UID. */
    private static final long serialVersionUID = 20070101L;

    public static final String PROPERTY_TITLE = "title";
    public static final String FOCUSED_COMPONENT_CHANGED_PROPERTY = "focusedComponent";
    public static final String MODAL_COMPONENTS_CHANGED_PROPERTY = "modalComponents";
    public static final String LAST_ENQUEUE_TASK_PROPERTY = "lastEnqueueTask";
    
    /** 
     * A <code>ThreadLocal</code> reference to the 
     * <code>Window</code> relevant to the current thread.
     */ 
    private static final ThreadLocal activeWindow = new ThreadLocal();
    
    /**
     * A <code>ThreadLocal</code> reference to a stack of the windows that
     * have been switched out of context for the current thread.
     */
    private static final ThreadLocal contextStackLocal = new ThreadLocal();
    
    /**
     * Determines the current modal component by searching the entire hierarchy for modal components.
     * This operation is only performed when multiple visibly rendered components are registered as modal.
     * 
     * @param searchComponent the root <code>Component</code> at which to start the search.
     * @param visibleModalComponents the set of visible modal components
     * @return the current modal component
     */
    private static Component findCurrentModalComponent(Component searchComponent, Set visibleModalComponents) {
        int count = searchComponent.getComponentCount();
        for (int i = count - 1; i >= 0; --i) {
            Component foundComponent = findCurrentModalComponent(searchComponent.getComponent(i), visibleModalComponents);
            if (foundComponent != null) {
                return foundComponent;
            }
        }

        if (searchComponent instanceof ModalSupport && ((ModalSupport) searchComponent).isModal()
                 && visibleModalComponents.contains(searchComponent)) {
            return searchComponent;
        }
        
        return null;
    }

    /**
     * Collection of modal components, the last index representing the current
     * modal context.
     */
    private List modalComponents;

    /**
     * The presently focused component.
     */
    transient WeakReference focusedComponent;

    /**
     * Mapping between <code>TaskQueueHandle</code>s and <code>List</code>s
     * of <code>Runnable</code> tasks.  Values may be null if a particular 
     * <code>TaskQueue</code> does not contain any tasks. 
     */
    HashMap taskQueueMap;
    
    /**
     * Mapping from the render ids of all registered components to the 
     * <code>Component</code> instances themselves.
     */
    private Map renderIdToComponentMap;
    
    /**
     * A map of component ids to component instances removed in the current request
     * @return
     */
    private Map componentsToRemove;
    
    /**
     * The current transactionId.  Used to ensure incoming ClientMessages reflect
     * changes made by user against current server-side state of user interface.
     * This is used to eliminate issues that could be encountered with two
     * browser windows pointing at the same application instance.
     */
    private int transactionId = 0;
    
    /**
     * The last time the window was involved in a synchronisation with a client
     */
    private long lastUpdateTime = System.currentTimeMillis();
    
    /**
     * Mapping between component instances and <code>RenderState</code> objects.
     */
    private Map componentToRenderStateMap = new HashMap();

  /**
   * The <code>UpdateManager</code> handling updates to/from this window.
   */
  private UpdateManager updateManager;

private boolean location;

private boolean menubar;

private boolean toolbar;

private boolean directories;

private boolean status;
    
    /**
     * Creates a new window.
     */
    public Window(ApplicationInstance appInstance) {
        super();
        taskQueueMap = new HashMap();
        renderIdToComponentMap = new HashMap();
        // set ourselves as the current context window
        if (Window.getActive() == null)
            Window.setActive(this);
        // ensure we know that we're the containing window
        setContainingWindow(this);
        // ensure we have a reference to the application instance we're associated with
        this.register(appInstance, this);
        super.setId(appInstance.generateWindowId());
        updateManager = new UpdateManager(this);
        add(new ContentPane());
    }
    
    public final void setId(String id) {
        throw new UnsupportedOperationException("Window ids cannot be changed");
    }
    
    /**
     * Retrieves the <code>UpdateManager</code> being used to manage the
     * client/server synchronization of this <code>ApplicationInstance</code>
     * 
     * @return the <code>UpdateManager</code>
     */
    public UpdateManager getUpdateManager() {
        return updateManager;
    }
    
    /**
     * Generates an identifier which is unique within this 
     * <code>ApplicationInstance</code>.  This identifier should not be
     * used outside of the context of this  <code>ApplicationInstance</code>.
     * 
     * @return the unique identifier
     * @see #generateSystemId()
     */
    public String generateId() {
        return getApplicationInstance().generateId();
    }
    
    /**
     * Returns a reference to the <code>Window</code> that is 
     * relevant to the current thread, or null if no instance is relevant.
     * 
     * @return the relevant <code>Window</code>
     */
    public static final Window getActive() {
        return (Window) activeWindow.get();
    }

    /**
     * Sets the <code>Window</code> that is relevant to the 
     * current thread.  This method should be invoked with a null
     * argument when the previously set <code>Window</code> is 
     * no longer relevant.
     * <p>
     * <b>This method should not be invoked by application code.</b>  
     * 
     * @param w the relevant <code>Window</code>
     */
    public static final void setActive(Window w) {
        activeWindow.set(w);
    }

    /**
     * Returns the current transaction id.
     * 
     * @return the current transaction id
     */
    public int getCurrentTransactionId() {
        return transactionId;
    }
    
    /**
     * Increments the current transaction id and returns it.
     * 
     * @return the current transaction id, after an increment
     */
    public int getNextTransactionId() {
        ++transactionId;
        return transactionId;
    }
    
    /**
     * Returns the content of the window.
     * 
     * @return the content of the window
     */
    public ContentPane getContent() {
        if (getComponentCount() == 0) {
            return null;
        } else {
            return (ContentPane) getComponent(0);
        }
    }
    
    /**
     * Returns the window title.
     * 
     * @return the window title
     */
    public String getTitle() {
        return (String) get(PROPERTY_TITLE);
    }
    
    /**
     * @see nextapp.echo.app.Component#isValidChild(nextapp.echo.app.Component)
     */
    public boolean isValidChild(Component component) {
        return getComponentCount() == 0 && component instanceof ContentPane;
    }
    
    /**
     * Reject all parents (<code>Window</code> may only be used as a 
     * top-level component).
     * 
     * @see nextapp.echo.app.Component#isValidParent(nextapp.echo.app.Component)
     */
    public boolean isValidParent(Component parent) {
        return false;
    }
    
    /**
     * Sets the content of the window.
     * 
     * @param newValue the new window content
     */
    public void setContent(ContentPane newValue) {
        removeAll();
        add(newValue);
    }
    
    /**
     * Sets the window title.
     * 
     * @param newValue the new window title
     */
    public void setTitle(String newValue) {
        set(PROPERTY_TITLE, newValue);
    }

    /**
     * Returns the presently focused component, if known.
     * 
     * @param applicationInstance TODO
     * @return the focused component
     */
    public Component getFocusedComponent() {
        if (focusedComponent == null) {
            return null;
        } else {
            return (Component) focusedComponent.get();
        }
    }
    
    /**
     * Sets the presently focused component.
     * 
     * @param newValue the component to be focused
     */
    public void setFocusedComponent(Component newValue) {
        if (newValue instanceof DelegateFocusSupport) {
            newValue = ((DelegateFocusSupport) newValue).getFocusComponent(); 
        }
        
        Component oldValue = getFocusedComponent();
        if (newValue == null) {
            focusedComponent = null;
        } else {
            focusedComponent = new WeakReference(newValue);
        }
        firePropertyChange(FOCUSED_COMPONENT_CHANGED_PROPERTY, oldValue, newValue);
        getUpdateManager().getServerUpdateManager().processApplicationPropertyUpdate(FOCUSED_COMPONENT_CHANGED_PROPERTY, 
                oldValue, newValue);
    }
    
    /**
     * Processes client input specific to the <code>Window</code> 
     * received from the <code>UpdateManager</code>.
     * Derivative implementations should take care to invoke 
     * <code>super.processInput()</code>.
     */
    public void processInput(String propertyName, Object propertyValue) {
        if (FOCUSED_COMPONENT_CHANGED_PROPERTY.equals(propertyName)) {
            setFocusedComponent((Component) propertyValue);
        }
    }

    /**
     * Creates a new task queue.  A handle object representing the created task
     * queue is returned.  The created task queue will remain active until it is
     * provided to the <code>removeTaskQueue()</code> method.  Developers must
     * take care to invoke <code>removeTaskQueue()</code> on any created
     * task queues.
     * 
     * @return a <code>TaskQueueHandler</code> representing the created task 
     *         queue
     * @param applicationInstance TODO
     * @see #removeTaskQueue(TaskQueueHandle)
     */
    public TaskQueueHandle createTaskQueue() {
        TaskQueueHandle taskQueue = new TaskQueueHandle() { 
            /** Serial Version UID. */
            private static final long serialVersionUID = 20070101L;
        };
        synchronized (taskQueueMap) {
            taskQueueMap.put(taskQueue, null);
        }
        return taskQueue;
    }
    
    public void doDispose() {
        super.doDispose();
        synchronized (taskQueueMap) {
            taskQueueMap.clear();
        }
    }
    
    /**
     * Enqueues a task to be run during the next client/server 
     * synchronization.  The task will be run 
     * <b>synchronously</b> in the user interface update thread.
     * Enqueuing a task in response to an external event will result 
     * in changes being pushed to the client.
     * 
     * @param taskQueue the <code>TaskQueueHandle</code> representing the
     *        queue into which this task should be placed
     * @param task the task to run on client/server synchronization
     */
    public void enqueueTask(TaskQueueHandle taskQueue, Runnable task) {
        synchronized (taskQueueMap) {
            List taskList = (List) taskQueueMap.get(taskQueue);
            if (taskList == null) {
                taskList = new ArrayList();
                taskQueueMap.put(taskQueue, taskList);
            }
            taskList.add(task);
            firePropertyChange(LAST_ENQUEUE_TASK_PROPERTY, null, new Long(System.currentTimeMillis()));
        }
    }
    
    /**
     * Determines if this <code>ApplicationInstance</code> currently has any 
     * active tasks queues, which might be monitoring external events.
     * 
     * @return true if the instance has any task queues
     */
    public final boolean hasTaskQueues() {
        return taskQueueMap.size() > 0;
    }
    
    /**
     * Determines if there are any queued tasks in any of the task
     * queues associated with this <code>ApplicationInstance</code>.
     * <p>
     * This method may be overridden by an application in order to check
     * on the status of long-running operations and enqueue tasks 
     * just-in-time.  In such cases tasks should be <strong>enqueued</strong>
     * and the value of <code>super.hasQueuedTasks()</code> should be 
     * returned.  This method is not invoked by a user-interface thread and
     * thus the component hierarchy may not be modified in
     * overriding implementations.
     * 
     * @return true if any tasks are queued
     */
    public boolean hasQueuedTasks() {
        if (taskQueueMap.size() == 0) {
            return false;
        }
        Iterator it = taskQueueMap.values().iterator();
        while (it.hasNext()) {
            List taskList = (List) it.next();
            if (taskList != null && taskList.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes all queued tasks. This method may only be invoked from within a
     * UI thread by the <code>UpdateManager</code>. Tasks are removed from queues
     * once they have been processed.
     */
    public void processQueuedTasks() {
        if (taskQueueMap.size() == 0) {
            return;
        }
        
        List currentTasks = new ArrayList();
        synchronized (taskQueueMap) {
            Iterator taskListsIt = taskQueueMap.values().iterator();
            while (taskListsIt.hasNext()) {
                List tasks = (List) taskListsIt.next();
                if (tasks != null) {
                    currentTasks.addAll(tasks);
                    tasks.clear();
                }
            }
        }
        Iterator it = currentTasks.iterator();
        while (it.hasNext()) {
            ((Runnable) it.next()).run();
        }
    }
    
    /**
     * Removes the task queue described the specified 
     * <code>TaskQueueHandle</code>.
     * 
     * @param taskQueueHandle the <code>TaskQueueHandle</code> specifying the
     *        task queue to remove
     * @see #MISSING()
     */
    public void removeTaskQueue(TaskQueueHandle taskQueueHandle) {
        synchronized(taskQueueMap) {
            taskQueueMap.remove(taskQueueHandle);
        }
    }

    /**
     * Queues the given stateless <code>Command</code> for execution on the 
     * current client/server synchronization.
     * 
     * @param command the <code>Command</code> to execute
     */
    public void enqueueCommand(Command command) {
        getUpdateManager().getServerUpdateManager().enqueueCommand(command);
    }
    
    /**
     * Retrieves the root component of the current modal context, or null
     * if no modal context exists.  Components which are not within the 
     * descendant hierarchy of the modal context are barred from receiving
     * user input.
     * 
     * @return the root component of the modal context
     */
    public Component getModalContextRoot() {
        if (modalComponents == null || modalComponents.size() == 0) {
            // No components marked as modal.
            return null;
        } else if (modalComponents.size() == 1) {
            // One component marked as modal, return it if visible, null otherwise.
            Component component = (Component) modalComponents.get(0);
            return component.isRenderVisible() ? component : null;
        }

        // Multiple modal components.
        Set visibleModalComponents = new HashSet();
        for (int i = modalComponents.size() - 1; i >= 0; --i) {
            Component component = (Component) modalComponents.get(i);
            if (component.isRenderVisible()) {
                visibleModalComponents.add(component);
            }
        }
        
        return findCurrentModalComponent(Window.getActive(), visibleModalComponents);  
    }
    
    /**
     * Determines if the given component is modal (i.e., that only components
     * below it in the hierarchy should be enabled).
     * 
     * @param component the <code>Component</code>
     * @return true if the <code>Component</code> is modal 
     */
    private boolean isModal(Component component) {
        return modalComponents != null && modalComponents.contains(component);
    }
    
    /**
     * Sets the modal state of a component (i.e, whether only it and 
     * components below it in the hierarchy should be enabled).
     * 
     * @param component the <code>Component</code>
     * @param newValue the new modal state
     */
    private void setModal(Component component, boolean newValue) {
        boolean oldValue = isModal(component);
        if (newValue) {
            if (modalComponents == null) {
                modalComponents = new ArrayList();
            }
            if (!modalComponents.contains(component)) {
                modalComponents.add(component);
            }
        } else {
            if (modalComponents != null) {
                modalComponents.remove(component);
            }
        }
        firePropertyChange(MODAL_COMPONENTS_CHANGED_PROPERTY, new Boolean(oldValue), new Boolean(newValue));
    }
    
    /**
     * Verifies that a <code>Component</code> is within the modal context, 
     * i.e., that if a modal <code>Component</code> is present, that it either 
     * is or is a child of that <code>Component</code>.
     * 
     * @param component the <code>Component</code> to evaluate
     * @return true if the <code>Component</code> is within the current 
     *         modal context
     * @see Component#verifyInput(java.lang.String, java.lang.Object)
     */
    boolean verifyModalContext(Component component) {
        Component modalContextRoot = getModalContextRoot();
        return modalContextRoot == null || modalContextRoot.isAncestorOf(component);
    }
    
    /**
     * Notifies the <code>UpdateManager</code> in response to a component 
     * property change or child addition/removal.
     * <p>
     * This method is invoked directly from <code>Component</code>s
     * (rather than using a <code>PropertyChangeListener</code>) in the interest
     * of memory efficiency. 
     * 
     * @param parent the parent/updated component
     * @param propertyName the name of the property changed
     * @param oldValue the previous value of the property 
     *        (or the removed component in the case of a
     *        <code>CHILDREN_CHANGED_PROPERTY</code>)
     * @param newValue the new value of the property 
     *        (or the added component in the case of a
     *        <code>CHILDREN_CHANGED_PROPERTY</code>)
     * @throws IllegalStateException in the event that the current thread is not
     *         permitted to update the state of the user interface
     */
    void notifyComponentPropertyChange(Component parent, String propertyName, Object oldValue, Object newValue) {
        // Ensure current thread is a user interface thread.
        if (Window.getActive() == null) {
            throw new IllegalStateException(
                    "Attempt to update state of application user interface outside of user interface thread.");
        // if the application instance is null, we're in weird territory (or unit tests)
        } else if (getApplicationInstance() == null) {
            return;
        // If the window isn't active yet, don't worry about any updates as it will get a full render after being added
        } else if (!getApplicationInstance().isWindowActive(this)) {
            return;
        }

        ServerUpdateManager serverUpdateManager = getUpdateManager().getServerUpdateManager();
        if (Component.CHILDREN_CHANGED_PROPERTY.equals(propertyName)) {
            if (newValue == null) {
                serverUpdateManager.processComponentRemove(parent, (Component) oldValue);
            } else {
                serverUpdateManager.processComponentAdd(parent, (Component) newValue);
            }
        } else if (Component.PROPERTY_LAYOUT_DATA.equals(propertyName)) {
            serverUpdateManager.processComponentLayoutDataUpdate(parent);
        } else if (Component.VISIBLE_CHANGED_PROPERTY.equals(propertyName)) {
            if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
                return;
            }
            serverUpdateManager.processComponentVisibilityUpdate(parent);
        } else {
            if ((oldValue == null && newValue == null) 
                || (oldValue != null && newValue != null && oldValue.equals(newValue))) {
                return;
            }
            if (parent instanceof ModalSupport && ModalSupport.MODAL_CHANGED_PROPERTY.equals(propertyName)) {
                setModal(parent, ((Boolean) newValue).booleanValue());
            }
            serverUpdateManager.processComponentPropertyUpdate(parent, propertyName, oldValue, newValue);
        }
    }
    
    /**
     * Registers a component with the <code>Window</code>.
     * The component will be assigned a unique render id in the event that
     * it does not currently have one.
     * <p>
     * This method is invoked by <code>Component.setApplicationInstance()</code>
     * 
     * @param component the component to register
     * @see Component#register(ApplicationInstance)
     */
    void registerComponent(Component component) {
        if (component == this)
            return;
        String renderId = component.getRenderId();
        if (renderId == null || renderIdToComponentMap.containsKey(renderId)) {
            // Note that the render id is reassigned if it currently exists renderIdToComponentMap.  This could be the case
            // in the event a Component was being used in a pool.
        	renderId = generateId();
            component.assignRenderId(renderId);
        }
        renderIdToComponentMap.put(renderId, component);
        if (component instanceof ModalSupport && ((ModalSupport) component).isModal()) {
            setModal(component, true);
        }
    }

    /**
     * Enqueues a component to be unregistered from the <code>Window</code>.
     * <p>
     * Component removals are enqueued in case the component is removed by one
     * update and a later update references the component.
     * </p>
     * <p>
     * This method is invoked by <code>Component.setContainingWindow()</code>.
     * </p>
     * 
     * @param component the component to unregister
     * @see Component#register(ApplicationInstance)
     */
    void unregisterComponent(Component component) {
    	if (componentsToRemove == null) {
    		componentsToRemove = new HashMap();
    	}
    	componentsToRemove.put(component.getRenderId(), component);
    }
        
    /**
     * Actually performs the unregister of each component.
     */
    public void processComponentRemovals() {
    	if (componentsToRemove != null) {
    		Iterator i = componentsToRemove.entrySet().iterator();
    		while (i.hasNext()) {
    			Map.Entry entry = (Map.Entry)i.next();
    			String renderId = (String)entry.getKey();
    			Component component = (Component)entry.getValue();
    	        component.assignLastRenderId(renderId);
    			renderIdToComponentMap.remove(renderId);
    	        if (component instanceof ModalSupport && ((ModalSupport) component).isModal()) {
    	            setModal(component, false);
    	        }
    		}
    		componentsToRemove = null;
    	}
     }

    /**
     * Retrieves the component currently registered with the application 
     * with the specified render id.
     * 
     * @param renderId the render id of the component
     * @return the component (or null if no component with the specified
     *         render id is registered)
     */
    public Component getComponentByRenderId(String renderId) {
        return (Component) renderIdToComponentMap.get(renderId);
    }
    
    /**
     * Removes all <code>RenderState</code>s whose components are not
     * registered.
     */
    public void purgeRenderStates() {
        ServerComponentUpdate[] updates = getUpdateManager().getServerUpdateManager().getComponentUpdates();

        Iterator it = componentToRenderStateMap.keySet().iterator();
        while (it.hasNext()) {
            Component component = (Component) it.next();
            if (!component.isRegistered() || !component.isRenderVisible()) {
                it.remove();
                continue;
            }

            for (int i = 0; i < updates.length; ++i) {
                if (updates[i].hasRemovedDescendant(component.getRenderId())) {
                    it.remove();
                    continue;
                }
            }
        }
    }

    /**
     * Clears all <code>RenderState</code> information.
     */
    public void clearRenderStates() {
        componentToRenderStateMap.clear();
    }

    /**
     * Retrieves the <code>RenderState</code> of the specified
     * <code>Component</code>.
     * 
     * @param component the component
     * @return the rendering state
     */
    public RenderState getRenderState(Component component) {
        return (RenderState) componentToRenderStateMap.get(component);
    }

    /**
     * Removes the <code>RenderState</code> of the specified
     * <code>Component</code>.
     * 
     * @param component the component
     */
    public void removeRenderState(Component component) {
        componentToRenderStateMap.remove(component);
    }

    /**
     * Sets the <code>RenderState</code> of the specified 
     * <code>Component</code>.
     * 
     * @param component the component
     * @param renderState the render state
     */
    public void setRenderState(Component component, RenderState renderState) {
        componentToRenderStateMap.put(component, renderState);
    }
    
    /**
     * This method activates this window instance as the active window.
     * Any calls to this method <b>must</b> be accompanied by a call to
     * switchOutOfContext.
     */
    public void switchIntoContext() {
        Stack contextStack = (Stack)contextStackLocal.get();
        if (contextStack == null) {
            contextStack = new Stack();
            contextStackLocal.set(contextStack);
        }
        contextStack.push(Window.getActive());
        Window.setActive(this);
    }
    
    /**
     * This method re-activates the previously active window instance.
     * If there was no previous call to switchIntoContext, then a call
     * to this will result in Window.getActive() returning null.
     */
    public void switchOutOfContext() {
        Window.setActive(null);
        Stack contextStack = (Stack)contextStackLocal.get();
        if (contextStack == null) {
            return;
        }
        Window w = (Window)contextStack.pop();
        Window.setActive(w);
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void updateLastUpdateTime() {
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * If this feature is set to true, then the new secondary window renders 
     * the menubar.<br/>
     * Mozilla and Firefox users can force new windows to always render the 
     * menubar by setting dom.disable_window_open_feature.menubar to true in 
     * about:config or in their user.js file. <br/>
     * Supported in: Internet Explorer 5+, Netscape 6.x, Netscape 7.x, 
     * Mozilla 1.x, Firefox 1.x 
     * 
     * @return true if the feature is set, otherwise false
     */
    public boolean isMenubar() {
        return menubar;
    }

    /**
     * If this feature is set to true, then the new secondary window renders 
     * the Navigation Toolbar (Back, Forward, Reload, Stop buttons). 
     * In addition to the Navigation Toolbar, Mozilla-based browsers will 
     * render the Tab Bar if it is visible, present in the parent window. 
     * (If this feature is set to no all toolbars in the window will be 
     * invisible, for example extension toolbars).<br/>
     * Mozilla and Firefox users can force new windows to always render the 
     * Navigation Toolbar by setting dom.disable_window_open_feature.toolbar 
     * to true in about:config or in their user.js file.<br/>
     * Supported in: Internet Explorer 5+, Netscape 6.x, Netscape 7.x, 
     * Mozilla 1.x, Firefox 1.x 
     * 
     * @return true if the feature is set, otherwise false
     */
    public boolean isToolbar() {
        return toolbar;
    }

    /**
     * If this feature is set to true, then the new secondary window renders the 
     * Personal Toolbar in Netscape 6.x, Netscape 7.x and Mozilla browser. 
     * It renders the Bookmarks Toolbar in Firefox 1.x and, in MSIE 5+, it 
     * renders the Links bar. In addition to the Personal Toolbar, Mozilla 
     * browser will render the Site Navigation Bar if such toolbar is visible, 
     * present in the parent window.<br/>
     * Mozilla and Firefox users can force new windows to always render the 
     * Personal Toolbar/Bookmarks toolbar by setting 
     * dom.disable_window_open_feature.directories to true in about:config or 
     * in their user.js file.<br/>
     * Supported in: Internet Explorer 5+, Netscape 6.x, Netscape 7.x, 
     * Mozilla 1.x, Firefox 1.x 
     * 
     * @return true if the feature is set, otherwise false
     */
    public boolean isDirectories() {
        return directories;
    }

    /**
     * If this feature is set to true, then the new secondary window has a 
     * status bar. Users can force the rendering of status bar in all 
     * Mozilla-based browsers, in MSIE 6 SP2 (Note on status bar in XP SP2) 
     * and in Opera 6+. The default preference setting in recent Mozilla-based 
     * browser releases and in Firefox 1.0 is to force the presence of the 
     * status bar.<br/>
     * Supported in: Internet Explorer 5+, Netscape 6.x, Netscape 7.x, 
     * Mozilla 1.x, Firefox 1.x
     * 
     * @return true if the feature is set, otherwise false
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * If this feature is set to yes, then the new secondary window renders 
     * the Location bar in Mozilla-based browsers. MSIE 5+ and Opera 7.x 
     * renders the Address Bar. <br/>
     * Mozilla and Firefox users can force new windows to always render the 
     * location bar by setting dom.disable_window_open_feature.location to 
     * true in about:config or in their user.js file. <br/>
     * 
     * Firefox 3 note: <br/>
     * In Firefox 3, dom.disable_window_open_feature.location now defaults to 
     * true, forcing the presence of the Location Bar much like in IE7. 
     * See bug mozilla 337344 for more information.
     * 
     * @return true if the feature is set, otherwise false
     */
    public boolean isLocation() {
        return location;
    }
    
    /**
     * @see #isMenubar()
     * @param menubar
     */
    public void setMenubar(boolean menubar) {
        this.menubar = menubar;
    }

    /**
     * @see #isToolbar()
     * @param toolbar
     */
    public void setToolbar(boolean toolbar) {
        this.toolbar = toolbar;
    }

    /**
     * @see #isDirectories()
     * @param directories
     */
    public void setDirectories(boolean directories) {
        this.directories = directories;
    }

    /**
     * @see #isStatus()
     * @param status
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * @see #isLocation()
     * @param location
     */
    public void setLocation(boolean location) {
        this.location = location;
    }
    
    public Command getOpenWindowCommand() {
        return new OpenEcho3WindowCommand(this);
    }
}
