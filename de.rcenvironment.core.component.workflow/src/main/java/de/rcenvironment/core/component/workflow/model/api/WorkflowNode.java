/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.UUID;
import java.util.regex.Pattern;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;

/**
 * A node within a {@link WorkflowDescription}.
 * 
 * @author Roland Gude
 * @author Heinrich Wendel
 * @author Robert Mischke
 * @author Christian Weiss
 * @author Hendrik Abbenhaus
 */
public class WorkflowNode extends PropertiesChangeSupport
    implements Serializable, ComponentInstanceProperties, Comparable<WorkflowNode> {

    /**
     * {@link Pattern} to match property names of {@link java.beans.PropertyChangeEvent}s concerning properties.
     */
    public static final Pattern PROPERTIES_PATTERN = Pattern.compile("^properties\\.(.*)$");

    /** Property that is fired when the location changes. */
    public static final String PROPERTY_COMMUNICATION_NODE = "de.rcenvironment.rce.component.workflow.CommunicationNode";

    /** Property that is fired when the name changes. */
    public static final String PROPERTY_NODE_ATTRIBUTES = "de.rcenvironment.props.n";

    private static final int DEFAULT_X_Y = 0;

    private static final long serialVersionUID = -7495156467094187194L;

    private static final int INITIAL_ZINDEX = -1;

    private ComponentDescription compDesc;

    private WorkflowNodeIdentifier identifier;

    private String name;

    /** X position of the location in a graphical editor. */
    private int x;

    /** Y position of the location in a graphical editor. */
    private int y;

    /** Z index of component. */
    private int zIndex;

    private boolean isEnabled = true;

    private boolean valid = false;

    private boolean isChecked = false;

    private boolean init = false;
   
    /**
     * Constructor.
     * 
     * @param componentDescription The {@link ComponentDescription} of the {@link de.rcenvironment.core.component.model.spi.Component}
     *        represented by this {@link WorkflowNode}.
     * @param componentConfiguration The configuration of this {@link de.rcenvironment.core.component.model.spi.Component} represented by
     *        this {@link WorkflowNode}.
     */
    public WorkflowNode(ComponentDescription componentDescription) {
        compDesc = componentDescription;
        identifier = new WorkflowNodeIdentifier(UUID.randomUUID().toString());
        x = DEFAULT_X_Y;
        y = DEFAULT_X_Y;
        zIndex = INITIAL_ZINDEX;
    }

    @Override
    public String toString() {
        return compDesc.getIdentifier();
    }

    public ComponentDescription getComponentDescription() {
        return compDesc;
    }

    public void setComponentDescription(ComponentDescription cd) {
        this.compDesc = cd;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
    
    public boolean isImitiationModeActive() {
        return Boolean
            .valueOf(getConfigurationDescription().getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE));
    }
    
    public boolean isImitationModeSupported() {
        return Boolean.valueOf(getConfigurationDescription().getComponentConfigurationDefinition()
            .getReadOnlyConfiguration().getValue(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED));
    }

    /**
     * @param newX The new X location.
     * @param newY The new Y location.
     */
    public void setLocation(int newX, int newY) {
        x = newX;
        y = newY;
        firePropertyChange(PROPERTY_COMMUNICATION_NODE);
    }

    /**
     * @param newEnabled <code>true</code> of {@link WorkflowNode} is activated and considered during execution, otherwise
     *        <code>false</code>
     */
    public void setEnabled(boolean newEnabled) {
        this.isEnabled = newEnabled;
        firePropertyChange(PROPERTY_NODE_ATTRIBUTES);
    }
    
    /**
     * 
     * @param isImitiationModeActive set <code>true</code> to activate or <code>false</code> to deactivate imitation mode
     */
    public void setImitiationModeActive(boolean isImitiationModeActive) {
        getConfigurationDescription().setConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE, 
            Boolean.toString(isImitiationModeActive));
       
        firePropertyChange(PROPERTY_NODE_ATTRIBUTES);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
        firePropertyChange(PROPERTY_NODE_ATTRIBUTES);
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public String getIdentifier() {
        return identifier.toString();
    }
    
    public WorkflowNodeIdentifier getIdentifierAsObject() {
        return identifier;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof WorkflowNode) {
            return identifier.equals(((WorkflowNode) other).identifier);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    // needed for the persistence handler to set the identifier given by file
    public void setIdentifier(String newIdentifier) {
        identifier = new WorkflowNodeIdentifier(newIdentifier);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        super.addPropertyChangeListener(l);
        getConfigurationDescription().addPropertyChangeListener(l);
        getInputDescriptionsManager().addPropertyChangeListener(l);
        getOutputDescriptionsManager().addPropertyChangeListener(l);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        super.removePropertyChangeListener(l);
        getConfigurationDescription().removePropertyChangeListener(l);
        getInputDescriptionsManager().removePropertyChangeListener(l);
        getOutputDescriptionsManager().removePropertyChangeListener(l);
    }

    @Override
    public int compareTo(WorkflowNode o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public EndpointDescriptionsManager getInputDescriptionsManager() {
        return compDesc.getInputDescriptionsManager();
    }

    @Override
    public EndpointDescriptionsManager getOutputDescriptionsManager() {
        return compDesc.getOutputDescriptionsManager();
    }

    @Override
    public ConfigurationDescription getConfigurationDescription() {
        return compDesc.getConfigurationDescription();
    }

    @Override
    public String getComponentIdentifierWithVersion() {
        return getComponentDescription().getIdentifier();
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    /**
     * 
     * @return flag.
     */
    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndexD) {
        this.zIndex = zIndexD;
    }

}
