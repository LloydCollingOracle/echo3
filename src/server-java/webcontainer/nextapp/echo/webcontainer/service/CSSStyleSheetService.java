package nextapp.echo.webcontainer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nextapp.echo.webcontainer.Connection;
import nextapp.echo.webcontainer.ServerConfiguration;
import nextapp.echo.webcontainer.Service;
import nextapp.echo.webcontainer.util.Resource;

/**
 * A Service for CSS StyleSheets that supports the application of a relative path to the
 * URLs in the StyleSheet.
 * 
 * @author Lloyd Colling
 */
public class CSSStyleSheetService implements Service, StringVersionService {

    public static final int ALL = 1;
    public static final int BRAILLE = 2;
    public static final int EMBOSSED = 4;
    public static final int HANDHELD = 8;
    public static final int PRINT = 16;
    public static final int PROJECTION = 32;
    public static final int SCREEN = 64;
    public static final int SPEECH = 128;
    public static final int TTY = 256;
    public static final int TV = 512;

    /**
     * Creates a new <code>CSSStyleSheetService</code> based on the content in
     * the specified <code>CLASSPATH</code> resource. A runtime exception will
     * be thrown in the event the resource does not exist (it generally should
     * not be caught).
     * 
     * Please Note that all urls in the StyleSheet must be relative to the
     * Servlet location when this method is used.
     * 
     * @param id
     *            the <code>Service</code> identifier
     * @param resourceName
     *            the path to the content resource in the <code>CLASSPATH</code>
     * @return the created <code>CSSStyleSheetService</code>
     */
    public static CSSStyleSheetService forResource(String id,
            String resourceName) {
        String content = Resource.getResourceAsString(resourceName);
        return new CSSStyleSheetService(id, content);
    }

    /**
     * Creates a new <code>CSSStyleSheetService</code> based on the content in
     * the specified <code>CLASSPATH</code> resource with any image URLs updated
     * to reflect the relative location compared to the application Servlet. A
     * runtime exception will be thrown in the event the resource does not exist
     * (it generally should not be caught).
     * 
     * @param id
     *            the <code>Service</code> identifier
     * @param resourceName
     *            the path to the content resource in the <code>CLASSPATH</code>
     * @param relativePath
     *            the relative path to the stylesheet from the servlet
     * @return the created <code>CSSStyleSheetService</code>
     */
    public static CSSStyleSheetService forResource(String id,
            String resourceName, String relativePath) {
        String content = Resource.getResourceAsString(resourceName);
        return new CSSStyleSheetService(id, content, relativePath);
    }
    
    /**
     * Creates a new <code>CSSStyleSheetService</code> based on the content in
     * the specified <code>CLASSPATH</code> resource. A runtime exception will
     * be thrown in the event the resource does not exist (it generally should
     * not be caught).
     * 
     * Please Note that all urls in the StyleSheet must be relative to the
     * Servlet location when this method is used.
     * 
     * @param id
     *            the <code>Service</code> identifier
     * @param resourceName
     *            the path to the content resource in the <code>CLASSPATH</code>
     * @param media
                  the supported media
     * @return the created <code>CSSStyleSheetService</code>
     */
    public static CSSStyleSheetService forResource(String id,
            String resourceName, int media) {
        String content = Resource.getResourceAsString(resourceName);
        return new CSSStyleSheetService(id, content, media);
    }

    /**
     * Creates a new <code>CSSStyleSheetService</code> based on the content in
     * the specified <code>CLASSPATH</code> resource with any image URLs updated
     * to reflect the relative location compared to the application Servlet. A
     * runtime exception will be thrown in the event the resource does not exist
     * (it generally should not be caught).
     * 
     * @param id
     *            the <code>Service</code> identifier
     * @param resourceName
     *            the path to the content resource in the <code>CLASSPATH</code>
     * @param relativePath
     *            the relative path to the stylesheet from the servlet
     * @param media
                  the supported media
     * @return the created <code>CSSStyleSheetService</code>
     */
    public static CSSStyleSheetService forResource(String id,
            String resourceName, String relativePath, int media) {
        String content = Resource.getResourceAsString(resourceName);
        return new CSSStyleSheetService(id, content, relativePath, media);
    }

    private String id;
    private String content;
    private final String contentType = "text/css";
    private int media = ALL;
    private String stringVersion;

