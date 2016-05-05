package com.habitsoft.xhtml.dtds;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XhtmlEntityResolver implements EntityResolver {
    static final String XHTML_PREFIX="http://www.w3.org/TR/xhtml1/DTD/";
    final EntityResolver next;
    
    public XhtmlEntityResolver(EntityResolver next) {
        this.next = next;
    }

    public XhtmlEntityResolver() {
        this(null);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if(systemId.startsWith(XHTML_PREFIX)) {
            InputSource inputSource = new InputSource(systemId);
            inputSource.setPublicId(publicId);
            inputSource.setByteStream(getClass().getResourceAsStream(systemId.substring(XHTML_PREFIX.length())));
            return inputSource;
        }
        if(next != null)
            return next.resolveEntity(publicId, systemId);
        else
            return null;
    }
}
