/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.component.xml.api.EndpointXMLService;
import de.rcenvironment.core.component.xml.impl.EndpointXMLServiceImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.core.utils.common.xml.impl.XMLMapperServiceImpl;
import de.rcenvironment.core.utils.common.xml.impl.XMLSupportServiceImpl;

/**
 * Integration test for the XML Merger Component and the underlying XML Support.
 *
 * @author Brigitte Boden
 */
public class XMLMergerIntegrationTest {

    private static final String DYN_INPUT_ID = "dyn_input_id";

    private static final String XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT = "XML Mapping didn't produce the expected result.";

    private static final String DYN_INPUT_NAME = "dyn_input";

    private static final String VARIABLE_XPATH = "variable.xpath";

    private static final String ENDPOINT_NAME_XML = "XML";

    private static final String ENDPOINT_NAME_XML_TO_INTEGRATE = XmlMergerComponentConstants.INPUT_NAME_XML_TO_INTEGRATE;
    
    private static final String ENDPOINT_NAME_MAPPING_FILE = XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE;

    /**
     * Junit Exception rule.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private TempFileService tempFileService;

    private final Log log = LogFactory.getLog(getClass());

    private XMLMergerIntegrationTestComponentContextMock context;

    private ComponentTestWrapper component;

    private ComponentDataManagementService componentDataManagementServiceMock;

    private TypedDatumFactory typedDatumFactory;

    private File testRootDir;

    private File originCPACS;

    private File xmlToIntegrate;

    private File mappingRules;
    
    private File mappingRulesXml;

    private File invalidFile;

    private File expectedResult;
    
    private File expectedResultXml;
    
    private File expectedResultForDynInput;

    private File expectedResultForNonExistentXPath;
    
    private XMLSupportService xmlSupport;

    /**
     * Context mock for XMLMerger Component test.
     * 
     * @author Brigitte Boden
     */
    private final class XMLMergerIntegrationTestComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = 1714212907147291860L;

