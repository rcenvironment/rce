/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Creates {@link ComponentInterface} objects.
 * 
 * @author Doreen Seider
 */
public class ComponentInterfaceBuilder {

    private final ComponentInterfaceImpl componentInterface;

    public ComponentInterfaceBuilder() {
        componentInterface = new ComponentInterfaceImpl();
    }

    /**
     * @param identifier identifier if the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setIdentifier(String identifier) {
        componentInterface.setIdentifier(identifier);
        List<String> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        componentInterface.setIdentifiers(identifiers);
        return this;
    }

    /**
     * @param identifiers identifier if the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setIdentifiers(List<String> identifiers) {
        componentInterface.setIdentifiers(identifiers);
        return this;
    }

    /**
     * @param displayName name of the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setDisplayName(String displayName) {
        componentInterface.setDisplayName(displayName);
        return this;
    }

    /**
     * @param groupName group of the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setGroupName(String groupName) {
        componentInterface.setGroupName(groupName);
        return this;
    }

    /**
     * @param icon16 small icon
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setIcon16(byte[] icon16) {
        componentInterface.setIcon16(icon16);
        return this;
    }

    /**
     * @param icon24 mid size icon
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setIcon24(byte[] icon24) {
        componentInterface.setIcon24(icon24);
        return this;
    }

    /**
     * @param icon32 large icon
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setIcon32(byte[] icon32) {
        componentInterface.setIcon32(icon32);
        return this;
    }

    /**
     * 
     * @param docuHash has of the doumentation folder for the tool.
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setDocumentationHash(String docuHash) {
        componentInterface.setDocumentationHash(docuHash);
        return this;
    }

    /**
     * @param version version of the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setVersion(String version) {
        componentInterface.setVersion(version);
        return this;
    }

    /**
     * @param color color of the component's background
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setColor(ComponentColor color) {
        componentInterface.setColor(color);
        return this;
    }

    /**
     * @param shape shape of the component.
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setShape(ComponentShape shape) {
        componentInterface.setShape(shape);
        return this;
    }

    /**
     * @param size size of the component
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setSize(ComponentSize size) {
        componentInterface.setSize(size);
        return this;
    }

    /**
     * @param inputDefinitionsProvider input definitions
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setInputDefinitionsProvider(EndpointDefinitionsProvider inputDefinitionsProvider) {
        componentInterface.setInputDefinitionsProvider((EndpointDefinitionsProviderImpl) inputDefinitionsProvider);
        return this;
    }

    /**
     * @param outputDefinitionsProvider output definitions
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setOutputDefinitionsProvider(EndpointDefinitionsProvider outputDefinitionsProvider) {
        componentInterface.setOutputDefinitionsProvider((EndpointDefinitionsProviderImpl) outputDefinitionsProvider);
        return this;
    }

    /**
     * @param configurationDefinition configuration definitions
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        componentInterface.setConfigurationDefinition((ConfigurationDefinitionImpl) configurationDefinition);
        return this;
    }

    /**
     * @param configurationExtensionDefinitions extended configuration definitions
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setConfigurationExtensionDefinitions(
        Set<ConfigurationExtensionDefinition> configurationExtensionDefinitions) {
        Set<ConfigurationExtensionDefinitionImpl> configurationDefinitionsImpls = new HashSet<ConfigurationExtensionDefinitionImpl>();
        for (ConfigurationExtensionDefinition definition : configurationExtensionDefinitions) {
            configurationDefinitionsImpls.add((ConfigurationExtensionDefinitionImpl) definition);
        }
        componentInterface.setConfigurationExtensionDefinitions(configurationDefinitionsImpls);
        return this;
    }

    /**
     * @param localExecutionOnly whether it is remote executable
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setLocalExecutionOnly(boolean localExecutionOnly) {
        componentInterface.setLocalExecutionOnly(localExecutionOnly);
        return this;
    }

    /**
     * @param performLazyDisposal whether disposal must be performed lazily
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setPerformLazyDisposal(boolean performLazyDisposal) {
        componentInterface.setPerformLazyDisposal(performLazyDisposal);
        return this;
    }

    /**
     * @param configurationDefinition configuration definitions
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setCanHandleUndefinedInputDatums(ConfigurationDefinition configurationDefinition) {
        componentInterface.setConfigurationDefinition((ConfigurationDefinitionImpl) configurationDefinition);
        return this;
    }

    /**
     * @param canHandleNotAValueDataTypes whether component can handle incoming
     *        {@link TypedDatum}s of {@link DataType} {@link NotAValueTD}
     * @return builder object for method chaining purposes
     */
    public ComponentInterfaceBuilder setCanHandleNotAValueDataTypes(boolean canHandleNotAValueDataTypes) {
        componentInterface.setCanHandleNotAValueDataTypes(canHandleNotAValueDataTypes);
        return this;
    }

    /**
     * @return {@link ComponentInterface} object built
     */
    public ComponentInterface build() {
        return componentInterface;
    }

}
