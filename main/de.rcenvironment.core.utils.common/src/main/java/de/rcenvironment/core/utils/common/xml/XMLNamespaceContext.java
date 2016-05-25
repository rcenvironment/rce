/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Document;

/**
 * Class for looking up a namespace context in XML documents.
 * @author Holger Cornelsen
 */
public class XMLNamespaceContext implements NamespaceContext {

    /**
     * The document included.
     */
    private Document myDoc;

    /**
     * Constructor.
     * @param xmlDoc The document where to look up namespaces.
     */
    public XMLNamespaceContext(final Document xmlDoc) {
        myDoc = xmlDoc;
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(String)
     */
    @Override
    public String getNamespaceURI(final String prefix) {
        return myDoc.lookupNamespaceURI(prefix);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see javax.xml.namespace.NamespaceContext#getPrefix(String)
     */
    @Override
    public String getPrefix(final String namespaceURI) {
        return myDoc.lookupPrefix(namespaceURI);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(String)
     */
    @Override
    public Iterator<?> getPrefixes(final String namespaceURI) {
        return null;
    }

}
