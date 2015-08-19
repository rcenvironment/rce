/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;

/**
 * Class that maps configuration of a component onto the IPropertySource interface.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 */
public class ComponentPropertySource implements IPropertySource2, ComponentInstanceProperties {

    /** The WorkflowNode to operate on, holding the ComponentDescription. */
    private final WorkflowNode node;

    /** List of all properties of the component. */
    private final ConfigurationDescription configurationDesc;

    private final CommandStack cs;

    public ComponentPropertySource(CommandStack stack, WorkflowNode node) {
        this.node = node;
        configurationDesc = node.getComponentDescription().getConfigurationDescription();
        cs = stack;
    }

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {

        List<IPropertyDescriptor> descriptors = new ArrayList<IPropertyDescriptor>();

        for (String key : configurationDesc.getConfiguration().keySet()) {
            descriptors.add(new TextPropertyDescriptor(key, key));
        }

        return descriptors.toArray(new IPropertyDescriptor[] {});
    }
    
    @Override
    public boolean isPropertyResettable(final Object key) {
        return false;
    }

    @Override
    public Object getPropertyValue(Object key) {
        Object value = WorkflowNodeUtil.getConfigurationValue(node, (String) key);
        if (value == null) {
            return ""; //$NON-NLS-1$
        }
        return value.toString();
    }

    @Override
    public boolean isPropertySet(Object key) {
        return WorkflowNodeUtil.isConfigurationValueSet(node, (String) key);
    }

    @Override
    public void resetPropertyValue(Object key) {
        setConfiguration((String) key, configurationDesc.getComponentConfigurationDefinition().getDefaultValue((String) key));
    }

    @Override
    public void setPropertyValue(Object key, Object value) {
        if (value == null || ((String) value).isEmpty()) {
            setConfiguration((String) key, (String) null);
            return;
        }

        setConfiguration((String) key, (String) value);
    }

    private String getConfiguration(String key) {
        return WorkflowNodeUtil.getConfigurationValue(node, key);
    }

    private void setConfiguration(String key, String value) {
        if ((value == null && getConfiguration(key) != null)
            || (value != null && !value.equals(getConfiguration(key)))) {
            SetValueCommand setCommand = new SetValueCommand(Messages.property, node, key, value);
            cs.execute(setCommand);
        }
    }

    /**
     * Command to change a property value.
     * 
     * @author Heinrich Wendel
     */
    class SetValueCommand extends Command {

        private final WorkflowNode target;

        private final String propertyName;

        private final String propertyValue;

        private String undoValue;
        
        private ConfigurationDescription config;

        public SetValueCommand(String label, WorkflowNode node, String id, String value) {
            super(label);
            target = node;
            propertyName = id;
            propertyValue = value;
            config = target.getConfigurationDescription();
        }

        @Override
        public void execute() {
            undoValue = config.getConfigurationValue(propertyName);
            config.setConfigurationValue(propertyName, propertyValue);
        }

        @Override
        public void undo() {
            config.setConfigurationValue(propertyName, undoValue);
        }

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        node.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        node.removePropertyChangeListener(listener);
    }
    
    @Override
    public EndpointDescriptionsManager getInputDescriptionsManager() {
        return node.getInputDescriptionsManager();
    }

    @Override
    public EndpointDescriptionsManager getOutputDescriptionsManager() {
        return node.getOutputDescriptionsManager();
    }

    @Override
    public ConfigurationDescription getConfigurationDescription() {
        return node.getConfigurationDescription();
    }


}