    public CSSStyleSheetService(String id, String content) {
        super();
        this.id = id;
        this.content = content;
        
        if (ServerConfiguration.CSS_CACHING_ENABLED) {
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

    public CSSStyleSheetService(String id, String content, String relativePath) {
        this(id, content);
        processImageURLs(relativePath);
    }

    public CSSStyleSheetService(String id, String content, int media) {
        this(id, content);
        this.media = media;
    }

    public CSSStyleSheetService(String id, String content, String relativePath, int media) {
        this(id, content, relativePath);
        this.media = media;
    }

    public String getMediaCSV() {
        StringBuffer buffer = new StringBuffer();
        if((media & ALL) == ALL) {
            addToCSVBuffer(buffer, "all");
        }
        if((media & BRAILLE) == BRAILLE) {
            addToCSVBuffer(buffer, "braille");
        }
        if((media & EMBOSSED) == EMBOSSED) {
            addToCSVBuffer(buffer, "embossed");
        }
        if((media & HANDHELD) == HANDHELD) {
            addToCSVBuffer(buffer, "handheld");
        }
        if((media & PRINT) == PRINT) {
            addToCSVBuffer(buffer, "print");
        }
        if((media & PROJECTION) == PROJECTION) {
            addToCSVBuffer(buffer, "projection");
        }
        if((media & SCREEN) == SCREEN) {
            addToCSVBuffer(buffer, "screen");
        }
        if((media & SPEECH) == SPEECH) {
            addToCSVBuffer(buffer, "speech");
        }
        if((media & TTY) == TTY) {
            addToCSVBuffer(buffer, "tty");
        }
        if((media & TV) == TV) {
            addToCSVBuffer(buffer, "tv");
        }
        return buffer.toString();
    }

    private void addToCSVBuffer(StringBuffer buffer, String media) {
        if(buffer.length() > 0) {
            buffer.append(", ");
        }
        buffer.append(media);
    }

    public String getId() {
        return id;
    }
    
    public int getVersion() {
        if (ServerConfiguration.CSS_CACHING_ENABLED) {
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
        if (ServerConfiguration.CSS_CACHING_ENABLED) {
            return stringVersion;
        }
        else {
            return null;
        }
    }

    public void service(Connection conn) throws IOException {
        /*
         * Apply our specific cache seconds value if it has been specified
         * using the system property.
         */
        if (ServerConfiguration.CSS_CACHING_ENABLED && ServerConfiguration.CSS_CACHE_SECONDS != -1l) {
            conn.getResponse().setHeader("Cache-Control", "max-age=" + String.valueOf(ServerConfiguration.CSS_CACHE_SECONDS) + ", public");
            conn.getResponse().setDateHeader("Expires", System.currentTimeMillis() + (ServerConfiguration.CSS_CACHE_SECONDS * 1000));
        }
        conn.getResponse().setContentType(contentType);
        conn.getWriter().print(content);
    }

    /**
     * Processes the CSS StyleSheet and replaces all image URLs so that they
     * start with the relativePath. For example,
     * <code>URL(../images/MyImage.png)</code> if processed with relative path
     * <code>resources/css</code> would result in
     * <code>URL(resources/css/../images/MyImage.png</code>.
     * 
     * @param relativePath
     */
    private void processImageURLs(String relativePath) {
        if (!relativePath.endsWith("/")) {
            relativePath = relativePath + "/";
        }
        int indexOfURL = content.indexOf("url");
        if (indexOfURL == -1)
            return;
        int lastIndexOfURL = indexOfURL;
        StringBuffer endContent = new StringBuffer();
        endContent.append(content.substring(0, indexOfURL));
        
        do {
            int openParen = content.indexOf("(", indexOfURL);
            int closeParen = content.indexOf(")", indexOfURL);
            String imgUrl = content.substring(openParen + 1, closeParen);
            imgUrl = imgUrl.trim();
            if (imgUrl.matches("'.*'"))
                imgUrl = imgUrl.substring(1, imgUrl.length() - 1);
            
            endContent.append("url('");
            endContent.append(relativePath);
            endContent.append(imgUrl);
            endContent.append("')");
            
            indexOfURL = content.indexOf("url", indexOfURL + 1);
            if (indexOfURL != -1) {
                lastIndexOfURL = indexOfURL;
                endContent.append(content.substring(closeParen + 1, indexOfURL));
            }
        } while (indexOfURL != -1);
        
        if (lastIndexOfURL != -1) {
            int closeParen = content.indexOf(")", lastIndexOfURL);
            endContent.append(content.substring(closeParen + 1));
        }
        this.content = endContent.toString();
    }

    public void setMedia(int media) {
        this.media = media;
    }

    public int getMedia() {
        return media;
    }
}
