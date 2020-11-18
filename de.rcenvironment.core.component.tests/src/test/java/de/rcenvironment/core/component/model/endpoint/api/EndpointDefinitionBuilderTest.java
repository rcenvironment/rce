/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointMetaDataDefinitionImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Alexander Weinert
 *
 */
public class EndpointDefinitionBuilderTest {

    /**
     * Tests that constructing a minimal endpoint works.
     */
    @Test
    public void buildingSucceedsWithMinimalParameters() {
        
        final EndpointDefinition definition = EndpointDefinition.inputBuilder()
            .defaultDatatype(DataType.ShortText)
            .allowedDatatype(DataType.ShortText)
            .build();
        
        Assert.assertEquals(EndpointType.INPUT, definition.getEndpointType());
    }
    
    /**
     * Constructs an endpoint using all parameters supported by the EndpointDefinitionBuilder and checks that the values are set correctly
     */
    @Test
    public void allParametersWork() {
        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        
        metadata.put("toplevelKey", new HashMap<>());
        metadata.get("toplevelKey").put("lowlevelKey1", "value1");
        metadata.get("toplevelKey").put("lowlevelKey2", "value2");

        final EndpointDefinition definition = EndpointDefinition.inputBuilder()
            .defaultDatatype(DataType.Boolean)
            .allowedDatatype(DataType.Boolean)
            .name("SomeEndpoint")
            .inputHandlings(Arrays.asList(InputDatumHandling.Constant, InputDatumHandling.Single))
            .defaultInputHandling(InputDatumHandling.Constant)
            .inputExecutionConstraints(Arrays.asList(InputExecutionContraint.Required, InputExecutionContraint.RequiredIfConnected))
            .defaultInputExecutionConstraint(InputExecutionContraint.Required)
            .metadata(metadata)
            .build();
        
        Assert.assertEquals("SomeEndpoint", definition.getName());
        Assert.assertEquals(2, definition.getInputDatumOptions().size());
        Assert.assertTrue(definition.getInputDatumOptions().contains(InputDatumHandling.Constant));
        Assert.assertTrue(definition.getInputDatumOptions().contains(InputDatumHandling.Single));
        Assert.assertEquals(InputDatumHandling.Constant, definition.getDefaultInputDatumHandling());
        Assert.assertEquals(2,  definition.getInputExecutionConstraintOptions().size());
        Assert.assertTrue(definition.getInputExecutionConstraintOptions().contains(InputExecutionContraint.Required));
        Assert.assertTrue(definition.getInputExecutionConstraintOptions().contains(InputExecutionContraint.RequiredIfConnected));
        Assert.assertEquals(InputExecutionContraint.Required, definition.getDefaultInputExecutionConstraint());
        Assert.assertNotNull(definition.getMetaDataDefinition());
        Assert.assertEquals(1, ((EndpointMetaDataDefinitionImpl)definition.getMetaDataDefinition()).getRawMetaData().size());
        Assert.assertTrue(((EndpointMetaDataDefinitionImpl)definition.getMetaDataDefinition()).getRawMetaData().containsKey("toplevelKey"));
        final Map<String, Object> actualMetadata = ((EndpointMetaDataDefinitionImpl)definition.getMetaDataDefinition()).getRawMetaData().get("toplevelKey");
        Assert.assertEquals(2, actualMetadata.size());
        Assert.assertTrue(actualMetadata.containsKey("lowlevelKey1"));
        Assert.assertEquals("value1", actualMetadata.get("lowlevelKey1"));
        Assert.assertTrue(actualMetadata.containsKey("lowlevelKey2"));
        Assert.assertEquals("value2", actualMetadata.get("lowlevelKey2"));
    }
    
    /**
     * Tests that building an endpoint fails if the given default input handling is not contained in the list of valid input handlings
     */
    @Test
    public void buildingFailsWithInvalidDefaultDatumHandling() {
        final EndpointDefinitionBuilder definitionBuilder = EndpointDefinition.inputBuilder()
            .defaultDatatype(DataType.Boolean)
            .allowedDatatype(DataType.Boolean)
            .name("SomeEndpoint")
            .inputHandlings(Arrays.asList(InputDatumHandling.Constant, InputDatumHandling.Single))
            .defaultInputHandling(InputDatumHandling.Queue);
        
        Exception exception = null;
        try {
            definitionBuilder.build();
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        
        Assert.assertNotNull(exception);
    }
    
    /**
     * Tests that an input builder fails if not default data type is set
     */
    @Test
    public void buildingFailsWithoutDefaultDataType() {
        final EndpointDefinitionBuilder builder = EndpointDefinition.inputBuilder();
        
        IllegalStateException exception = null;
        try {
            builder.build();
        } catch (IllegalStateException e) {
            exception = e;
        }
        
        Assert.assertNotNull(exception);
    }

}
