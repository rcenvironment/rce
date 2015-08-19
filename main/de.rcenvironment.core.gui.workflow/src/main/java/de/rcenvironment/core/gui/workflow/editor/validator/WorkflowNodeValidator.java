/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.Collection;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Validator for {@link WorkflowNode}s.
 * 
 * <p>
 * This interface comprises the following design decisions:
 * <ul>
 * <li>Validators are loosely coupled to validating program parts. This is realized through the
 * listener model using {@link WorkflowNodeValidityChangeListener}s and
 * {@link WorkflowNodeValidityStateListener}s. program part can register itself as interested in
 * changes in the validity state of a {@link WorkflowNode}.</li>
 * <li>Two listener interfaces are used, {@link WorkflowNodeValidityChangeListener} and
 * {@link WorkflowNodeValidityStateListener} to separate the broadcasting of changes in the validity
 * state from one state to another (change) from events that preserve the state but e.g. reduce, add
 * or change {@link WorkflowNodeValidationMessage}s.</li>
 * <li>The interface uses {@link WorkflowNode} so its implementations are able to access the input
 * and output channels. The read-only interface <code>ReadableComponentInstanceConfiguration</code>
 * does not yield access to those.</li>
 * </ul>
 * 
 * </p>
 * 
 * @author Christian Weiss
 */
public interface WorkflowNodeValidator {

    /**
     * Sets the {@link WorkflowNode} to validate.
     * 
     * @param workflowNode the {@link WorkflowNode} to validate
     */
    void setWorkflowNode(WorkflowNode workflowNode);

    /**
     * Returns, whether this {@link WorkflowNodeValidator} has a validation status of successful.
     * 
     * @return true, if no errors appeared during validation
     */
    boolean isValid();

    /**
     * Returns the {@link WorkflowValidationMessage}s that are the results of the last
     * {@link #validate()} call.
     * 
     * @return a collection of {@link WorkflowNodeValidationMessage} informing about the validation
     *         results
     */
    Collection<WorkflowNodeValidationMessage> getMessages();

    /**
     * Adds a {@link WorkflowNodeValidityChangeListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityChangeListener} to add
     */
    void addWorkflowNodeValidityChangeListener(WorkflowNodeValidityChangeListener listener);

    /**
     * Removes a {@link WorkflowNodeValidityChangeListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityChangeListener} to remove
     */
    void removeWorkflowNodeValidityChangeListener(WorkflowNodeValidityChangeListener listener);

    /**
     * Adds a {@link WorkflowNodeValidityStateListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityStateListener} to add
     */
    void addWorkflowNodeValidityStateListener(WorkflowNodeValidityStateListener listener);

    /**
     * Removes a {@link WorkflowNodeValidityStateListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityStateListener} to remove
     */
    void removeWorkflowNodeValidityStateListener(WorkflowNodeValidityStateListener listener);

}
