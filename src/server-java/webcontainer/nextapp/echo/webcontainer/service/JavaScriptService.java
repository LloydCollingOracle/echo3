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

package nextapp.echo.webcontainer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nextapp.echo.webcontainer.Connection;
import nextapp.echo.webcontainer.ServerConfiguration;
import nextapp.echo.webcontainer.Service;
import nextapp.echo.webcontainer.util.GZipCompressor;
import nextapp.echo.webcontainer.util.JavaScriptCompressor;
import nextapp.echo.webcontainer.util.Resource;

/**
 * A service which renders <code>JavaScript</code> resource files.
 */
public class JavaScriptService 
implements Service {
    /**
     * Creates a new <code>JavaScript</code> service from the specified
     * resource in the <code>CLASSPATH</code>.
     * 
     * @param id the <code>Service</code> id
     * @param resourceName the <code>CLASSPATH</code> resource name containing
     *        the JavaScript content
     * @return the created <code>JavaScriptService</code>
     */
    public static JavaScriptService forResource(String id, String resourceName) {
        String content = Resource.getResourceAsString(resourceName);
        return new JavaScriptService(id, content);
    }
    
    public static JavaScriptService forResources(String id, String[] resourceNames) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < resourceNames.length; ++i) {
            out.append(Resource.getResourceAsString(resourceNames[i]));
            out.append("\n\n");
        }
        return new JavaScriptService(id, out.toString());
    }

    /**
     * Creates JavaScript services from the specified resources
     * @param id the <code>Service</code> id
     * @param resourceNames the <code>CLASSPATH</code> resource name containing
     *        the JavaScript content
     * @param strip the javascript of comments and whitespace
     * @param zip compress the javascript
     * @return the created <code>JavaScriptService</code>
     */
    public static JavaScriptService forResources(String id, String[] resourceNames, boolean strip, boolean zip) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < resourceNames.length; ++i) {
            out.append(Resource.getResourceAsString(resourceNames[i]));
            out.append("\n\n");
        }
        return new JavaScriptService(id, out.toString(), strip, zip);
    }

    /** <code>Service</code> identifier. */
    private String id;
    
    /** The JavaScript content in plain text. */
    private String content;
    
    /** The JavaScript content in GZip compressed form. */
    private byte[] gzipContent;
    
    /** The MD5 hash in the case that caching is enabled */
    private String stringVersion;
    
    /**
     * Creates a new <code>JavaScriptService</code>.
     * 
     * @param id the <code>Service</code> id
     * @param content the <code>JavaScript content</code>
     */
    public JavaScriptService(String id, String content) {
        this(id, content, true, true);
    }
    
    /**
     * Creates a new <code>JavaScriptService</code>.
     * 
     * @param id the <code>Service</code> id
     * @param content the <code>JavaScript content</code>
     */
    public JavaScriptService(String id, String content, boolean strip, boolean gzip) {
        super();
        this.id = id;
//        this.content = ServerConfiguration.JAVASCRIPT_COMPRESSION_ENABLED ? JavaScriptCompressor.compress(content) : content;
        this.content = content;
        if (strip && ServerConfiguration.JAVASCRIPT_COMPRESSION_ENABLED) {
        	this.content = JavaScriptCompressor.compress(this.content);
        }
        if (gzip) {
	        try {
	            gzipContent = GZipCompressor.compress(this.content);
	        } catch (IOException ex) {
	            // Should not occur.
	            throw new RuntimeException("Exception compressing JavaScript source.", ex);
	        }
        }
        
        if (ServerConfiguration.JS_CACHING_ENABLED) {
        	try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(content.getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                stringVersion = hash.toString(16);
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("Unable to generate MD5 hash for javascript contents - caching will not be enabled");
            }
        }
    }
    
    /**
     * Creates a new <code>JavaScriptService</code>.
     * 
     * @param id the <code>Service</code> id
     * @param content the <code>JavaScript content</code>
     * @param gzippedContent the gzip-compressed <code>JavaScript content</code>
     */
    public JavaScriptService(String id, String content, byte[] gzippedContent) {
        super();
        this.id = id;
        this.content = ServerConfiguration.JAVASCRIPT_COMPRESSION_ENABLED ? JavaScriptCompressor.compress(content) : content;
        gzipContent = gzippedContent;
        
        if (ServerConfiguration.JS_CACHING_ENABLED) {
        	try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(content.getBytes());
                BigInteger hash = new BigInteger(1, md5.digest());
                stringVersion = hash.toString(16);
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("Unable to generate MD5 hash for javascript contents - caching will not be enabled");
            }
        }
    }
    
    /**
     * @see Service#getId()
     */
    public String getId() {
        return id;
    }
    
    /**
     * <code>DO_NOT_CACHE</code> is returned for <code>JavaScript</code>
     * to avoid possibility of ever running out-of-date JavaScript in the
     * event an application is updated and redeployed. 
     * 
     * @see Service#getVersion()
     */
    public int getVersion() {
        if (ServerConfiguration.JS_CACHING_ENABLED) {
            return 0;
        }
        else {
            return DO_NOT_CACHE;
        }
    }
    
    /**
     * @see StringVersionService#getVersionAsString()
     */
    public String getVersionAsString() {
        if (ServerConfiguration.JS_CACHING_ENABLED) {
            return stringVersion;
        }
        else {
            return null;
        }
    }
    
    /**
     * @see Service#service(nextapp.echo.webcontainer.Connection)
     */
    public void service(Connection conn) 
    throws IOException {
        /*
         * Apply our specific cache seconds value if it has been specified
         * using the system property.
         */
        if (ServerConfiguration.JS_CACHING_ENABLED && ServerConfiguration.JS_CACHE_SECONDS != -1l) {
           conn.getResponse().setHeader("Cache-Control", "max-age=" + String.valueOf(ServerConfiguration.JS_CACHE_SECONDS) + ", public");
           conn.getResponse().setDateHeader("Expires", System.currentTimeMillis() + (ServerConfiguration.JS_CACHE_SECONDS * 1000));
        }
        String userAgent = conn.getRequest().getHeader("user-agent");
        if (!ServerConfiguration.ALLOW_IE_COMPRESSION && (userAgent == null || userAgent.indexOf("MSIE") != -1 || gzipContent == null)) {
            // Due to behavior detailed Microsoft Knowledge Base Article Id 312496, 
            // all HTTP compression support is disabled for this browser.
            // Due to the fact that ClientProperties information is not necessarily 
            // available at this stage, browsers which provide deceitful user-agent 
            // headers will also be affected.
            servicePlain(conn);
        } else {
            String acceptEncoding = conn.getRequest().getHeader("accept-encoding");
            if (acceptEncoding != null && acceptEncoding.indexOf("gzip") != -1 && gzipContent != null) {
                serviceGZipCompressed(conn);
            } else {
                servicePlain(conn);
            }
        }
    }
    
    /**
     * Renders the JavaScript resource using GZip encoding.
     * 
     * @param conn the relevant <code>Connection</code>
     */
    private void serviceGZipCompressed(Connection conn) 
    throws IOException {
        conn.getResponse().setContentType("text/plain");
        conn.getResponse().setHeader("Content-Encoding", "gzip");
        conn.getOutputStream().write(gzipContent);
    }
    
    /**
     * Renders the JavaScript resource WITHOUT using GZip encoding.
     * 
     * @param conn the relevant <code>Connection</code>
     */
    private void servicePlain(Connection conn) 
    throws IOException {
        conn.getResponse().setContentType("text/plain");
        conn.getWriter().print(content);
    }
}
