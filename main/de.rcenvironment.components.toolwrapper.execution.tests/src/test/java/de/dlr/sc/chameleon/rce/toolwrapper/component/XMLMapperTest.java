/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.component;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.utils.common.xml.EMappingMode;
import de.rcenvironment.core.utils.common.xml.XMLMappingInformation;
import de.rcenvironment.core.utils.incubator.xml.XMLException;

/**
 * Junit test for class XMLMapper.
 * 
 * @author Markus Kunde
 */
@SuppressWarnings("deprecation") //Tests the deprecated class XMLMapper
public class XMLMapperTest {

    private static final String CPACS_AREA_XPATH = "/cpacs/vehicles/aircraft/model/reference/area";

    private static final String WRONG_TARGET_XPATH = "Wrong target xpath.";

    private static final String WRONG_SOURCE_XPATH = "Wrong source xpath.";

    private static final String WRONG_MAPPING_MODE = "Content should be 'MappingMode Delete'.";

    private XMLMapper xmlMapper;

    private XMLHelper xmlHelper;

    private File originCPACS;

    private File compareCPACS1;

    private File compareCPACS2;

    private File toolOutput;

    private File mappingInputSimple;

    private File mappingInputComplex;

    private File mappingOutput;

    /**
     * SetUp-Method.
     * 
     * @throws Exception exception
     */
    @Before
    public void setUp() throws Exception {
        xmlMapper = new XMLMapper();
        xmlHelper = new XMLHelper();
        originCPACS = new File("src/test/resources/CPACS.xml");
        compareCPACS1 = new File("src/test/resources/compareCPACS_1.xml");
        compareCPACS2 = new File("src/test/resources/compareCPACS_2.xml");
        toolOutput = new File("src/test/resources/toolOutput.xml");
        mappingInputSimple = new File("src/test/resources/mappingInputDoc.xml");
        mappingInputComplex = new File("src/test/resources/mappingInputDocLoop.xml");
        mappingOutput = new File("src/test/resources/mappingOutputDoc.xml");
    }

    /**
     * Test if correct mapping informations will be created.
     * 
     * @throws XMLException Error in XML handling
     * @throws ComponentException Mapping error.
     * 
     */
    @Test
    public void testReadXMLMapping() throws XMLException, ComponentException {
        List<XMLMappingInformation> inputSimple = xmlMapper.readXMLMapping(xmlHelper.readXMLFromFile(mappingInputSimple));
        List<XMLMappingInformation> inputComplex = xmlMapper.readXMLMapping(xmlHelper.readXMLFromFile(mappingInputComplex));
        List<XMLMappingInformation> output = xmlMapper.readXMLMapping(xmlHelper.readXMLFromFile(mappingOutput));

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

    /**
     * Test if executing mapping is like expected (in a CPACS-manner). Problem with this Junit test: comparing XML is done via string.
     * However, sub-nodes are not defined in a special sequence. Therefore test may unsuccessful when order is not as expected.
     * 
     * @throws XPathExpressionException Thrown if mapping fails.
     * @throws XMLException Error in XML handling
     * @throws ComponentException Mapping error.
     * 
     */
    @Test
    public void testMap() throws XPathExpressionException, XMLException, ComponentException {

        List<XMLMappingInformation> inputSimple = xmlMapper.readXMLMapping(xmlHelper.readXMLFromFile(mappingInputSimple));
        List<XMLMappingInformation> output = xmlMapper.readXMLMapping(xmlHelper.readXMLFromFile(mappingOutput));

        // like CPACS input mapping
        Document target = xmlHelper.createDocument();
        Document compareDoc = xmlHelper.readXMLFromFile(compareCPACS1);
        compareDoc.normalizeDocument();
        target.normalizeDocument();
        xmlMapper.map(xmlHelper.readXMLFromFile(originCPACS), target, inputSimple);
        assertTrue("Content of mapping is not as expected.",
            xmlHelper.writeXMLToString(target).equals(xmlHelper.writeXMLToString(compareDoc)));

        // like CPACS output mapping
        target = xmlHelper.readXMLFromFile(originCPACS);
        target.normalizeDocument();
        xmlMapper.map(xmlHelper.readXMLFromFile(toolOutput), target, output);
        compareDoc = xmlHelper.readXMLFromFile(compareCPACS2);
        compareDoc.normalizeDocument();

        assertTrue("Content of mapping is not as expected.",
            xmlHelper.writeXMLToString(target).equals(xmlHelper.writeXMLToString(compareDoc)));

    }
}
