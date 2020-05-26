/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;

import de.rcenvironment.core.utils.common.FileUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.EMappingMode;
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.XMLMappingInformation;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;

/**
 * Test for XML Mapper. Uses methods from XMLSupport for handling XML files.
 * 
 * TODO Moved here for 6.2 because the class XMLException could not be moved due to serialization issues. Should be moved back to
 * core.utils.common in 7.0.
 *
 * @author Brigitte Boden
 * 
 */
public class XMLMapperImplTest {

    private static final String CONTENT_OF_MAPPING_IS_NOT_AS_EXPECTED = "Content of mapping is not as expected.";

    private static final String CPACS_AREA_XPATH = "/cpacs/vehicles/aircraft/model/reference/area";

    private static final String WRONG_TARGET_XPATH = "Wrong target xpath.";

    private static final String WRONG_SOURCE_XPATH = "Wrong source xpath.";

    private static final String WRONG_MAPPING_MODE = "Content should be 'MappingMode Delete'.";

    /**
     * JUnit Rule for expected exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final Log log = LogFactory.getLog(getClass());

    private XMLSupportService support;

    private XMLMapperServiceImpl xmlMapper;

    private File originCPACS;

    private File compareCPACS1;

    private File compareCPACS2;

    private File compareCPACSXSL;

    private File toolOutput;

    private File mappingInputSimple;

    private File mappingInputComplex;

    private File mappingOutput;

    private File xsltFile;

    private File resultFile;

    private TempFileService tempFileService;

    private File testRootDir;

    /**
     * Setup for the XML Mapper tests.
     * 
     * @throws Exception on error.
     */
    @Before
    public void setup() throws Exception {

        // Setup temp file service for testing
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        log.debug("Testing in temporary directory " + testRootDir.getAbsolutePath());

        xmlMapper = new XMLMapperServiceImpl();
        support = new XMLSupportServiceImpl();
        xmlMapper.bindXMLSupportService(support);

        originCPACS = new File("src/test/resources/CPACS.xml");
        compareCPACS1 = new File("src/test/resources/compareCPACS_1.xml");
        compareCPACS2 = new File("src/test/resources/compareCPACS_2.xml");
        compareCPACSXSL = new File("src/test/resources/compareCPACS_xsl.xml");
        toolOutput = new File("src/test/resources/toolOutput.xml");
        mappingInputComplex = new File("src/test/resources/mappingInputDocLoop.xml");
        mappingOutput = new File("src/test/resources/mappingOutputDoc.xml");
        xsltFile = new File("src/test/resources/mappingInputRaw.xsl");
        mappingInputSimple = new File("src/test/resources/mappingInput_newNamespace.xml");
        resultFile = tempFileService.createTempFileFromPattern("output*.xml");
    }
    
    /**
     * Clean up temp files.
     * 
     * @throws IOException on Error.
     */
    @After
    public void cleanup() throws IOException {
        tempFileService.disposeManagedTempDirOrFile(testRootDir);
        tempFileService.disposeManagedTempDirOrFile(resultFile);
    }

    /**
     * Test transforming a file with XSLT.
     * 
     * @throws XMLException on Error.
     */
    @Test
    public void testTransformXMLFileWithXSLT() throws XMLException {
        assertFalse(FileUtils.isLocked(originCPACS));
        assertFalse(FileUtils.isLocked(resultFile));
        assertFalse(FileUtils.isLocked(xsltFile));
        
        xmlMapper.transformXMLFileWithXSLT(originCPACS, resultFile, xsltFile, null);

        assertFalse(FileUtils.isLocked(originCPACS));
        assertFalse(FileUtils.isLocked(resultFile));
        assertFalse(FileUtils.isLocked(xsltFile));

        Document compareDoc = support.readXMLFromFile(compareCPACSXSL);
        Document target = support.readXMLFromFile(resultFile);
        target.normalizeDocument();
        compareDoc.normalizeDocument();
        assertTrue(CONTENT_OF_MAPPING_IS_NOT_AS_EXPECTED,
            support.writeXMLToString(target).equals(support.writeXMLToString(compareDoc)));
    }

    /**
     * Test if executing mapping is like expected (in a CPACS-manner). Problem with this Junit test: comparing XML is done via string.
     * However, sub-nodes are not defined in a special sequence. Therefore test may unsuccessful when order is not as expected.
     * 
     * @throws XPathExpressionException Thrown if mapping fails.
     * @throws XMLException Error in XML handling
     * 
     * @author Brigitte Boden
     * @author Markus Kunde (Code adapted from original class XMLMapperTest)
     * 
     */
    @Test
    public void testTransformXMLFileWithXMLMappingInformation() throws XMLException, XPathExpressionException {

        // like CPACS input mapping
        Document target = support.createDocument();
        Document compareDoc = support.readXMLFromFile(compareCPACS1);
        compareDoc.normalizeDocument();
        target.normalizeDocument();
        xmlMapper.transformXMLFileWithXMLMappingInformation(support.readXMLFromFile(originCPACS), target,
            support.readXMLFromFile(mappingInputSimple));
        assertTrue(CONTENT_OF_MAPPING_IS_NOT_AS_EXPECTED,
            support.writeXMLToString(target).equals(support.writeXMLToString(compareDoc)));

        // like CPACS output mapping
        target = support.readXMLFromFile(originCPACS);
        target.normalizeDocument();
        xmlMapper.transformXMLFileWithXMLMappingInformation(support.readXMLFromFile(toolOutput), target,
            support.readXMLFromFile(mappingOutput));
        compareDoc = support.readXMLFromFile(compareCPACS2);
        compareDoc.normalizeDocument();

        assertTrue(CONTENT_OF_MAPPING_IS_NOT_AS_EXPECTED,
            support.writeXMLToString(target).equals(support.writeXMLToString(compareDoc)));
    }
    
