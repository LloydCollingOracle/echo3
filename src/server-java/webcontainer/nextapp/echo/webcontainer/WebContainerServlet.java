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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nextapp.echo.app.ApplicationInstance;
import nextapp.echo.app.util.Log;
import nextapp.echo.app.util.Uid;
import nextapp.echo.webcontainer.service.AsyncMonitorService;
import nextapp.echo.webcontainer.service.BootService;
import nextapp.echo.webcontainer.service.NewInstanceService;
import nextapp.echo.webcontainer.service.NewWindowService;
import nextapp.echo.webcontainer.service.ResourceService;
import nextapp.echo.webcontainer.service.SessionExpiredService;
import nextapp.echo.webcontainer.service.StaticTextService;
import nextapp.echo.webcontainer.service.SynchronizeService;
import nextapp.echo.webcontainer.service.WindowHtmlService;

import java.util.*;

/**
 * Web container <code>HttpServlet</code> implementation.
 * An Echo application should provide an derivative of this
 * class which is registered in the web application
 * deployment descriptor.
 */
public abstract class WebContainerServlet extends HttpServlet {


    
    /** A <code>ThreadLocal</code> reference to the <code>Connection</code> relevant to the current thread. */ 
    private static final ThreadLocal activeConnection = new ThreadLocal();
    
    /**
     * A flag indicating whether caching should be disabled for all services.
     * This flag is for testing purposes only, and should be disabled for
     * production use.
     */
    public static final boolean DISABLE_CACHING = false;
    
    /** Flag indicating whether client-side debug console should be enabled. */
    public static final boolean ENABLE_CLIENT_DEBUG_CONSOLE = true;
    
    /**
     * Constant for <code>getInstanceMode()</code> indicating that one instance (<code>UserInstance</code> of
     * the application should exist per session.
     * If the user visits the application in a different browser window, the state of the singleton instance will
     * be displayed.  The user will experience "window not synchronized" errors if the user attempts to operate
     * the same application from multiple windows.
     */
    public static final int INSTANCE_MODE_SINGLE = 0;
    
    /**
     * Constant for <code>getInstanceMode()</code> indicating that one instance (<code>UserInstance</code> of
     * the application should exist per session per client browser window.
     * If the user visits the application in a different browser window, a new instance will be created.
     */
    public static final int INSTANCE_MODE_WINDOW = 1;
    
    /** Request parameter identifying requested <code>Service</code>. */
    public static final String SERVICE_ID_PARAMETER = "sid";
    
    /** Request parameter identifying requested <code>UserInstance</code>. */
    public static final String USER_INSTANCE_ID_PARAMETER = "uiid";
    
    /** Request parameter identifying requested Application Window. */
    public static final String APPLICATION_WINDOW_ID_PARAMETER = "wid";
    
    /**
     * <code>Service</code> identifier of the 'default' service. 
     * The 'default' service is rendered when a client makes a request
     * without a service identifier and a session DOES exist.
     */
    public static final String SERVICE_ID_DEFAULT = "Echo.Default";
    
    /** <code>Service</code> identifier of the blank document service. */
    public static final String SERVICE_ID_BLANK_DOCUMENT = "Echo.BlankDocument";
    
    /**
     * <code>Service</code> identifier of the 'new instance' service. 
     * The 'new instance' service is rendered when a client makes a request
     * without a service identifier and a session DOES NOT exist.
     */
    public static final String SERVICE_ID_NEW_INSTANCE = "Echo.NewInstance";
    
    /**
     * <code>Service</code> identifier of the 'new window' service. 
     * The 'new window' service is rendered when a client makes a request
     * with the service identifier <code>Echo.NewWindow</code> and a session DOES exist..
     */
    public static final String SERVICE_ID_NEW_WINDOW = "Echo.NewWindow";
    
    /**
     * <code>Service</code> identifier of the 'session expired' service.
     * The 'session expired' service is rendered when a client makes a
     * request that has an identifier and is intended for an active session, 
     * but no session exists. 
     */
    public static final String SERVICE_ID_SESSION_EXPIRED = "Echo.Expired";
    
    /** Global handler for multipart/form-data encoded HTTP requests. */
    private static MultipartRequestWrapper multipartRequestWrapper;
    
    /** Time at which servlet was loaded by class loader. */
    private static final long startupTime = System.currentTimeMillis();
    
