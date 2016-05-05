package nextapp.echo.webcontainer.service;

import java.io.IOException;

import nextapp.echo.webcontainer.Connection;
import nextapp.echo.webcontainer.Service;
import nextapp.echo.webcontainer.util.Resource;

/**
 * A Service for CSS StyleSheets that supports the application of a relative path to the
 * URLs in the StyleSheet.
 * 
 * @author Lloyd Colling
 */
public class CSSStyleSheetService implements Service {

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
     * @param relativePath
     *            the relative path to the stylesheet from the servlet
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

    private String id;
    private String content;
    private final String contentType = "text/css";

    public CSSStyleSheetService(String id, String content) {
        super();
        this.id = id;
        this.content = content;
    }

    public CSSStyleSheetService(String id, String content, String relativePath) {
        super();
        this.id = id;
        this.content = content;
        processImageURLs(relativePath);
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return 0;
    }

    public void service(Connection conn) throws IOException {
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
}