        @Override
        public String getInstanceName() {
            return "XMLMergerTestInstance";
        }

    }

    private IAnswer<Void> copyReferenceToLocalFileAnswer = new IAnswer<Void>() {

        @Override
        public Void answer() throws Throwable {
            String reference = (String) EasyMock.getCurrentArguments()[0];
            File targetfile = (File) EasyMock.getCurrentArguments()[1];

            FileUtils.copyFile(new File(reference), targetfile);
            return null;
        }
    };

    private IAnswer<FileReferenceTD> createFileReferenceTDFromLocalFileAnswer = new IAnswer<FileReferenceTD>() {

        @Override
        public FileReferenceTD answer() throws Throwable {
            final File file = (File) EasyMock.getCurrentArguments()[1];
            final String filename = (String) EasyMock.getCurrentArguments()[2];

            // Copy to testRootDir as the temp file will be deleted by processInputs()
            final File targetFile = new File(testRootDir, filename);
            FileUtils.copyFile(file, targetFile);

            FileReferenceTD reference = new FileReferenceTD() {

                @Override
                public DataType getDataType() {
                    return DataType.FileReference;
                }

                @Override
                public void setLastModified(Date lastModified) {}

                @Override
                public void setFileSize(long filesize) {}

                @Override
                public Date getLastModified() {
                    return null;
                }

                @Override
                public long getFileSizeInBytes() {
                    return 0;
                }

                @Override
                public String getFileReference() {
                    return targetFile.getAbsolutePath();
                }

                @Override
                public String getFileName() {
                    return filename;
                }
            };

            return reference;
        }
    };

    /**
     * Set up the test.
     * 
     * @throws Exception e
     */
    @Before
    public void setup() throws Exception {
        context = new XMLMergerIntegrationTestComponentContextMock();
        XMLSupportServiceImpl xmlSupportService = new XMLSupportServiceImpl();
        XMLMapperServiceImpl mapper = new XMLMapperServiceImpl();
        mapper.bindXMLSupportService(xmlSupportService);
        EndpointXMLServiceImpl endpointXMLService = new EndpointXMLServiceImpl();
        endpointXMLService.bindXMLSupportService(xmlSupportService);
        context.addService(XMLSupportService.class, xmlSupportService);
        context.addService(XMLMapperService.class, mapper);
        context.addService(EndpointXMLService.class, endpointXMLService);
        
        componentDataManagementServiceMock = EasyMock.createNiceMock(ComponentDataManagementService.class);
        componentDataManagementServiceMock.copyReferenceToLocalFile(EasyMock.anyObject(String.class),
            EasyMock.anyObject(File.class),
            EasyMock.anyObject(InstanceNodeSessionId.class));
        EasyMock.expectLastCall().andAnswer(copyReferenceToLocalFileAnswer).anyTimes();

        componentDataManagementServiceMock.createFileReferenceTDFromLocalFile(EasyMock.anyObject(ComponentContext.class),
            EasyMock.notNull(File.class), EasyMock.notNull(String.class));
        EasyMock.expectLastCall().andAnswer(createFileReferenceTDFromLocalFileAnswer).anyTimes();

        EasyMock.replay(componentDataManagementServiceMock);

        // Setup temp file service for testing
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        log.debug("Testing in temporary directory " + testRootDir.getAbsolutePath());

        originCPACS = new File("src/test/resources/CPACS.xml");
        xmlToIntegrate = new File("src/test/resources/xmlToIntegrate.xml");
        mappingRules = new File("src/test/resources/xsltMapping.xsl");
        mappingRulesXml = new File("src/test/resources/mappingInputDoc.xml");
        invalidFile = new File("src/test/resources/xsltMapping.xsl");
        expectedResult = new File("src/test/resources/expectedResult.xml");
        expectedResultXml = new File("src/test/resources/expectedResult2.xml");
        expectedResultForDynInput = new File("src/test/resources/expectedResultForDynInput.xml");
        expectedResultForNonExistentXPath = new File("src/test/resources/expectedResultForNonExistentXPath.xml");

        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
        context.addSimulatedInput(ENDPOINT_NAME_XML, ENDPOINT_NAME_XML, DataType.FileReference, false, null);
        context.addSimulatedInput(ENDPOINT_NAME_XML_TO_INTEGRATE, ENDPOINT_NAME_XML_TO_INTEGRATE, DataType.FileReference, false, null);
        context.addSimulatedOutput(ENDPOINT_NAME_XML, ENDPOINT_NAME_XML, DataType.FileReference, false, null);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
        
        component = new ComponentTestWrapper(new XmlMergerComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
        
        xmlSupport = xmlSupportService;

    }

    /**
     * Test with standard inputs (no dynamic inputs/outputs).
     * 
     * @throws Exception e
     */
    @Test
    public void testXSLTMappingWithStandardInput() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        component.processInputs();

        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResult);
        expectedDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }
    
    /**
     * Test with standard inputs and mapping file as input (no dynamic inputs/outputs).
     * 
     * @throws Exception e
     */
    @Test
    public void testXSLTMappingWithStandardInputAndMappingFileAsInput() throws Exception {

        context.addSimulatedInput(ENDPOINT_NAME_MAPPING_FILE, ENDPOINT_NAME_MAPPING_FILE, DataType.FileReference, true, null);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));
        context.setInputValue(ENDPOINT_NAME_MAPPING_FILE,
            typedDatumFactory.createFileReference(mappingRules.getAbsolutePath(), mappingRules.getName()));
        

        component.processInputs();

        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResult);
        expectedDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with standard inputs (no dynamic inputs/outputs).
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithStandardInput() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRulesXml);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        component.processInputs();
        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResultXml);
        expectedDoc.normalizeDocument();
        resultDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }
    
    /**
     * Test with standard inputs and mapping file as input (no dynamic inputs/outputs).
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithStandardInputAndMappingFileAsInput() throws Exception {

        context.addSimulatedInput(ENDPOINT_NAME_MAPPING_FILE, ENDPOINT_NAME_MAPPING_FILE, DataType.FileReference, true, null);
        
        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));
        context.setInputValue(ENDPOINT_NAME_MAPPING_FILE,
            typedDatumFactory.createFileReference(mappingRulesXml.getAbsolutePath(), mappingRulesXml.getName()));

        component.processInputs();
        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResultXml);
        expectedDoc.normalizeDocument();
        resultDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with invalid XSLT mapping.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testWithInvalidMappingXSLT() throws ComponentException {

        String mapping = "invalid mapping string";

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        exception.expect(ComponentException.class);
        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with invalid XML mapping.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testWithInvalidMappingXML() throws ComponentException {

        String mapping = "invalid mapping string";

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        exception.expect(ComponentException.class);
        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with invalid inputs/mappings (no dynamic inputs/outputs).
     * 
     * @throws Exception e
     * 
     */
    @Test
    public void testWithInvalidInputs() throws Exception {

        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(invalidFile.getAbsolutePath(), invalidFile.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(invalidFile.getAbsolutePath(), invalidFile.getName()));

        exception.expect(ComponentException.class);
        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with dynamic input.
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithDynamicInput() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(VARIABLE_XPATH, "/cpacs/vehicles/aircraft/model[@uID='0.0']");
        context.addSimulatedInput(DYN_INPUT_NAME, DYN_INPUT_ID, DataType.Float, true, metadata);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        context.setInputValue(DYN_INPUT_NAME, typedDatumFactory.createFloat(4));

        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResultForDynInput);
        expectedDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));
    }

    /**
     * Test with dynamic input.
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithDynamicOutput() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(VARIABLE_XPATH, "/cpacs/header/cpacsVersion");
        final String dynOutputName = "dyn_output";
        context.addSimulatedOutput(dynOutputName, "dyn_output_id", DataType.Float, true, metadata);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        component.processInputs();

        assertEquals(1, context.getCapturedOutput(dynOutputName).size());
        assertEquals("2.0", context.getCapturedOutput(dynOutputName).get(0).toString());

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with dynamic input.
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithDynamicInputInvalidXPath() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(VARIABLE_XPATH, "/cpacs/vehicles/aircraft/invalid´´Xpath");
        context.addSimulatedInput(DYN_INPUT_NAME, DYN_INPUT_ID, DataType.Float, true, metadata);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        context.setInputValue(DYN_INPUT_NAME, typedDatumFactory.createFloat(4));

        exception.expect(ComponentException.class);
        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with dynamic input.
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithDynamicInputNonExistentXPath() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(VARIABLE_XPATH, "/cpacs/vehicles/aircraft/newNode");
        context.addSimulatedInput(DYN_INPUT_NAME, DYN_INPUT_ID, DataType.Float, true, metadata);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        context.setInputValue(DYN_INPUT_NAME, typedDatumFactory.createFloat(4));

        component.processInputs();
        File resultFile = new File(((FileReferenceTD) context.getCapturedOutput(ENDPOINT_NAME_XML).get(0)).getFileReference());

        Document resultDoc = xmlSupport.readXMLFromFile(resultFile);
        Document expectedDoc = xmlSupport.readXMLFromFile(expectedResultForNonExistentXPath);
        expectedDoc.normalizeDocument();
        resultDoc.normalizeDocument();
        assertTrue(XML_MAPPING_DIDN_T_PRODUCE_THE_EXPECTED_RESULT,
            xmlSupport.writeXMLToString(resultDoc).equals(xmlSupport.writeXMLToString(expectedDoc)));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    /**
     * Test with dynamic input.
     * 
     * @throws Exception e
     */
    @Test
    public void testXMLMappingWithDynamicOutputInvalidXpath() throws Exception {

        // Read Mapping file to String
        String mapping = FileUtils.readFileToString(mappingRules);

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(VARIABLE_XPATH, "/cpacs/header/invalidXpath");
        final String dynOutputName = "dyn_output";
        context.addSimulatedOutput(dynOutputName, "dyn_output_id", DataType.Float, true, metadata);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        exception.expect(ComponentException.class);
        component.processInputs();

        if (context.getCapturedOutput(dynOutputName).size() != 1) {
            fail("Wrong number of outputs");
        } else {
            assertEquals("2.0", context.getCapturedOutput(dynOutputName).get(0).toString());
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

}