    /** Global <code>ResourceRegistry</code>. */
    private static final ResourceRegistry resources = new ResourceRegistry();
    
    /** Global <code>ServiceRegistry</code>. */
    private static final ServiceRegistry services = new ServiceRegistry();

    static {
        // Install bootstrap JavaScript service.
        BootService.install(services);
        
        // Register "Echo" package, add standard resources.
        resources.addPackage("Echo", "nextapp/echo/webcontainer/resource/");
        resources.add("Echo", "resource/Transparent.gif", ContentType.IMAGE_GIF);
        resources.add("Echo", "resource/Blank.html", ContentType.TEXT_HTML);
        
        // Add standard services.
        services.add(ResourceService.INSTANCE);
        services.add(new StaticTextService(SERVICE_ID_BLANK_DOCUMENT, "text/html", "<html></html>"));
    }
    
    /**
     * An interface implemented by a supporting object that will handle 
     * multipart/form-data encoded HTTP requests.  This type of request is
     * required for file uploads.  Echo does not provide internal support
     * for file uploads, but instead provides hooks for file-upload handling
     * components.  
     */
    public static interface MultipartRequestWrapper {
    
        /**
         * Returns a replacement <code>HttpServletRequest</code> object that
         * may be used to handle a multipart/form-data encoded HTTP request.
         *
         * @param request The HTTP request provided from the servlet container
         *        that has multipart/form-data encoding.
         * @return An HTTP request that is capable of handling 
         *         multipart/form-data encoding.
         */
        public HttpServletRequest getWrappedRequest(HttpServletRequest request)
        throws IOException, ServletException;
    }

    /**
     * Returns a reference to the <code>Connection</code> that is 
     * relevant to the current thread, or null if no connection is relevant.
     * 
     * @return the relevant <code>Connection</code>
     */
    public static final Connection getActiveConnection() {
        return (Connection) activeConnection.get();
    }
    
    /**
     * Returns the multipart/form-data encoded HTTP request handler.
     * 
     * @return The multipart/form-data encoded HTTP request handler.
     * @see #setMultipartRequestWrapper
     */
    public static MultipartRequestWrapper getMultipartRequestWrapper() {
        return multipartRequestWrapper;
    }
    
    /**
     * Retrieves the global <code>ResourceRegistry</code>.
     * 
     * @return the global <code>ResourceRegistry</code> 
     */
    public static ResourceRegistry getResourceRegistry() {
        return resources;
    }
    
    /**
     * Retrieves the global <code>ServiceRegistry</code>.
     * 
     * @return The global <code>ServiceRegistry</code>.
     */
    public static ServiceRegistry getServiceRegistry() {
        return services;
    }
    
    /**
     * Sets the multipart/form-data encoded HTTP request handler.
     * The multipart request wrapper can only be set one time.  It should be set
     * in a static block of your Echo application.  This method will disregard
     * additional attempts to set the wrapper if the provided wrapper's class
     * is identical to the existing one.  If the wrapper is already set and the
     * new wrapper object's class is different or the wrapper is null, an
     * exception is thrown.
     *
     * @param multipartRequestWrapper The handler for multipart/form-data 
     *        encoded HTTP requests.
     * @throws IllegalStateException if the application attempts to change
     *        a previously set multipart request handler.
     */
    public static final void setMultipartRequestWrapper(MultipartRequestWrapper multipartRequestWrapper) {
        if (WebContainerServlet.multipartRequestWrapper == null) {
            WebContainerServlet.multipartRequestWrapper = multipartRequestWrapper;
        } else {
            if (multipartRequestWrapper == null || 
                    !WebContainerServlet.multipartRequestWrapper.getClass().getName().equals(
                    multipartRequestWrapper.getClass().getName())) {
                throw new IllegalStateException("MultipartRequestWrapper already set.");
            }
        }
    }
    
    /** Collection of JavaScript <code>Service</code>s which should be initially loaded. */
    private List initScripts = null; 
    
    /** Collection of CSS style sheet <code>Service</code>s which should be initially loaded. */
    private List initStyleSheets = null;
    /** Whether to automatically handle asynchronous updates to windows in a multiple-window environment */
    private boolean allowAsyncWindowUpdates = true;
    
