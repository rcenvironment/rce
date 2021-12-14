/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.model.api.ComponentColor;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;


class ComponentInterfaceMock implements ComponentInterface {

    private DistributedComponentEntryMock distributedComponentEntryMock;

    private String identifier;

    ComponentInterfaceMock(DistributedComponentEntryMock distributedComponentEntryMock) {
        super();
        this.distributedComponentEntryMock = distributedComponentEntryMock;
        this.identifier = distributedComponentEntryMock.getInstallationID();

    }

    @Override
    public String getDisplayName() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public String getGroupName() {
        return distributedComponentEntryMock.getGroupName();
    }

    @Override
    public String getIconHash() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public byte[] getIcon16() {
        return null;
    }

    @Override
    public byte[] getIcon24() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public byte[] getIcon32() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentColor getColor() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentSize getSize() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentShape getShape() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public String getIdentifier() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public String getIdentifierAndVersion() {
        return this.identifier;
    }

    @Override
    public List<String> getIdentifiers() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public String getVersion() {
        return PaletteViewTestConstants.TOOL_TEST_VERSION;
    }

    @Override
    public EndpointDefinitionsProvider getInputDefinitionsProvider() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public EndpointDefinitionsProvider getOutputDefinitionsProvider() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ConfigurationDefinition getConfigurationDefinition() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public Set<ConfigurationExtensionDefinition> getConfigurationExtensionDefinitions() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public boolean getLocalExecutionOnly() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public boolean getPerformLazyDisposal() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public boolean getIsDeprecated() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public boolean getCanHandleNotAValueDataTypes() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public boolean getLoopDriverSupportsDiscard() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public boolean getIsLoopDriver() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public String getDocumentationHash() {
        // no implementation needed for unit testing
        return null;
    }

}
