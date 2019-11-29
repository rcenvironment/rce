/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.execution;

import java.io.File;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.xml.api.XMLMapperService;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.core.utils.common.xml.impl.XMLSupportServiceImpl;

/**
 * Unit test for the XML Merger Component. This test uses mocks for the XML services. It does not contain many test cases because most of
 * the logic can only be tested with the actual XMLSupport, so most of the tests for this component are in the XMLMergerIntegrationTest.
 *
 * @author Brigitte Boden
 */
public class XMLMergerComponentTest {

    private static final String ENDPOINT_NAME_XML = "XML";

    private static final String ENDPOINT_NAME_XML_TO_INTEGRATE = XmlMergerComponentConstants.INPUT_NAME_XML_TO_INTEGRATE;

    /**
     * Junit Exception rule.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ComponentTestWrapper component;

    private XMLMergerComponentContextMock context;

    private ComponentDataManagementService componentDataManagementServiceMock;

    private File originCPACS;

    private File xmlToIntegrate;

    private TypedDatumFactory typedDatumFactory;

    /**
     * Context mock for XMLMerger Component test.
     *
     * @author Brigitte Boden
     */
    private final class XMLMergerComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = -2806639411734497853L;

    }

    /**
     * Set up the test.
     * 
     * @throws Exception e
     */
    @Before
    public void setup() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();

        context = new XMLMergerComponentContextMock();
        componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);
        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
        component = new ComponentTestWrapper(new XmlMergerComponent(), context);

        context.addSimulatedInput(ENDPOINT_NAME_XML, ENDPOINT_NAME_XML, DataType.FileReference, false, null);
        context.addSimulatedInput(ENDPOINT_NAME_XML_TO_INTEGRATE, ENDPOINT_NAME_XML_TO_INTEGRATE, DataType.FileReference, false, null);
        context.addSimulatedOutput(ENDPOINT_NAME_XML, ENDPOINT_NAME_XML, DataType.FileReference, false, null);

        XMLSupportService support = EasyMock.createMock(XMLSupportService.class);
        XMLMapperService mapper = EasyMock.createMock(XMLMapperService.class);
        context.addService(XMLSupportService.class, support);
        context.addService(XMLMapperService.class, mapper);

        EasyMock.expect(support.readXMLFromString(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        Document emptyDoc = new XMLSupportServiceImpl().createDocument();
        EasyMock.expect(support.readXMLFromFile(EasyMock.anyObject(File.class))).andReturn(emptyDoc).anyTimes();
        support.writeXMLtoFile(EasyMock.anyObject(Document.class), EasyMock.anyObject(File.class));
        EasyMock.expectLastCall();
        EasyMock.replay(support);
        EasyMock.replay(mapper);

        originCPACS = new File("src/test/resources/CPACS.xml");
        xmlToIntegrate = new File("src/test/resources/xmlToIntegrate.xml");
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
    }

    /**
     * Test with no input.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testNoInputs() throws ComponentException {
        component.start();
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with null mapping. A component exception is expected, we should not get an uncatched NullPointerException.
     * 
     * @throws Exception e
     * 
     */
    @Test
    public void testWithNullMapping() throws Exception {

        String mapping = null;

        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);
        context.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, mapping);
        context.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);

        component.start();

        context.setInputValue(ENDPOINT_NAME_XML,
            typedDatumFactory.createFileReference(originCPACS.getAbsolutePath(), originCPACS.getName()));
        context.setInputValue(ENDPOINT_NAME_XML_TO_INTEGRATE,
            typedDatumFactory.createFileReference(xmlToIntegrate.getAbsolutePath(), xmlToIntegrate.getName()));

        exception.expect(ComponentException.class);
        component.processInputs();

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

}
