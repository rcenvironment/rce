/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Abstract base implementation of a {@link WorkflowNodeValidator}.
 * 
 * @author Christian Weiss
 */
public abstract class AbstractWorkflowNodeValidator implements WorkflowNodeValidator {

    private WorkflowNode workflowNode;

    private WorkflowNodeChangeListener changeListener;

    private final List<WorkflowNodeValidityChangeListener> changeListeners = new LinkedList<WorkflowNodeValidityChangeListener>();

    private final List<WorkflowNodeValidityStateListener> stateListeners = new LinkedList<WorkflowNodeValidityStateListener>();

    private final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();

    private Boolean valid;

    @Override
    public final void setWorkflowNode(final WorkflowNode workflowNode) {
        if (this.workflowNode != null) {
            throw new IllegalStateException("WorkflowNode already set.");
        }
        this.workflowNode = workflowNode;
        initializeModelBinding();
        revalidate();
    }

    private void initializeModelBinding() {
        final WorkflowNodeChangeListener listener = createWorkflowNodeChangeListener();
        workflowNode.addPropertyChangeListener(listener);
        afterInitializingModelBinding();
    }

    protected void afterInitializingModelBinding() {
        // do nothing
    }

    protected WorkflowNode getWorkflowNode() {
        return workflowNode;
    }

    protected WorkflowNodeChangeListener getWorkflowNodeChangeListener() {
        return changeListener;
    }

    protected WorkflowNodeChangeListener createWorkflowNodeChangeListener() {
        return new DefaultWorkflowNodeChangeListener();
    }

    private void revalidate() {
        if (workflowNode == null) {
            throw new IllegalStateException("WorkflowNode not set.");
        }
        messages.clear();
        final Collection<WorkflowNodeValidationMessage> validateMessages = validate();
        if (validateMessages != null) {
            messages.addAll(validateMessages);
        }
    }

    @Override
    public final Collection<WorkflowNodeValidationMessage> getMessages() {
        if (workflowNode == null) {
            throw new IllegalStateException("WorkflowNode not set.");
        }
        return messages;
    }

    /**
     * Returns all {@link WorkflowNodeValidationMessage} that hint for failures or errors in the
     * configuration. If no failures/errors were found an empty collection or 'null' should be
     * returned.
     * 
     * @return 'null' or an empty collection, if no failures/errors were found, otherwise all
     *         {@link WorkflowNodeValidationMessage} that explain the failures/errors.
     */
    protected abstract Collection<WorkflowNodeValidationMessage> validate();

    @Override
    public boolean isValid() {
        if (valid == null) {
            refreshValid();
        }
        return valid;
    }

    protected void setValid(final boolean valid) {
        if (this.valid == null || this.valid != valid) {
            this.valid = valid;
            final WorkflowNodeValidityChangeEvent event = new WorkflowNodeValidityChangeEvent(getWorkflowNode(), valid);
            fireWorkflowNodeValidityChangeEvent(event);
        }
        final WorkflowNodeValidityStateEvent event = new WorkflowNodeValidityStateEvent(getWorkflowNode(), valid);
        fireWorkflowNodeValidityStateEvent(event);
    }

    protected void refreshValid() {
        final List<WorkflowNodeValidationMessage> oldMessages = new ArrayList<WorkflowNodeValidationMessage>(messages);
        revalidate();
        if (valid == null || !messages.equals(oldMessages)) {
            final boolean newValid = messages == null || messages.isEmpty();
            setValid(newValid);
        }
    }

    @Override
    public void addWorkflowNodeValidityChangeListener(final WorkflowNodeValidityChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeWorkflowNodeValidityChangeListener(final WorkflowNodeValidityChangeListener listener) {
        changeListeners.remove(listener);
    }

    protected void fireWorkflowNodeValidityChangeEvent(final WorkflowNodeValidityChangeEvent event) {
        for (final WorkflowNodeValidityChangeListener listener : changeListeners) {
            listener.handleWorkflowNodeValidityChangeEvent(event);
        }
    }

    @Override
    public void addWorkflowNodeValidityStateListener(WorkflowNodeValidityStateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeWorkflowNodeValidityStateListener(WorkflowNodeValidityStateListener listener) {
        stateListeners.remove(listener);
    }

    protected void fireWorkflowNodeValidityStateEvent(final WorkflowNodeValidityStateEvent event) {
        for (final WorkflowNodeValidityStateListener listener : stateListeners) {
            listener.handleWorkflowNodeValidityStateEvent(event);
        }
    }

    protected boolean hasInputs() {
        return WorkflowNodeUtil.hasInputs(workflowNode);
    }

    protected boolean hasOutputs() {
        return WorkflowNodeUtil.hasOutputs(workflowNode);
    }

    protected boolean hasInputs(DataType type) {
        return WorkflowNodeUtil.hasInputs(workflowNode, type);
    }

    protected boolean hasOutputs(DataType type) {
        return WorkflowNodeUtil.hasOutputs(workflowNode, type);
    }

    protected Set<EndpointDescription> getInputs() {
        return WorkflowNodeUtil.getInputs(workflowNode);
    }

    protected Set<EndpointDescription> getInputs(DataType type) {
        return WorkflowNodeUtil.getInputsByDataType(workflowNode, type);
    }

    protected Set<EndpointDescription> getOutputs() {
        return WorkflowNodeUtil.getOutputs(workflowNode);
    }

    protected Set<EndpointDescription> getOutputs(DataType type) {
        return WorkflowNodeUtil.getOutputs(workflowNode, type);
    }

    protected boolean hasProperty(final String key) {
        return WorkflowNodeUtil.hasConfigurationValue(workflowNode, key);
    }

    protected boolean isPropertySet(final String key) {
        return WorkflowNodeUtil.isConfigurationValueSet(workflowNode, key);
    }

    protected String getProperty(final String key) {
        return WorkflowNodeUtil.getConfigurationValue(workflowNode, key);
    }

    protected String getProperty(final String key, final String clazz) {
        return WorkflowNodeUtil.getConfigurationValue(workflowNode, key);
    }

    /**
     * Interface to be used to synchronize with changes in the {@link WorkflowNode}.
     * 
     * @author Christian Weiss
     */
    protected interface WorkflowNodeChangeListener extends PropertyChangeListener {
        
    }

    /**
     * Default implementation of {@link WorkflowNodeChangeListener}.
     * 
     * @author Christian Weiss
     */
    protected class DefaultWorkflowNodeChangeListener implements WorkflowNodeChangeListener, Serializable {

        private static final long serialVersionUID = 8472168965932179663L;

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final Matcher propertiesPatternMatcher = WorkflowNode.PROPERTIES_PATTERN.matcher(event.getPropertyName());
            if (propertiesPatternMatcher.matches() || EndpointDescriptionsManager.PROPERTY_ENDPOINT.equals(event.getPropertyName())) {
                refresh();
            }
        }

        protected void refresh() {
            AbstractWorkflowNodeValidator.this.refreshValid();
        }

    }

}
