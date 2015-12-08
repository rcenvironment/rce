/*
 * Copyright (C) 2006-2015 DLR, Germany
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

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract base implementation of a {@link WorkflowNodeValidator}.
 * 
 * @author Christian Weiss
 */
public abstract class AbstractWorkflowNodeValidator implements WorkflowNodeValidator {

    private static final String WORKFLOW_NODE_ALREADY_SET = "WorkflowNode already set.";

    private WorkflowNode workflowNode;

    private WorkflowNodeChangeListener changeListener;

    private final List<WorkflowNodeValidityChangeListener> changeListeners = new LinkedList<WorkflowNodeValidityChangeListener>();

    private final List<WorkflowNodeValidityStateListener> stateListeners = new LinkedList<WorkflowNodeValidityStateListener>();

    private final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();

    private Boolean valid;

    @Override
    public final void setWorkflowNode(final WorkflowNode node, boolean onWorkflowStart) {
        if (this.workflowNode != null) {
            throw new IllegalStateException(WORKFLOW_NODE_ALREADY_SET);
        }
        this.workflowNode = node;
        initializeModelBinding();
        revalidate(onWorkflowStart);
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

    private void revalidate(boolean onWorkflowStart) {
        if (workflowNode == null) {
            throw new IllegalStateException(WORKFLOW_NODE_ALREADY_SET);
        }
        WorkflowNodeValidatonMessageStore validatonStore = WorkflowNodeValidatonMessageStore.getInstance();
        messages.clear();
        final Collection<WorkflowNodeValidationMessage> validateMessages;
        if (onWorkflowStart) {
            List<WorkflowNodeValidationMessage> retrievedValidationMessages =
                validatonStore.retrieveValidatonMessages(workflowNode.getIdentifier());
            if (retrievedValidationMessages != null) {
                messages.addAll(retrievedValidationMessages);
            }
            validateMessages = validateOnStart();
        } else {
            validateMessages = validate();
        }
        Collection<WorkflowNodeValidationMessage> validateInputMessages = validateInputsExecutionConstraints();
        if (validateMessages != null || !validateInputMessages.isEmpty()) {
            if (validateMessages != null) {
                addValidationMessages(validateMessages);
            }
            if (!validateInputMessages.isEmpty()) {
                addValidationMessages(validateInputMessages);
            }            
            if (!onWorkflowStart) {
                validatonStore.addValidatonMessages(workflowNode.getIdentifier(), messages);
            }
        }
    }
    
    private Collection<WorkflowNodeValidationMessage> validateInputsExecutionConstraints() {
        
        List<WorkflowNodeValidationMessage> m = new LinkedList<WorkflowNodeValidationMessage>();
        
        for (EndpointDescription inputEp : getInputs()) {
            EndpointDefinition.InputExecutionContraint exeConstraint = inputEp.getEndpointDefinition()
                .getDefaultInputExecutionConstraint(); 
            if (inputEp.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                exeConstraint = EndpointDefinition.InputExecutionContraint.valueOf(
                    inputEp.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
            }
            if (exeConstraint.equals(EndpointDefinition.InputExecutionContraint.Required) && !inputEp.isConnected()) {
                m.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                    "", StringUtils.format("Connect input '%s' to an output as it is required", inputEp.getName()),
                    StringUtils.format("Input '%s' is required but not connected to an output", inputEp.getName())));
            }
        }
        
        return m;
    }

    private void addValidationMessages(Collection<WorkflowNodeValidationMessage> validateMessages) {
        List<WorkflowNodeValidationMessage> validationMessages = new LinkedList<WorkflowNodeValidationMessage>();
        for (WorkflowNodeValidationMessage message : validateMessages) {
            if (!messages.contains(message)) {
                validationMessages.add(message);
            }
        }
        messages.addAll(validationMessages);
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

    /**
     * 
     * Returns all {@link WorkflowNodeValidationMessage} that hint for failures or errors in the configuration. It is only executed on
     * workflow start.
     * 
     * @return 'null' or an empty collection, if no failures/errors were found, otherwise all {@link WorkflowNodeValidationMessage} that
     *         explain the failures/errors.
     */
    protected Collection<WorkflowNodeValidationMessage> validateOnStart() {
        // do nothing, can be implemented to validate the component on workflow start
        return null;
    }

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
        revalidate(false);
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
            // propertyChange is called several times
            workflowNode.setValid(false);
        }

    }

}
