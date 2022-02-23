/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.xml.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;

/**
 * Test for the EndpointXMLUtilsImpl.
 * 
 * This unit test only tests for the correct exception handling for wrong parameters. The behavior for correct inputs is already tested in
 * the {@link XMLMergerComponentTest}.
 * 
 * TODO review the test cases if reasonable. do they actually create a reasonable safety net for {@link EndpointXMLServiceImpl}?
 *
 * @author Brigitte Boden
 */
public class EndpointXMLServiceImplTest {

    private static final String NULL_POINTER_EXCEPTION_EXPECTED = "NullPointerException expected";

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
        
        private static final String OUTPUT_1 = "out_1";
        
        @Override
        public Set<String> getOutputs() {
            Set<String> outputs = new HashSet<>();
            outputs.add(OUTPUT_1);
            return outputs;
        }
        
        @Override
        public boolean isDynamicOutput(String outputName) {
            return outputName.equals(OUTPUT_1);
        }

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
        try {
            endpointXMLService.updateXMLWithInputs(null, inputs, context);
            fail(NULL_POINTER_EXCEPTION_EXPECTED);
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        // removed as the NPE will occur later on, always after the XMLSupportService mock is used that always throws an exception
//        try {
//            endpointXMLService.updateXMLWithInputs(originCPACS, inputs, null);
//            fail(NULL_POINTER_EXCEPTION_EXPECTED);
//        } catch (NullPointerException e) {
//            assertTrue(true);
//        }
        try {
            endpointXMLService.updateXMLWithInputs(originCPACS, inputs, context);
            fail("ComponentException expected");
        } catch (ComponentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test for testUpdateOutputsFromXML.
     * 
     * @throws Exception on error.
     */
    @Test
    public void testUpdateOutputsFromXML() throws Exception {
        try {
            endpointXMLService.updateOutputsFromXML(null, context);
            fail(NULL_POINTER_EXCEPTION_EXPECTED);
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            endpointXMLService.updateOutputsFromXML(originCPACS, null);
            fail(NULL_POINTER_EXCEPTION_EXPECTED);
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            endpointXMLService.updateOutputsFromXML(originCPACS, context);
            fail("ComponentException expected");
        } catch (ComponentException e) {
            assertTrue(true);
        }
       
    }
}
