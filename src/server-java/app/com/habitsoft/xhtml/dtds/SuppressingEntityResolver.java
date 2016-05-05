package com.habitsoft.xhtml.dtds;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SuppressingEntityResolver implements EntityResolver {
    public SuppressingEntityResolver() {
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return new InputSource(new StringReader(""));
    }

}
