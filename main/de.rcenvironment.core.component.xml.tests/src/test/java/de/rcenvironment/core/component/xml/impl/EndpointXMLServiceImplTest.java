/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.xml.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.xml.XMLComponentConstants;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.incubator.xml.XMLException;
import de.rcenvironment.core.utils.incubator.xml.api.XMLSupportService;

/**
 * Test for the EndpointXMLUtilsImpl.
 * 
 * This unit test only tests for the correct exception handling for wrong parameters. The behavior for correct inputs is already tested in
 * the XMLMergerIntegrationTest.
 *
 * @author Brigitte Boden
 */
public class EndpointXMLServiceImplTest {

    /**
     * Junit Exception rule.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private EndpointXMLServiceImpl endpointXMLService;

    private XMLSupportService support;

    private EndpointXMLServiceImplTestComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    private File originCPACS;

    /**
     * Context mock for XMLMerger Component test.
     * 
     * @author Brigitte Boden
     */
    private final class EndpointXMLServiceImplTestComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = 5310024048572964077L;

    }

    /**
     * Setup for the test.
     * 
     * @throws Exception on error.
     */
    @Before
    public void setup() throws Exception {
        endpointXMLService = new EndpointXMLServiceImpl();
        support = EasyMock.createMock(XMLSupportService.class);
        EasyMock.expect(support.readXMLFromFile(EasyMock.anyObject(File.class))).andThrow(
            new XMLException("This mock only throws exceptions"));
        EasyMock.replay(support);

        endpointXMLService.bindXMLSupportService(support);
        context = new EndpointXMLServiceImplTestComponentContextMock();

        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
        originCPACS = new File("src/test/resources/CPACS.xml");
    }

    /**
     * Test for testUpdateXMLWithInputs.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testUpdateXMLWithInputs() throws Exception {
        Map<String, TypedDatum> inputs = new HashMap<String, TypedDatum>();
        inputs.put("input", typedDatumFactory.createFloat(0.0));
        context.setConfigurationValue(XMLComponentConstants.CONFIG_KEY_XPATH, "xpath");

        exception.expect(ComponentException.class);
        endpointXMLService.updateXMLWithInputs(null, inputs, context);
        endpointXMLService.updateXMLWithInputs(originCPACS, inputs, context);
        endpointXMLService.updateXMLWithInputs(originCPACS, inputs, null);
    }

    /**
     * Test for testUpdateOutputsFromXML.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testUpdateOutputsFromXML() throws Exception {
        exception.expect(ComponentException.class);
        endpointXMLService.updateOutputsFromXML(null, context);
        endpointXMLService.updateOutputsFromXML(originCPACS, context);
        endpointXMLService.updateOutputsFromXML(originCPACS, null);
    }
}
