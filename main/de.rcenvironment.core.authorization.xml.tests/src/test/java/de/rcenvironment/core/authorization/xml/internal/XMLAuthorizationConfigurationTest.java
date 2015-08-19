/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.xml.internal;

import junit.framework.TestCase;

/**
 * 
 * Test case for the class <code>XMLAuthorizationConfiguration</code>.
 * 
 * @author Doreen Seider
 */
public class XMLAuthorizationConfigurationTest extends TestCase {

    /**
     * The class under test.
     */
    private XMLAuthorizationConfiguration myAuthorizationConfiguration = null;

    @Override
    public void setUp() throws Exception {
        myAuthorizationConfiguration = new XMLAuthorizationConfiguration();
        myAuthorizationConfiguration.setXmlFile(XMLAuthorizationMockFactory.RELATIVE_XML_DOCUMENT);
    } 
    
    /**
     * 
     * Tests getting the store for success.
     * 
     */
    public void testGetDocumentForSuccess() {
        String document = myAuthorizationConfiguration.getXmlFile();
        assertNotNull(document);
    }

    /**
     * 
     * Tests getting the store for sanity.
     * 
     */
    public void testGetDocumentForSanity() {
        String document = myAuthorizationConfiguration.getXmlFile();
        assertEquals(XMLAuthorizationMockFactory.RELATIVE_XML_DOCUMENT, document);
    }
}
