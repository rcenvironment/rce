/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Create {@link ComponentInstallation} objects.
 * 
 * @author Doreen Seider
 */
public final class ComponentInstallationStubFactory {

    private ComponentInstallationStubFactory() {}

    /**
     * @param interfaceId identifier of related {@link ComponentInterface}
     * @return {@link ComponentInstallation} with default behavior
     */
    public static ComponentInstallationImpl createComponentInstallationStub(String interfaceId) {

        String name = "name";
        String group = "group";
        String version = "2.0";

        ComponentInterfaceImpl componentInterface = new ComponentInterfaceImpl();
        componentInterface.setIdentifier(interfaceId);
        List<String> interfaceIds = new ArrayList<>();
        interfaceIds.add(interfaceId);
        componentInterface.setIdentifiers(interfaceIds);
        componentInterface.setDisplayName(name);
        componentInterface.setGroupName(group);
        componentInterface.setVersion(version);
        componentInterface.setIcon16(new byte[7]);
        componentInterface.setIcon24(new byte[8]);
        componentInterface.setIcon32(new byte[9]);
        componentInterface.setLocalExecutionOnly(false);
        componentInterface.setPerformLazyDisposal(false);
        componentInterface.setInputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setOutputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setConfigurationDefinition(new ConfigurationDefinitionImpl());
        componentInterface.setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinitionImpl>());

        ComponentRevisionImpl componentRevision = new ComponentRevisionImpl();
        componentRevision.setComponentInterface(componentInterface);
        componentRevision.setClassName("de.rcenvironment.core.component.model.spi.ComponentStub");

        ComponentInstallationImpl componentInstallation = new ComponentInstallationImpl();
        componentInstallation.setComponentRevision(componentRevision);
        componentInstallation.setNodeId("node-id");
        return componentInstallation;
    }

    /**
     * @return {@link ComponentInstallation} with default behavior
     */
    public static ComponentInstallation createComponentInstallationStub() {

        String identifier = "de.rcenvironment.core.component.model.spi.ComponentStub_Component Stub";
        return createComponentInstallationStub(identifier);
    }
}
