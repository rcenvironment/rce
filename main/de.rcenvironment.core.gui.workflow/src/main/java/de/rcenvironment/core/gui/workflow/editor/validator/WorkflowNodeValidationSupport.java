/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * A utility class to support the integration of validation into other components. This class
 * encapsulates the plumbing code to handle multiple listeners for multiple validators and the
 * dynamic changing of the backing {@link WorkflowNode}.
 * 
 * <p>
 * <b>Here's how the <i>validation</i> works:</b> In {@link #setWorkflowNode(WorkflowNode)} the
 * backing {@link WorkflowNode} is set. If no other <code>WorkflowNode</code> has been set before,
 * the according {@link WorkflowNodeValidator}s are retrieved. Those validators fire update events
 * when properties are set to their configuration and their validity state changes. All listeners
 * that have been registered will get registered with those validators and thus information about
 * changes are passed through to them. If the <code>WorkflowNode</code> to be set is not the first
 * one, the registered listeners are removed from the validators first.
 * </p>
 * 
 * @author Christian Weiss
 */
public class WorkflowNodeValidationSupport {

    private WorkflowNode workflowNode;

    private final List<WorkflowNodeValidator> validators = new ArrayList<WorkflowNodeValidator>();

    private final Set<WorkflowNodeValidityStateListener> stateListeners = new HashSet<WorkflowNodeValidityStateListener>();;

    private final Set<WorkflowNodeValidityChangeListener> changeListeners = new HashSet<WorkflowNodeValidityChangeListener>();

    private final WorkflowNodeValidityChangeListener validityListener = new WorkflowNodeValidityChangeListener() {

        @Override
        public void handleWorkflowNodeValidityChangeEvent(final WorkflowNodeValidityChangeEvent event) {
            updateValid();
        }

    };

    private boolean valid;

    /**
     * Sets the new {@link WorkflowNode}.
     * 
     * @param workflowNode the new {@link WorkflowNode}
     */
    public synchronized void setWorkflowNode(final WorkflowNode workflowNode) {
        this.workflowNode = workflowNode;
    }

    /**
     * Sets the new {@link WorkflowNode} and adjusts the {@link WorkflowNodeValidator}s.
     * 
     * @param node the new {@link WorkflowNode}
     */
    public synchronized void setWorkflowNodeAndValidation(final WorkflowNode node) {
        if (this.workflowNode != null) {
            deactivateValidators();
        }
        this.workflowNode = node;
        if (this.workflowNode != null) {
            activateValidators();
        }
    }

    /**
     * Activates the {@link WorkflowNodeValidator}s for this {@link WorkflowNode}.
     * 
     * <p>
     * Procedure (as outlined in the doc of {@link WorkflowNodeValidatorsRegistry}):
     * <ol>
     * <li>Retrieve the {@link WorkflowNodeValidator} instances for this {@link WorkflowNode} from
     * the {@link WorkflowNodeValidatorsRegistry}.</li>
     * <li>Add all returned {@link WorkflowNodeValidator} instances to local set (in
     * {@link #validators}).</li>
     * <li>Add the local {@link WorkflowNodeValidityStateListener} in {@link #stateListeners} to
     * each validator.</li>
     * <li>Add the local {@link WorkflowNodeValidityChangeListener} in {@link #changeListeners} to
     * each validator.</li>
     * </ol>
     * </p>
     * 
     */
    private synchronized void activateValidators() {
        deactivateValidators();
        final WorkflowNodeValidatorsRegistry registry = WorkflowNodeValidatorsRegistry.Factory.getInstance();
        List<WorkflowNodeValidator> validatorInstances = registry.getValidatorsForWorkflowNode(workflowNode, false);
        validators.addAll(validatorInstances);
        Iterator<WorkflowNodeValidator> it = validators.iterator();
        
        while (it.hasNext()) {
            WorkflowNodeValidator validator = it.next();
            
            validator.addWorkflowNodeValidityChangeListener(validityListener);
            for (final WorkflowNodeValidityStateListener stateListener : stateListeners) {
                validator.addWorkflowNodeValidityStateListener(stateListener);
            }
            for (final WorkflowNodeValidityChangeListener changeListener : changeListeners) {
                validator.addWorkflowNodeValidityChangeListener(changeListener);
            }
        }
    }