    /**
     * Tests if the all involved files are not locked after calling transformXMLFileWithXMLMappingInformation.
     * 
     * @throws XPathExpressionException Thrown if mapping fails.
     * @throws XMLException Error in XML handling
     * 
     * @author Tobias Rodehutskors
     */
    @Test
    public void testTransformXMLFileWithXMLMappingInformationForNotLocked() throws XPathExpressionException, XMLException {
        File target = new File(testRootDir, "target.xml");
        xmlMapper.transformXMLFileWithXMLMappingInformation(toolOutput, target, mappingOutput);
        assertFalse(FileUtils.isLocked(toolOutput));
        assertFalse(FileUtils.isLocked(target));
        assertFalse(FileUtils.isLocked(mappingOutput));

    }

    /**
     * Check handling of null values. They shouldn't cause uncatched NullPointerExceptions. XMLExceptions are expected.
     * 
     * @throws XMLException on Error.
     * @throws XPathExpressionException on Error.
     */
    @Test
    public void testWithNullValues() throws XMLException, XPathExpressionException {
        thrown.expect(XMLException.class);
        Document nullDoc = null;
        File nullFile = null;
        xmlMapper.transformXMLFileWithXMLMappingInformation(nullFile, nullFile, nullFile);
        xmlMapper.transformXMLFileWithXMLMappingInformation(originCPACS, nullFile, nullFile);
        xmlMapper.transformXMLFileWithXMLMappingInformation(nullFile, resultFile, nullFile);
        xmlMapper.transformXMLFileWithXMLMappingInformation(nullFile, nullFile, mappingInputSimple);
        xmlMapper.transformXMLFileWithXMLMappingInformation(nullFile, nullFile, nullDoc);
        xmlMapper.transformXMLFileWithXSLT(null, null, null, null);
    }

    /**
     * Test if correct mapping informations will be created.
     * 
     * @throws Exception on error.
     * 
     * @author Brigitte Boden
     * @author Markus Kunde (Code adapted from original class XMLMapperTest)
     * 
     */
    @Test
    public void testReadXMLMapping() throws Exception {
        List<XMLMappingInformation> inputSimple = xmlMapper.readXMLMapping(support.readXMLFromFile(mappingInputSimple));
        List<XMLMappingInformation> inputComplex = xmlMapper.readXMLMapping(support.readXMLFromFile(mappingInputComplex));
        List<XMLMappingInformation> output = xmlMapper.readXMLMapping(support.readXMLFromFile(mappingOutput));

        // Validation of mapping information length
        assertTrue("Length of mapping information in simple input is not correct.", inputSimple.size() == 1);
        assertTrue("Length of mapping information in complex input is not correct.", inputComplex.size() == 4);
        assertTrue("Length of mapping information in output is not correct.", output.size() == 1);

        // Validation of mapping information content
        // inputSimple
        assertTrue(WRONG_MAPPING_MODE, inputSimple.get(0).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, inputSimple.get(0).getSourceXPath().equals(CPACS_AREA_XPATH));
        assertTrue(WRONG_TARGET_XPATH, inputSimple.get(0).getTargetXPath().equals("/toolInput/data/var1"));

        // inputComplex
        assertTrue(WRONG_MAPPING_MODE, inputComplex.get(0).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, inputComplex.get(0).getSourceXPath().equals(CPACS_AREA_XPATH));
        assertTrue(WRONG_TARGET_XPATH, inputComplex.get(0).getTargetXPath().equals("/toolInput/data/var1"));

        assertTrue(WRONG_MAPPING_MODE, inputComplex.get(1).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, inputComplex.get(1).getSourceXPath().equals("/cpacs/vehicles/aircraft/model/wings/wing[1]"));
        assertTrue(WRONG_TARGET_XPATH, inputComplex.get(1).getTargetXPath().equals("/toolInput/data/wing[1]"));

        assertTrue(WRONG_MAPPING_MODE, inputComplex.get(2).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, inputComplex.get(2).getSourceXPath().equals("/cpacs/vehicles/aircraft/model/wings/wing[2]"));
        assertTrue(WRONG_TARGET_XPATH, inputComplex.get(2).getTargetXPath().equals("/toolInput/data/wing[2]"));

        assertTrue(WRONG_MAPPING_MODE, inputComplex.get(3).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, inputComplex.get(3).getSourceXPath().equals("/cpacs/vehicles/aircraft/model/wings/wing[3]"));
        assertTrue(WRONG_TARGET_XPATH, inputComplex.get(3).getTargetXPath().equals("/toolInput/data/wing[3]"));

        // output
        assertTrue(WRONG_MAPPING_MODE, output.get(0).getMode() == EMappingMode.Delete);
        assertTrue(WRONG_SOURCE_XPATH, output.get(0).getSourceXPath().equals("/toolOutput/data/result1"));
        assertTrue(WRONG_TARGET_XPATH, output.get(0).getTargetXPath().equals(CPACS_AREA_XPATH));

    }
}
