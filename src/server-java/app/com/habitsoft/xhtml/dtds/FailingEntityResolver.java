package com.habitsoft.xhtml.dtds;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FailingEntityResolver implements EntityResolver {

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        throw new SAXException("Entity should have been resolved from the cache: "+systemId);
    }

}