    /**
     * Deactivates the {@link WorkflowNodeValidator}s for this {@link WorkflowNode}.
     * 
     * <p>
     * Procedure (as outlined in the doc of {@link WorkflowNodeValidatorsRegistry}):
     * <ol>
     * <li>Remove the local {@link WorkflowNodeValidityStateListener} in {@link #stateListeners}
     * from each validator in {@link #validators}.</li>
     * <li>Remove the local {@link WorkflowNodeValidityChangeListener} in {@link #changeListeners}
     * from each validator in {@link #validators}.</li>
     * </ol>
     * </p>
     * 
     */
    private synchronized void deactivateValidators() {
        Iterator<WorkflowNodeValidator> it = validators.iterator();
        
        while (it.hasNext()) {
            WorkflowNodeValidator validator = it.next();
            for (final WorkflowNodeValidityStateListener stateListener : stateListeners) {
                validator.removeWorkflowNodeValidityStateListener(stateListener);
            }
            for (final WorkflowNodeValidityChangeListener changeListener : changeListeners) {
                validator.removeWorkflowNodeValidityChangeListener(changeListener);
            }
            validator.removeWorkflowNodeValidityChangeListener(validityListener);
        }
        validators.clear();
    }

    private synchronized void updateValid() {
        boolean valid2 = true;
        /*
         * All validators need to be valid, thus iterate over all of them.
         */
        for (final WorkflowNodeValidator validator : validators) {
            valid2 &= validator.isValid();
            if (!valid2) {
                break;
            }
        }
        this.valid = valid2;
    }

    /**
     * Returns, whether this {@link WorkflowNodeValidator} has a validation status of successful.
     * 
     * @return true, if no errors appeared during validation
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the {@link WorkflowValidationMessage}s.
     * 
     * @return a collection of {@link WorkflowNodeValidationMessage} informing about the validation
     *         results
     */
    public Collection<WorkflowNodeValidationMessage> getMessages() {
        final List<WorkflowNodeValidationMessage> result = new LinkedList<WorkflowNodeValidationMessage>();
        for (final WorkflowNodeValidator validator : validators) {
            result.addAll(validator.getMessages());
        }
        return result;
    }

    /**
     * Returns the recent {@link WorkflowValidationMessage}s.
     * 
     * @return a collection of {@link WorkflowNodeValidationMessage} informing about the validation results
     */
    public Collection<WorkflowNodeValidationMessage> getRecentMessages() {
        final List<WorkflowNodeValidationMessage> result = new LinkedList<WorkflowNodeValidationMessage>();
        for (final WorkflowNodeValidator validator : validators) {
            result.addAll(validator.getMessages());
        }
        result.addAll(WorkflowNodeValidatonMessageStore.getInstance().getRecentMessages(result, workflowNode.getIdentifier()));
        return result;
    }

    /**
     * Adds a {@link WorkflowNodeValidityChangeListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityChangeListener} to add
     */
    public synchronized void addWorkflowNodeValidityChangeListener(final WorkflowNodeValidityChangeListener listener) {
        changeListeners.add(listener);
        for (final WorkflowNodeValidator validator : validators) {
            validator.addWorkflowNodeValidityChangeListener(listener);
        }
    }

    /**
     * Removes a {@link WorkflowNodeValidityChangeListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityChangeListener} to remove
     */
    public synchronized void removeWorkflowNodeValidityChangeListener(final WorkflowNodeValidityChangeListener listener) {
        for (final WorkflowNodeValidator validator : validators) {
            validator.removeWorkflowNodeValidityChangeListener(listener);
        }
        changeListeners.remove(listener);
    }

    /**
     * Adds a {@link WorkflowNodeValidityStateListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityStateListener} to add
     */
    public synchronized void addWorkflowNodeValidityStateListener(final WorkflowNodeValidityStateListener listener) {
        stateListeners.add(listener);
        for (final WorkflowNodeValidator validator : validators) {
            validator.addWorkflowNodeValidityStateListener(listener);
        }
    }

    /**
     * Removes a {@link WorkflowNodeValidityStateListener}.
     * 
     * @param listener the {@link WorkflowNodeValidityStateListener} to remove
     */
    public synchronized void removeWorkflowNodeValidityStateListener(final WorkflowNodeValidityStateListener listener) {
        for (final WorkflowNodeValidator validator : validators) {
            validator.removeWorkflowNodeValidityStateListener(listener);
        }
        stateListeners.remove(listener);
    }

}
