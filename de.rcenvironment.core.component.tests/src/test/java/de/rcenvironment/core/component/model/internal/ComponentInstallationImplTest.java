/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
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
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Test cases for {@link ComponentInstallationImpl}.
 * 
 * @author Doreen Seider
 */
public class ComponentInstallationImplTest {

    /**
     * Tests if the {@link ComponentInstallationImpl} is serialized correctly regarding configuration keys, endpoints etc.
     * 
     * Note: This test doesn't cover backwards compatibility checks.
     * 
     * @throws IOException on error
     * @throws JsonGenerationException on error
     * @throws JsonMappingException on error
     * @throws JsonParseException on error
     */
    @Test
    public void testComponentInstallationSerialization() throws JsonParseException, JsonMappingException,
        JsonGenerationException, IOException {

        Integer maxInstances = Integer.valueOf(4);
        String readOnlyConfigKey = "someReadOnlyConfigKey";
        String readOnlyConfigValue = "someReadOnlyConfigValue";

        ConfigurationDefinitionImpl configDefImpl = ConfigurationDescriptionStubFactory.createConfigurationDefinitionFromTestFile();

        Set<ConfigurationExtensionDefinitionImpl> configExtDefsImpl = ConfigurationDescriptionStubFactory
            .createConfigurationExtensionDefinitionsFromTestFiles();

        EndpointDefinitionsProviderImpl inputsProviderImpl = EndpointDefinitionProviderStubFactory
            .createInputDefinitionsProviderFromTestFile();

        ComponentInterfaceImpl compInterf = new ComponentInterfaceImpl();
        compInterf.setIdentifier("interface-id");
        List<String> identifiers = new ArrayList<>();
        identifiers.add("interface-id");
        identifiers.add("interface-id.old");
        compInterf.setIdentifiers(identifiers);
        compInterf.setDisplayName("display name");
        Map<String, String> readOnlyConfig = new HashedMap<>();
        readOnlyConfig.put(readOnlyConfigKey, readOnlyConfigValue);
        configDefImpl.setRawReadOnlyConfiguration(readOnlyConfig);
        compInterf.setConfigurationDefinition(configDefImpl);
        compInterf.setConfigurationExtensionDefinitions(configExtDefsImpl);
        compInterf.setInputDefinitionsProvider(inputsProviderImpl);
        ComponentRevisionImpl compRev = new ComponentRevisionImpl();
        compRev.setComponentInterface(compInterf);
        ComponentInstallationImpl compInst = new ComponentInstallationImpl();
        compInst.setComponentRevision(compRev);
        compInst.setInstallationId("install-id");
        compInst.setMaximumCountOfParallelInstances(maxInstances);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

        ComponentInstallationImpl otherCompInst = mapper.readValue(mapper.writeValueAsString(compInst), ComponentInstallationImpl.class);

        assertEquals(0, otherCompInst.compareTo(compInst));

        ComponentInterface otherCompInterf = otherCompInst.getComponentRevision().getComponentInterface();

        assertEquals(compInterf.getIdentifiers().size(), otherCompInterf.getIdentifiers().size());
        assertEquals(compInst.getMaximumCountOfParallelInstances(), otherCompInst.getMaximumCountOfParallelInstances());

        // check configuration
        ConfigurationDefinition configDef = compInterf.getConfigurationDefinition();
        ConfigurationDefinition otherConfigDef = otherCompInterf.getConfigurationDefinition();

        assertEquals(configDef.getConfigurationKeys().size(), otherConfigDef.getConfigurationKeys().size());
        assertEquals(configDef.getDefaultValue(ConfigurationDescriptionStubFactory.PORT),
            otherConfigDef.getDefaultValue(ConfigurationDescriptionStubFactory.PORT));
        assertEquals(configDef.getReadOnlyConfiguration().getConfigurationKeys().size(),
            otherConfigDef.getReadOnlyConfiguration().getConfigurationKeys().size());
        assertEquals(configDef.getReadOnlyConfiguration().getValue(readOnlyConfigKey),
            otherConfigDef.getReadOnlyConfiguration().getValue(readOnlyConfigKey));

        assertEquals(compInterf.getConfigurationExtensionDefinitions().size(),
            otherCompInterf.getConfigurationExtensionDefinitions().size());

        // check activation filter
        for (ConfigurationExtensionDefinition configExtDef : compInterf.getConfigurationExtensionDefinitions()) {
            if (configExtDef.getConfigurationKeys().contains(ConfigurationDescriptionStubFactory.WORK_DIR)) {
                boolean matchFound = false;
                for (ConfigurationExtensionDefinition otherConfigExtDef : otherCompInterf.getConfigurationExtensionDefinitions()) {
                    if (otherConfigExtDef.getConfigurationKeys().contains(ConfigurationDescriptionStubFactory.WORK_DIR)) {
                        assertEquals(((ConfigurationExtensionDefinitionImpl) configExtDef).getRawActivationFilter(),
                            ((ConfigurationExtensionDefinitionImpl) otherConfigExtDef).getRawActivationFilter());
                        matchFound = true;
                    }
                }
                assertTrue(matchFound);
            } else if (configExtDef.getConfigurationKeys().contains(ConfigurationDescriptionStubFactory.FORMAT)) {
                boolean matchFound = false;
                for (ConfigurationExtensionDefinition otherConfigExtDef : otherCompInterf.getConfigurationExtensionDefinitions()) {
                    if (otherConfigExtDef.getConfigurationKeys().contains(ConfigurationDescriptionStubFactory.FORMAT)) {
                        assertEquals(((ConfigurationExtensionDefinitionImpl) configExtDef)
                            .getActivationFilter(ConfigurationDescriptionStubFactory.FORMAT),
                            ((ConfigurationExtensionDefinitionImpl) otherConfigExtDef)
                                .getActivationFilter(ConfigurationDescriptionStubFactory.FORMAT));
                        matchFound = true;
                    }
                }
                assertTrue(matchFound);
            } else {
                fail("Missing configuration key");
            }
        }

        // check endpoints
        EndpointDefinition dynInputDef = compInterf.getInputDefinitionsProvider()
            .getDynamicEndpointDefinition(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1);
        EndpointDefinition otherDynInputDef = otherCompInterf.getInputDefinitionsProvider()
            .getDynamicEndpointDefinition(EndpointDefinitionProviderStubFactory.DYNAMICINPUTID1);
        assertNotNull(otherDynInputDef);
        assertEquals(dynInputDef.getMetaDataDefinition().getMetaDataKeys().size(),
            otherDynInputDef.getMetaDataDefinition().getMetaDataKeys().size());
        
        assertEquals(compInterf.getInputDefinitionsProvider().getStaticEndpointGroupDefinitions().size(),
            otherCompInterf.getInputDefinitionsProvider().getStaticEndpointGroupDefinitions().size());
        assertEquals(compInterf.getInputDefinitionsProvider().getDynamicEndpointGroupDefinitions().size(),
            otherCompInterf.getInputDefinitionsProvider().getDynamicEndpointGroupDefinitions().size());
    }
}
