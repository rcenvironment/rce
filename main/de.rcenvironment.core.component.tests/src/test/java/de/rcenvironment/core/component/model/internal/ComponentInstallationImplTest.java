/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescriptionStubFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionProviderStubFactory;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Test cases for {@link ComponentInstallationImpl}.
 * @author Doreen Seider
 */
public class ComponentInstallationImplTest {

    /**
     * Tests if the {@link ComponentInstallationImpl} is serialized correctly regarding configuration keys.
     * 
     * @throws IOException on error
     * @throws JsonGenerationException on error
     * @throws JsonMappingException on error
     * @throws JsonParseException on error
     */
    @Test
    public void testComponentInstallationSerialization() throws JsonParseException, JsonMappingException,
    JsonGenerationException, IOException {
        
        ConfigurationDefinitionImpl configDef = ConfigurationDescriptionStubFactory.createConfigurationDefinitionFromTestFile();
        
        Set<ConfigurationExtensionDefinitionImpl> configExtDefs = ConfigurationDescriptionStubFactory
            .createConfigurationExtensionDefinitionsFromTestFiles();
        
        EndpointDefinitionsProviderImpl inputsProvider = EndpointDefinitionProviderStubFactory
            .createInputDefinitionsProviderFromTestFile();

        ComponentInterfaceImpl cinter = new ComponentInterfaceImpl();
        cinter.setIdentifier("interface-id");
        List<String> identifiers = new ArrayList<>();
        identifiers.add("interface-id");
        identifiers.add("interface-id.old");
        cinter.setIdentifiers(identifiers);
        cinter.setDisplayName("display name");
        cinter.setConfigurationDefinition(configDef);
        cinter.setConfigurationExtensionDefinitions(configExtDefs);
        cinter.setInputDefinitionsProvider(inputsProvider);
        ComponentRevisionImpl cr = new ComponentRevisionImpl();
        cr.setComponentInterface(cinter);
        ComponentInstallationImpl ci = new ComponentInstallationImpl();
        ci.setComponentRevision(cr);
        ci.setInstallationId("install-id");
        
        ObjectMapper mapper = new ObjectMapper();
        ComponentInstallationImpl otherCi = mapper.readValue(mapper.writeValueAsString(ci), ComponentInstallationImpl.class);
        
        assertEquals(0, otherCi.compareTo(ci));
        
        ComponentInterface otherCinter = otherCi.getComponentRevision().getComponentInterface();
        
        assertEquals(cinter.getIdentifiers().size(), otherCinter.getIdentifiers().size());
        
        // check configuration
        assertEquals(cinter.getConfigurationDefinition().getConfigurationKeys().size(),
            otherCinter.getConfigurationDefinition().getConfigurationKeys().size());
        assertEquals(cinter.getConfigurationExtensionDefinitions().size(),
            otherCinter.getConfigurationExtensionDefinitions().size());
        
        for (ConfigurationExtensionDefinition configExtDef : otherCinter.getConfigurationExtensionDefinitions()) {
            assertEquals(1, configExtDef.getConfigurationKeys().size());
        }
        
        assertEquals(cinter.getConfigurationDefinition().getDefaultValue(ConfigurationDescriptionStubFactory.PORT),
            otherCinter.getConfigurationDefinition().getDefaultValue(ConfigurationDescriptionStubFactory.PORT));
        
        // check endpointsd
        EndpointDefinition dynInputDef = cinter.getInputDefinitionsProvider()
            .getDynamicEndpointDefinition(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1);
        
        EndpointDefinition otherDynInputDef = otherCinter.getInputDefinitionsProvider()
            .getDynamicEndpointDefinition(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1);
        
        assertNotNull(otherDynInputDef);
        assertEquals(dynInputDef.getMetaDataDefinition().getMetaDataKeys().size(),
            otherDynInputDef.getMetaDataDefinition().getMetaDataKeys().size());
        
    }
}
