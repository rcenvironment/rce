/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.List;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Registry for {@link WorkflowNodeValidator}s.
 * 
 * <p>
 * The usual way to integrate the validator facilities is:
 * <ol>
 * <li>Setup
 * <ol>
 * <li>Retrieve the {@link WorkflowNodeValidator} instances for this {@link WorkflowNode} from the
 * {@link WorkflowNodeValidatorsRegistry}.</li>
 * <li>Add all returned {@link WorkflowNodeValidator} instances to a local container (e.g. a
 * <code>Set</code>).</li>
 * <li>Add a listener to each validator to get informed about validity events. Use a
 * {@link WorkflowNodeValidityStateListener} to receive all events or
 * {@link WorkflowNodeValidityChangeListener} to only get informed about state changes.</li>
 * <li>The listener should handle all necessary stuff to react on validity changes.</li>
 * </ol>
 * </li>
 * <li>Processing<p>... should be handled in the listener</p></li>
 * <li>Teardown</li>
 * <ol>
 * <li>Remove the listener from each validator in the local container.</li>
 * </ol>
 * </ol>
 * </p>
 * 
 * @author Christian Weiss
 */
public interface WorkflowNodeValidatorsRegistry {

    /** The ID of the extension point. */
    String EXTENSION_POINT_ID = "de.rcenvironment.core.gui.workflow.nodeValidators";

    /**
     * Initializes the {@link WorkflowNodeValidatorsRegistry} instance.
     * 
     */
    void initialize();

    /**
     * Determines the {@link WorkflowNodeValidator}s for the specified {@link WorkflowNode} and returns instances thereof.
     * 
     * @param workflowNode the {@link WorkflowNode} to get the {@link WorkflowNodeValidator}s for
     * @param onWorkflowStart identifies if the workflow is executed
     * @return the {@link WorkflowNodeValidator}s for the specified {@link WorkflowNode}
     */
    List<WorkflowNodeValidator> getValidatorsForWorkflowNode(WorkflowNode workflowNode, boolean onWorkflowStart);

    /**
     * Factory method to create a {@link WorkflowNodeValidatorsRegistry}.
     * 
     * @author Christian Weiss
     */
    public static final class Factory {

        private static WorkflowNodeValidatorsRegistry instance;

        private Factory() {
            // do nothing
        }

        /**
         * Returns the single instance of {@link WorkflowNodeValidatorsRegistry}.
         * 
         * @return the single {@link WorkflowNodeValidatorsRegistry} instance
         */
        public static WorkflowNodeValidatorsRegistry getInstance() {
            if (instance == null) {
                final String classString = System.getProperty(
                    "de.rcenvironment.rce.gui.workflow.editor.validator.WorkflowNodeValidatorsRegistry");
                if (classString == null) {
                    instance = new WorkflowNodeValidatorsRegistryImpl();
                } else {
                    try {
                        final Class<?> clazz = Class.forName(classString);
                        instance = (WorkflowNodeValidatorsRegistry) clazz.newInstance();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load WorkflowNodeValidatorsRegistry class.", e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException("Failed to instantiate WorkflowNodeValidatorsRegistry class.", e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to instantiate WorkflowNodeValidatorsRegistry class.", e);
                    }
                }
                instance.initialize();
            }
            return instance;
        }

    }

}
