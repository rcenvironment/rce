/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.xml.internal;

/**
 * Provides configuration of this bundle and initializes default configuration.
 * 
 * @author Doreen Seider
 * @author Tobias Menden
 */
public final class XMLAuthorizationConfiguration {

    private String xmlDocument = "authorization.xml";

    public String getXmlFile() {
        return xmlDocument;
    }
    
    public void setXmlFile(String value) {
        this.xmlDocument = value;
    }
}
