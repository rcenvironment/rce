/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.xml;

import java.io.File;
import java.io.InputStream;

import junit.framework.TestCase;

import org.dom4j.DocumentException;

/**
 * 
 * Test cases for the class <code>XMLIOSupport</code>.
 * 
 * @author Andre Nurzenski
 */
public class XMLIOSupportTest extends TestCase {

    /**
     * Message that indicates that a test has failed because no exception was thrown.
     */
    private static final String EXCEPTION_MESSAGE = "Should raise a DocumentException";

    /**
     * The filename of the XML file to read.
     */
    private static final String XML_INPUT_FILENAME = "src/test/resources/authorization.xml";

    /**
     * The schema definition for the used XML file.
     */
    private static final String XML_SCHEMA_DEFINITION = "src/test/resources/authorization.xsd";

    /**
     * An input stream containing XML data.
     */
    private InputStream myXMLInputStream = null;

    /**
     * A <code>File</code> representation of the XML file to read.
     */
    private File myXMLInputFile = null;

    /**
     * A <code>File</code> representation of the XML schema definition file to read.
     */
    private File myXMLSchemaDefinitionFile = null;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myXMLInputStream = getClass().getResourceAsStream("/authorization.xml");
        myXMLInputFile = new File(XML_INPUT_FILENAME);
        myXMLSchemaDefinitionFile = new File(XML_SCHEMA_DEFINITION);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myXMLInputStream.close();
        myXMLInputFile = null;
        myXMLSchemaDefinitionFile = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test if a XML file can be read.
     * 
     * @throws Exception
     *             if an exception occurs.
     */
    public void testReadXMLInputStreamForSuccess() throws Exception {
        XMLIOSupport.readXML(myXMLInputStream);
    }

    /**
     * 
     * Test if a XML file can be read.
     * 
     * @throws Exception
     *             if an exception occurs.
     */
    public void testReadXMLFilenameForSuccess() throws Exception {
        XMLIOSupport.readXML(XML_INPUT_FILENAME);
    }

    /**
     * 
     * Test if a XML file can be read.
     * 
     * @throws Exception
     *             if an exception occurs.
     */
    public void testReadXMLFileForSuccess() throws Exception {
        XMLIOSupport.readXML(myXMLInputFile);
    }

    /**
     * 
     * Test if a XML file can be read and validated.
     * 
     * @throws Exception
     *             if an exception occurs.
     */
    public void testReadWithValidationXMLFilenameForSuccess() throws Exception {
        XMLIOSupport.readXML(XML_SCHEMA_DEFINITION, XML_INPUT_FILENAME);
    }

    /**
     * 
     * Test if a XML file can be read and validated.
     * 
     * @throws Exception
     *             if an exception occurs.
     */
    public void testReadWithValidationXMLFileForSuccess() throws Exception {
        XMLIOSupport.readXML(myXMLSchemaDefinitionFile, myXMLInputFile);
    }

    /*
     * #################### Test for failure ####################
     */

    /**
     * 
     * Test if a document exception is raised while reading a non-XML file.
     * 
     */
    public void testReadXMLInputStreamForFailure() {
        try {
            XMLIOSupport.readXML(getClass().getResourceAsStream("/rce.cer"));
            fail(EXCEPTION_MESSAGE);
        } catch (DocumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test if a document exception is raised while reading a non-XML file.
     * 
     */
    public void testReadXMLFilenameForFailure() {
        try {
            XMLIOSupport.readXML("src/test/resources/rce.cer");
            fail(EXCEPTION_MESSAGE);
        } catch (DocumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test if a document exception is raised while reading a non-XML file.
     * 
     */
    public void testReadXMLFileForFailure() {
        try {
            XMLIOSupport.readXML(new File("src/test/resources/rce.cer"));
            fail(EXCEPTION_MESSAGE);
        } catch (DocumentException e) {
            assertTrue(true);
        }
    }

    /*
     * #################### Test for sanity ####################
     */

}