    private WebSocketConnectionHandler wsHandler = null;
    /**
     * Default constructor.
     */
    public WebContainerServlet() {
        super();
        services.add(NewInstanceService.INSTANCE);
        services.add(SessionExpiredService.INSTANCE);
        services.add(SynchronizeService.INSTANCE);
        services.add(WindowHtmlService.INSTANCE);
        services.add(AsyncMonitorService.INSTANCE);
    }

    public void init() throws ServletException {
        super.init();
        
        if (getServletConfig().getInitParameter("allowAsyncWindowUpdates") != null) {
            allowAsyncWindowUpdates = Boolean.parseBoolean(getServletConfig().getInitParameter("allowAsyncWindowUpdates"));
        }

        // Read servlet init parameters and update server configuration
        ServerConfiguration.adoptServletConfiguration(getInitParameterMap());
    }

    /**
     * Return the servlet's init parameters as a map.
     *
     * @return servlet init parameters as map
     */
    private Map getInitParameterMap() {
        Map initParameters = new HashMap();
        Enumeration parameterNames = getInitParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = (String) parameterNames.nextElement();
            initParameters.put(name, getInitParameter(name));
        }
        return initParameters;
    }

    /**
     * Adds a JavaScript service to be loaded at initialization.
     * 
     * @param service the service which will provide JavaScript content.
     */
    protected void addInitScript(Service service) {
        if (initScripts == null) {
            initScripts = new ArrayList();
        } else if (initScripts.contains(service)) {
            return;
        }
        
        services.add(service);
        initScripts.add(service);
    }
    
    /**
     * Adds a CSS style sheet to be loaded at initialization.
     * 
     * @param service the service which will provide the CSS content.
     */
    protected void addInitStyleSheet(Service service) {
        if (initStyleSheets == null) {
            initStyleSheets = new ArrayList();
        } else if (initStyleSheets.contains(service)) {
            return;
        }

        services.add(service);
        initStyleSheets.add(service);
    }
    
    protected final void setWebSocketConnectionHandler(WebSocketConnectionHandler handler) {
        this.wsHandler = handler;
        this.wsHandler.assignParent(this);
    }
    
    protected final void removeWebSocketConnectionHandler() {
        wsHandler = null;
    }
    
    public final boolean hasWebSocketConnectionHandler() {
        return wsHandler != null;
    }
    
    /**
     * Handles a GET request.
     *
     * @see #process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public final void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        process(request, response);
    }
    
    /**
     * Handles a POST request.
     *
     * @see #process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public final void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        process(request, response);
    }
    
    /**
     * Returns the service that corresponds to the specified Id.
     *
     * @param id The id of the service to return.
     * @return The service corresponding to the specified Id.
     */
    private static Service getService(String id, boolean hasInstance) {
        if (id == null) {
            if (hasInstance) {
                id = SERVICE_ID_DEFAULT;
            } else {
                id = SERVICE_ID_NEW_INSTANCE;
            }
        } else {
            if (!hasInstance) {
                id = SERVICE_ID_SESSION_EXPIRED;
            }
        }
        
        Service service = services.get(id);
        if (service == null) {
            if (SERVICE_ID_DEFAULT.equals(id)) {
                throw new RuntimeException("Service not registered: SERVICE_ID_DEFAULT");
            } else if (SERVICE_ID_NEW_INSTANCE.equals(id)) {
                throw new RuntimeException("Service not registered: SERVICE_ID_NEW_INSTANCE");
            } else if (SERVICE_ID_SESSION_EXPIRED.equals(id)) {
                throw new RuntimeException("Service not registered: SERVICE_ID_SESSION_EXPIRED");
            }
        }
        return service;
    }
    
    /**
     * Returns an iterator over initialization script services.
     * 
     * @return the iterator
     */
    public Iterator getInitScripts() {
        return initScripts == null ? null : Collections.unmodifiableCollection(initScripts).iterator();
    }
    
    /**
     * Returns an iterator over initialization script services.
     * 
     * @return the iterator
     */
    public Iterator getInitStyleSheets() {
        return initStyleSheets == null ? null : Collections.unmodifiableCollection(initStyleSheets).iterator();
    }

    /**
     * Returns the instance operating mode of the application, determining how the application will perform if it
     * is visited by multiple browser windows.
     * 
     * @return the operating mode, one of the following values:
     *         <ul>
     *          <li><code>INSTANCE_MODE_SINGLE</code> (the default) to allow only a single instance of the application</li>
     *          <li><code>INSTANCE_MODE_WINDOW</code> to allow multiple instances of the application per session, with new
     *           instances created for new browser windows</li>
     *          </ul>
     */
    public int getInstanceMode() {
        return INSTANCE_MODE_SINGLE;
    }
    
    /**
     * Creates a new <code>ApplicationInstance</code> for visitor to an 
     * application.
     * 
     * @return a new <code>ApplicationInstance</code>
     */
    public abstract ApplicationInstance newApplicationInstance();
    
    /**
     * Processes an HTTP request and generates a response.
     * 
     * @param request the incoming <code>HttpServletRequest</code>
     * @param response the outgoing <code>HttpServletResponse</code>
     */
    protected void process(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException {
        Connection conn = null;
        try {
            conn = new Connection(this, request, response);
            activeConnection.set(conn);
            String serviceId = request.getParameter(SERVICE_ID_PARAMETER);
            Service service = getService(serviceId, conn.getUserInstanceContainer() != null);
            if (service == null) {
                throw new ServletException("Service id \"" + serviceId + "\" not registered.");
            }
            int version = service.getVersion();
            
            // Set caching directives.
            if ((!DISABLE_CACHING) && version != Service.DO_NOT_CACHE) {
                // Setting all of the following (possibly with the exception of "Expires")
                // are *absolutely critical* in order to ensure proper caching of resources
                // with Internet Explorer 6.  Without "Last-Modified", IE6 appears to not
                // cache images properly resulting in an substantially greater than expected
                // performance impact.
                response.setHeader("Cache-Control", "max-age=3600, public");
                response.setDateHeader("Expires", System.currentTimeMillis() + (3600 * 1000));
                response.setDateHeader("Last-Modified", startupTime);
            } else if (version == Service.IE_HTTPS_CACHE_ONLY) {
                response.reset();
                response.setHeader("Pragma","cache");
            } else {
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Expires", "0");
            }
            
            service.service(conn);
            
        } catch (ServletException ex) {
        	// an error from any window must destroy the user instance
            if (conn != null) {
                try {
                	conn.disposeUserInstance();
                } catch (RuntimeException ex2) {
                    Log.log("Disposal of user instance due to exception in processing failed due to an error in disposal", ex2);
                }
            }
            processError(conn, request, response, ex);
        } catch (IOException ex) {
        	// an error from any window must destroy the user instance
            if (conn != null) {
                try {
                	conn.disposeUserInstance();
                } catch (RuntimeException ex2) {
                    Log.log("Disposal of user instance due to exception in processing failed due to an error in disposal", ex2);
                }
            }
            processError(conn, request, response, ex);
        } catch (RuntimeException ex) {
        	// an error from any window must destroy the user instance
            if (conn != null) {
                try {
                	conn.disposeUserInstance();
                } catch (RuntimeException ex2) {
                    Log.log("Disposal of user instance due to exception in processing failed due to an error in disposal", ex2);
                }
            }
            processError(conn, request, response, ex);
        } finally {
            activeConnection.set(null);
        }
    }
    
    /**
     * Exception handler for process() method. The current implementation writes an Exception ID to the client.
     * 
     * @param request The current HTTP request in progress
     * @param response The HTTP response
     * @param ex The exception triggering this error handleing.
     * @throws IOException May be thrown on issues writing to the HTTP response.
     */
    protected void processError(Connection conn, HttpServletRequest request, HttpServletResponse response, Exception ex) 
            throws IOException {
        if (conn != null) {
            try {
                conn.disposeUserInstance();
            } catch (Exception ignore) {
                //do nothing...
            }
        }

        String exceptionId = Uid.generateUidString();
        Log.log("Server Exception. ID: " + exceptionId, ex);
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Server Exception. ID: " + exceptionId);
    }
    
    private List cssFileNames = new LinkedList();
    
    /**
     * Adds a css file name to be added to the window html.
     */
    public void addCssFileName(String cssFileName) {
        cssFileNames.add(cssFileName);
    }
    
    /**
     * Returns an iterator over the CSS file names.
     * @return the iterator.
     */
    public Iterator getCssFileNames() {
        return cssFileNames.iterator();
    }

    /**
     * Returns whether this web container servlet instance is automatically
     * allowing asynchronous updates when multiple windows are active.
     * @return
     */
    public boolean getAllowAsyncWindowUpdates() {
        return allowAsyncWindowUpdates;
    }
}
