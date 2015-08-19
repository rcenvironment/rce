/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class holding information about an installed {@link Component}.
 * 
 * @author Roland Gude
 * @author Jens Ruehmkorf
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Christian Weiss
 * @author Sascha Zur
 * @author David Scholz
 */
public class ComponentDescription implements Serializable, Cloneable, Comparable<ComponentDescription> {

    /** Prefix for {@link java.beans.PropertyChangeEvent}s concerning properties. */
    public static final String PROPERTIES_PREFIX = "properties.";

    /** Key of default configuration map. */
    public static final String DEFAULT_CONFIG_ID = "de.rcenvironment.rce.component.configuration.default";

    private static final long serialVersionUID = -7551319972711119245L;

    private ComponentInstallation componentInstallation;

    private ComponentRevision componentRevision;

    private ComponentInterface componentInterface;

    private final EndpointDescriptionsManager inputDescriptionsManager;

    private final EndpointDescriptionsManager outputDescriptionsManager;

    private ConfigurationDescription configurationDescription;

    private boolean isNodeTransient = false;

    public ComponentDescription(ComponentInstallation componentInstallation) {

        this.componentInstallation = componentInstallation;
        this.componentRevision = componentInstallation.getComponentRevision();
        this.componentInterface = componentRevision.getComponentInterface();

        inputDescriptionsManager = new EndpointDescriptionsManager(componentInterface.getInputDefinitionsProvider(),
            EndpointType.INPUT);

        outputDescriptionsManager = new EndpointDescriptionsManager(componentInterface.getOutputDefinitionsProvider(),
            EndpointType.OUTPUT);

        configurationDescription = new ConfigurationDescription(componentInterface.getConfigurationDefinition(),
            componentInterface.getConfigurationExtensionDefinitions());
    }

    /**
     * Initializes the component with default initial values such as endpoints. May only be called for newly created components.
     */
    public void initializeWithDefaults() {
        inputDescriptionsManager.addInitialDynamicEndpointDescriptions();
        outputDescriptionsManager.addInitialDynamicEndpointDescriptions();
    }

    public String getIdentifier() {
        return componentInterface.getIdentifier();
    }

    /**
     * @return name of implementing class
     */
    public String getClassName() {
        return componentRevision.getClassName();
    }

    public String getName() {
        return componentInterface.getDisplayName();
    }

    public String getGroup() {
        return componentInterface.getGroupName();
    }

    public String getVersion() {
        return componentInterface.getVersion();
    }
    
    public ComponentSize getSize() {
        return componentInterface.getSize();
    }

    public boolean getIsNodeTransient() {
        return isNodeTransient;
    }

    public void setIsNodeTransient(boolean isNodeTransient) {
        this.isNodeTransient = isNodeTransient;
    }

    public EndpointDescriptionsManager getInputDescriptionsManager() {
        return inputDescriptionsManager;
    }

    public EndpointDescriptionsManager getOutputDescriptionsManager() {
        return outputDescriptionsManager;
    }

    public EndpointDefinitionsProvider getDeclarativeInputDescriptionsProvider() {
        return componentInterface.getInputDefinitionsProvider();
    }

    public EndpointDefinitionsProvider getDeclarativeOutputDescriptionsProvider() {
        return componentInterface.getOutputDefinitionsProvider();
    }

    public ConfigurationDescription getConfigurationDescription() {
        return configurationDescription;
    }

    /**
     * @return <code>true</code> if the component can technically only be executed locally, otherwise <code>false</code>
     */
    public boolean canOnlyBeExecutedLocally() {
        return componentInterface.getLocalExecutionOnly();
    }

    /**
     * @return <code>true</code> if the component should be disposed workflow disposal, <code>false</code> if immediately when the component
     *         had finished (default behavior)
     */
    public boolean performLazyDisposal() {
        return componentInterface.getPerformLazyDisposal();
    }

    public byte[] getIcon16() {
        return componentInterface.getIcon16();
    }

    public byte[] getIcon24() {
        return componentInterface.getIcon24();
    }

    public byte[] getIcon32() {
        return componentInterface.getIcon32();
    }

    /**
     * @return node to run on
     */
    public NodeIdentifier getNode() {
        if (componentInstallation.getNodeId() != null) {
            return NodeIdentifierFactory.fromNodeId(componentInstallation.getNodeId());
        }
        return null;
    }

    /**
     * @param installation new {@link ComponentInstallation} to set
     */
    public void setComponentInstallationAndUpdateConfiguration(ComponentInstallation installation) {
        setComponentInstallation(installation);        
        Map<String, String> config = configurationDescription.getConfiguration();
        Map<String, String> placeholders = configurationDescription.getPlaceholders();

        configurationDescription = new ConfigurationDescription(installation.getComponentRevision()
            .getComponentInterface().getConfigurationDefinition(),
            installation.getComponentRevision().getComponentInterface().getConfigurationExtensionDefinitions());
        configurationDescription.setConfiguration(config);
        configurationDescription.setPlaceholders(placeholders);
    }

    public ComponentInstallation getComponentInstallation() {
        return componentInstallation;
    }
    
    /**
     * Copies the execution information of the given component description.
     * 
     * @param installation component installation with execution information to copy
     */
    public void setComponentInstallation(ComponentInstallation installation) {
        String interfaceId = componentInterface.getIdentifier();
        String newInterfaceId = installation.getComponentRevision().getComponentInterface().getIdentifier();
        if (!newInterfaceId.equals(interfaceId)
            && !(newInterfaceId.startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) && newInterfaceId.endsWith(interfaceId)
            && !(interfaceId.startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) && interfaceId.endsWith(newInterfaceId)) {
            throw new IllegalArgumentException("Component installation doesn't refer to the interface: "
                + componentInterface.getIdentifier());
        }
        
        this.componentInstallation = installation;
        this.componentRevision = componentInstallation.getComponentRevision();
        this.componentInterface = componentRevision.getComponentInterface();
    }
    
    @Override
    public String toString() {
        return getNode() + StringUtils.SEPARATOR + componentInterface.getIdentifier();
    }

    @Override
    public ComponentDescription clone() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            ComponentDescription cd = (ComponentDescription) ois.readObject();
            ois.close();
            bin.close();
            oos.close();
            bos.close();
            return cd;
        } catch (IOException e) {
            LogFactory.getLog(ComponentDescription.class).error("cloning component description failed", e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            LogFactory.getLog(ComponentDescription.class).error("cloning component description failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(ComponentDescription o) {
        return this.getName().compareTo(o.getName());
    }

}
