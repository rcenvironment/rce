/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;

/**
 * Test case for {@link EndpointDescriptionsManager}.
 * 
 * @author Doreen Seider
 */
public class EndpointDescriptionsManagerTest {

    private static final String DECLARED = "declared";

    private static final String EXIST = "exist";

    private static final String INVALID = "invalid";

    private EndpointDescriptionsManager inputManager;

    private EndpointDescriptionsManager outputManager;

    /**
     * Set up.
     * 
     * @throws IOException on error
     */
    @Before
    public void setUp() throws IOException {
        
        Set<EndpointDefinitionImpl> outputDescriptions = ComponentUtils
            .extractStaticEndpointDefinition(getClass().getResourceAsStream("/outputs.json"),
                new ArrayList<InputStream>(), EndpointType.OUTPUT);
        outputDescriptions.addAll(ComponentUtils
            .extractDynamicEndpointDefinition(getClass().getResourceAsStream("/outputs.json"),
                new ArrayList<InputStream>(), EndpointType.OUTPUT));

        EndpointDefinitionsProviderImpl outputProvider = new EndpointDefinitionsProviderImpl();
        outputProvider.setEndpointDefinitions(outputDescriptions);
        outputManager = new EndpointDescriptionsManager(outputProvider, EndpointType.OUTPUT);
        
        EndpointDefinitionsProviderImpl inputProvider = EndpointDefinitionProviderStubFactory
            .createInputDefinitionsProviderFromTestFile();
        inputManager = new EndpointDescriptionsManager(inputProvider, EndpointType.INPUT);
        
        // TODO improve injecting the TypedDatumService into the EndpointDescription class
        EndpointDescription description = outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);
        description.bindTypedDatumService(new TypedDatumServiceDefaultStub());
    }

    /** Test. */
    @Test
    public void testAccessStaticOutputs() {
        assertEquals(2, outputManager.getStaticEndpointDefinitions().size());

        EndpointDefinition definition = outputManager.getStaticEndpointDefinition(
            EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);
        assertEquals(DataType.Integer, definition.getDefaultDataType());
        assertEquals(EndpointType.OUTPUT, definition.getEndpointType());
        assertEquals(null, definition.getIdentifier());
        assertEquals(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME, definition.getName());
        List<DataType> possibleDataTypes = definition.getPossibleDataTypes();
        assertEquals(3, possibleDataTypes.size());
        assertTrue(possibleDataTypes.contains(DataType.Boolean));
        assertTrue(possibleDataTypes.contains(DataType.ShortText));
        assertTrue(possibleDataTypes.contains(DataType.Integer));
        assertTrue(definition.isStatic());
        assertFalse(definition.isReadOnly());
        assertTrue(definition.isNameReadOnly());
    }

    /** Test. */
    @Test
    public void testAccessDynamicOutputs() {
        assertEquals(1, outputManager.getDynamicEndpointDefinitions().size());

        EndpointDefinition definition = outputManager.getDynamicEndpointDefinition(
            EndpointDefinitionProviderStubFactory.DYNAMICOUTPUTID);
        assertEquals(DataType.FileReference, definition.getDefaultDataType());
        assertEquals(EndpointType.OUTPUT, definition.getEndpointType());
        assertEquals(EndpointDefinitionProviderStubFactory.DYNAMICOUTPUTID, definition.getIdentifier());
        assertEquals(null, definition.getName());
        List<DataType> possibleDataTypes = definition.getPossibleDataTypes();
        assertEquals(2, possibleDataTypes.size());
        assertTrue(possibleDataTypes.contains(DataType.FileReference));
        assertTrue(possibleDataTypes.contains(DataType.SmallTable));
        assertFalse(definition.isStatic());
        assertTrue(definition.isReadOnly());
        assertFalse(definition.isNameReadOnly());
    }

    /** Test. */
    @Test
    public void testAccessStaticInputs() {
        assertEquals(1, inputManager.getStaticEndpointDefinitions().size());

        EndpointDefinition definition = inputManager.getStaticEndpointDefinition(
            EndpointDefinitionProviderStubFactory.STATICINPUTNAME);
        assertEquals(DataType.Integer, definition.getDefaultDataType());
        assertEquals(EndpointType.INPUT, definition.getEndpointType());
        assertEquals(null, definition.getIdentifier());
        assertEquals(EndpointDefinitionProviderStubFactory.STATICINPUTNAME, definition.getName());
        List<DataType> possibleDataTypes = definition.getPossibleDataTypes();
        assertEquals(1, possibleDataTypes.size());
        assertTrue(possibleDataTypes.contains(DataType.Integer));
        assertTrue(definition.isStatic());
        assertFalse(definition.isReadOnly());
    }

    /** Test. */
    @Test
    public void testAccessDynamicInputs() {
        assertEquals(2, inputManager.getDynamicEndpointDefinitions().size());

        EndpointDefinition definition = inputManager.getDynamicEndpointDefinition(
            EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1);
        assertEquals(DataType.Vector, definition.getDefaultDataType());
        assertEquals(EndpointType.INPUT, definition.getEndpointType());
        assertEquals(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1, definition.getIdentifier());
        assertEquals(null, definition.getName());
        List<DataType> possibleDataTypes = definition.getPossibleDataTypes();
        assertEquals(3, possibleDataTypes.size());
        assertTrue(possibleDataTypes.contains(DataType.Float));
        assertTrue(possibleDataTypes.contains(DataType.Vector));
        assertTrue(possibleDataTypes.contains(DataType.Matrix));
        assertFalse(definition.isStatic());
        assertFalse(definition.isReadOnly());

        EndpointMetaDataDefinition metaDataDescription = definition.getMetaDataDefinition();

        assertEquals(3, metaDataDescription.getMetaDataKeys().size());
        
        String metaDataKey = "myMetaDataKey";
        assertTrue(metaDataDescription.getMetaDataKeys().contains(metaDataKey));
        assertEquals(3, metaDataDescription.getPossibleValues(metaDataKey).size());
        assertEquals("required", metaDataDescription.getDefaultValue(metaDataKey));
        assertEquals(0, metaDataDescription.getGuiPosition(metaDataKey));
        assertEquals("Meta data name", metaDataDescription.getGuiName(metaDataKey));
        assertEquals(Visibility.shown, metaDataDescription.getVisibility(metaDataKey));
        assertNull(metaDataDescription.getGuiActivationFilter(metaDataKey));
        assertNull(metaDataDescription.getActivationFilter(metaDataKey));

        try {
            metaDataDescription.getDefaultValue("unknown key");
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        
        String filteredMetaDataKey = "myFilteredMetaDataKey";
        assertTrue(metaDataDescription.getMetaDataKeys().contains(filteredMetaDataKey));
        assertNotNull(metaDataDescription.getGuiActivationFilter(filteredMetaDataKey));
        assertNotNull(metaDataDescription.getActivationFilter(filteredMetaDataKey));
        assertTrue(metaDataDescription.getGuiActivationFilter(filteredMetaDataKey).containsKey("goal"));
        assertTrue(metaDataDescription.getGuiActivationFilter(filteredMetaDataKey).get("goal").contains("Solve for"));
        assertTrue(metaDataDescription.getActivationFilter(filteredMetaDataKey).containsKey("scriptLanguage"));
        assertTrue(metaDataDescription.getActivationFilter(filteredMetaDataKey).get("scriptLanguage").contains("Python"));
        
        String additionalMetaDataKey = "additionalMetaDataKey";
        assertTrue(metaDataDescription.getMetaDataKeys().contains(additionalMetaDataKey));
    }

    /** Test. */
    @Test
    public void testAddAndRemoveDynamicEndpointDescriptions() {

        String inputName1 = "input name 1";
        String inputName2 = "input name 2";
        String inputName3 = "input name 3";
        DataType inputType = DataType.Float;
        Map<String, String> inputMetaData = new HashMap<String, String>();
        inputMetaData.put("myMetaDataKey", "optional");

        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
            inputName1, inputType, inputMetaData);

        // invalid name
        try {
            inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
                inputName1, inputType, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(EXIST));
        }

        // invalid endpoint id
        try {
            inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICOUTPUTID,
                "output name", inputType, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(DECLARED));
        }

        // invalid data type
        try {
            inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
                inputName2, DataType.SmallTable, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(INVALID));
        }

        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1, inputName2,
            inputType, inputMetaData);
        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID2,
            inputName3, DataType.Matrix, inputMetaData);

        assertNotNull(inputManager.removeDynamicEndpointDescription(inputName1));
        assertNotNull(inputManager.removeDynamicEndpointDescription(inputName2));

        assertNull(inputManager.removeDynamicEndpointDescription("unknown name"));

    }

    /** Test. */
    @Test
    public void testEditDynamicEndpointDescription() {

        String inputName = "input name";
        String newInputName = "new input name";
        DataType inputType = DataType.Float;
        DataType newInputType = DataType.Vector;
        Map<String, String> inputMetaData = new HashMap<String, String>();

        EndpointDescription description = inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
            inputName, inputType, inputMetaData);
        assertEquals(inputType, description.getDataType());

        assertNull(inputManager.getEndpointDescription(newInputName));

        description = inputManager.editDynamicEndpointDescription(inputName, newInputName, newInputType, inputMetaData);
        assertEquals(newInputType, description.getDataType());

        description = inputManager.getEndpointDescription(newInputName);
        assertEquals(newInputType, description.getDataType());

        assertNull(inputManager.getEndpointDescription(inputName));

        description = inputManager.editDynamicEndpointDescription(newInputName, newInputName, inputType, inputMetaData);
        assertEquals(inputType, description.getDataType());

        // unknow name
        try {
            inputManager.editDynamicEndpointDescription("unknown input name", inputName, DataType.ShortText, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(EXIST));
        }

        // invalid data type
        try {
            inputManager.editDynamicEndpointDescription(newInputName, inputName, DataType.ShortText, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(INVALID));
        }

        // invalid new name
        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
            inputName, inputType, inputMetaData);
        try {
            inputManager.editDynamicEndpointDescription(newInputName, inputName, inputType, inputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }
    }

    /** Test. */
    @Test
    public void testEditStaticEndpointDescription() {

        DataType outputType = DataType.Integer;
        DataType newOutputType = DataType.ShortText;
        Map<String, String> outputMetaData = new HashMap<String, String>();

        EndpointDescription description = outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);
        assertEquals(outputType, description.getDataType());

        description = outputManager.editStaticEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME,
            newOutputType, outputMetaData);
        assertEquals(newOutputType, description.getDataType());

        assertNotNull(outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME));

        // unknown name
        try {
            outputManager.editStaticEndpointDescription("unknown output name", newOutputType, outputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(EXIST));
        }

        // invalid data type
        try {
            outputManager.editStaticEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME,
                DataType.Matrix, outputMetaData);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(INVALID));
        }

    }

    /** Test. */
    @Test
    public void testManagingEndpointDescription() {

        assertEquals(2, outputManager.getStaticEndpointDescriptions().size());

        assertEquals(1, inputManager.getStaticEndpointDescriptions().size());
        assertEquals(0, inputManager.getDynamicEndpointDescriptions().size());
        assertEquals(1, inputManager.getEndpointDescriptions().size());

        String inputName11 = "input name 1";
        String inputName12 = "input name 2";
        String inputName23 = "input name 3";
        DataType inputType1 = DataType.Float;
        DataType inputType2 = DataType.Matrix;
        Map<String, String> inputMetaData = new HashMap<String, String>();

        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
            inputName11, inputType1, inputMetaData);
        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1,
            inputName12, inputType1, inputMetaData);
        inputManager.addDynamicEndpointDescription(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID2,
            inputName23, inputType2, inputMetaData);

        assertEquals(2, outputManager.getStaticEndpointDescriptions().size());

        assertEquals(1, inputManager.getStaticEndpointDescriptions().size());
        assertEquals(3, inputManager.getDynamicEndpointDescriptions().size());
        assertEquals(4, inputManager.getEndpointDescriptions().size());

        inputManager.removeDynamicEndpointDescription(inputName12);

        assertEquals(2, inputManager.getDynamicEndpointDescriptions().size());
        assertEquals(3, inputManager.getEndpointDescriptions().size());

        inputManager.removeDynamicEndpointDescription(inputName11);
        inputManager.removeDynamicEndpointDescription(inputName23);

        assertEquals(0, inputManager.getDynamicEndpointDescriptions().size());
        assertEquals(1, inputManager.getEndpointDescriptions().size());
    }

    /** Test. */
    @Test
    public void testManagingConnectedDataTypes() {

        EndpointDescription description = outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);

        assertTrue(description.isDataTypeValid(DataType.Boolean));
        assertFalse(description.isDataTypeValid(DataType.Matrix));

        outputManager.addConnectedDataType(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME, DataType.Float);

        description = outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);
        assertFalse(description.isDataTypeValid(DataType.ShortText));

        outputManager.addConnectedDataType(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME, DataType.Float);
        outputManager.removeConnectedDataType(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME, DataType.Float);

        assertFalse(description.isDataTypeValid(DataType.ShortText));

        outputManager.removeConnectedDataType(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME, DataType.Float);

        description = outputManager.getEndpointDescription(EndpointDefinitionProviderStubFactory.STATICOUTPUTNAME);

        assertTrue(description.isDataTypeValid(DataType.ShortText));

    }
}
