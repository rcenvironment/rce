/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.XMLException;

/**
 * A test for the XMLSupport class.
 * 
 * TODO Moved here for 6.2 because the class XMLException could not be moved due to serialization issues. Should be moved back to
 * core.utils.common in 7.0.
 *
 * @author Brigitte Boden
 */
public class XMLSupportImplTest {

    /**
     * JUnit Rule for expected exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private XMLSupportServiceImpl xmlSupport;

    private File inputFile;

    private String inputString;

    private InputStream inputStream;

    private File invalidFile;

    private String invalidString;

    private InputStream invalidStream;

    private TempFileService tempFileService;

    private File testRootDir;

    private Log log;

    /**
     * Setup for the XMLSupport Test.
     * 
     * @throws Exception on error.
     */
    @Before
    public void setup() throws Exception {
        log = LogFactory.getLog(getClass());

        xmlSupport = new XMLSupportServiceImpl();
        inputFile = new File("src/test/resources/compareCPACS_1.xml");
        inputString = FileUtils.readFileToString(inputFile);
        inputStream = getClass().getClassLoader().getResourceAsStream("compareCPACS_1.xml");

        invalidFile = new File("src/test/resources/invalidFile.xml");
        invalidString = FileUtils.readFileToString(invalidFile);
        invalidStream = getClass().getClassLoader().getResourceAsStream("invalidFile.xml");

        // Setup root directory for testing
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        log.debug("Testing in temporary directory " + testRootDir.getAbsolutePath());
    }

    /**
     * Tests all the methods for reading XML.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testReadXML() throws Exception {
        // Valid input should be parsed correctly
        assertNotNull(xmlSupport.readXMLFromFile(inputFile));
        // test that all streams are properly closed
        assertFalse(de.rcenvironment.core.utils.common.FileUtils.isLocked(inputFile));
        assertNotNull(xmlSupport.readXMLFromStream(inputStream));
        assertNotNull(xmlSupport.readXMLFromString(inputString));

        // Null input should result in XMLException, no other Exceptions
        thrown.expect(XMLException.class);
        assertNull(xmlSupport.readXMLFromFile(null));
        assertNull(xmlSupport.readXMLFromStream(null));
        assertNull(xmlSupport.readXMLFromString(null));

        // Same for invalid inputs
        assertNull(xmlSupport.readXMLFromFile(invalidFile));
        // test that all streams are properly closed
        assertFalse(de.rcenvironment.core.utils.common.FileUtils.isLocked(invalidFile));
        assertNull(xmlSupport.readXMLFromStream(invalidStream));
        assertNull(xmlSupport.readXMLFromString(invalidString));
    }

    /**
     * Tests all the methods for writing XML.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testWriteXML() throws Exception {
        Document doc = xmlSupport.readXMLFromFile(inputFile);
        File file = tempFileService.createTempFileFromPattern("testfile*.xml");
        xmlSupport.writeXMLtoFile(doc, file);
        // test that all streams are properly closed
        assertFalse(de.rcenvironment.core.utils.common.FileUtils.isLocked(file));
        assertNotNull(FileUtils.readFileToString(file));
        String string = xmlSupport.writeXMLToString(doc);
        assertNotNull(string);
        String elementString = xmlSupport.writeXMLToString(doc.getDocumentElement());
        assertNotNull(elementString);

        // Test behavior for null values
        // Null input should result in XMLException, no other Exceptions
        thrown.expect(XMLException.class);
        Document nullDoc = null;
        Element nullElement = null;
        assertNull(xmlSupport.writeXMLToString(nullDoc));
        assertNull(xmlSupport.writeXMLToString(nullElement));
        xmlSupport.writeXMLtoFile(null, file);
        // test that all streams are properly closed
        assertFalse(de.rcenvironment.core.utils.common.FileUtils.isLocked(file));
        xmlSupport.writeXMLtoFile(doc, null);
    }

    /**
     * Tests methods for handling Documents.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testHandleDocuments() throws Exception {
        Document newDoc = xmlSupport.createDocument();
        assertNotNull(xmlSupport.createElement(newDoc, "element"));

        // Test behavior for null values
        // Null input should result in XMLException, no other Exceptions
        thrown.expect(XMLException.class);
        assertNull(xmlSupport.createElement(null, "element"));
        assertNull(xmlSupport.createElementTree(null, "xpath"));
        assertNull(xmlSupport.createElementTree(newDoc, null));
    }

}
