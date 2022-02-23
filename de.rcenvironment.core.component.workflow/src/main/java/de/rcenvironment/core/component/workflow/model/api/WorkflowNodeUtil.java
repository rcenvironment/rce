/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Utility functions to access {@link WorkflowNode}s.
 * 
 * @author Christian Weiss
 */
public final class WorkflowNodeUtil {

    private WorkflowNodeUtil() {}

    /**
     * Returns, whether the specified {@link WorkflowNode} has inputs.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @return true, the specified {@link WorkflowNode} has inputs
     */
    public static boolean hasInputs(final WorkflowNode workflowNode) {
        final ComponentDescription compDescription = workflowNode.getComponentDescription();
        return !compDescription.getInputDescriptionsManager().getEndpointDescriptions().isEmpty();
    }

    /**
     * Returns, whether the specified {@link WorkflowNode} has outputs.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @return true, if the specified {@link WorkflowNode} has outputs
     */
    public static boolean hasOutputs(final WorkflowNode workflowNode) {
        final ComponentDescription compDescription = workflowNode.getComponentDescription();
        return !compDescription.getOutputDescriptionsManager().getEndpointDescriptions().isEmpty();
    }

    /**
     * Returns, whether the specified {@link WorkflowNode} has inputs of the specified type.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @param type the desired type of inputs
     * @return true, if the specified {@link WorkflowNode} has inputs of the specified type
     */
    public static boolean hasInputs(WorkflowNode workflowNode, DataType type) {
        return !getInputsByDataType(workflowNode, type).isEmpty();
    }

    /**
     * Returns, whether the specified {@link WorkflowNode} has outputs of the specified type.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @param type the desired type of outputs
     * @return true, if the specified {@link WorkflowNode} has outputs of the specified type
     */
    public static boolean hasOutputs(WorkflowNode workflowNode, DataType type) {
        return !getOutputs(workflowNode, type).isEmpty();
    }

    /**
     * Returns all inputs of the specified {@link WorkflowNode}.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @return all inputs of the specified {@link WorkflowNode}
     */
    public static Set<EndpointDescription> getInputs(final WorkflowNode workflowNode) {
        return Collections.unmodifiableSet(workflowNode.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions());
    }

    /**
     * Returns the inputs of the specified {@link WorkflowNode} having the specified type.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @param type the desired type of inputs
     * @return the inputs of the specified {@link WorkflowNode} having the specified type
     */
    public static Set<EndpointDescription> getInputsByDataType(WorkflowNode workflowNode, DataType type) {
        return getEndpointsByDataType(workflowNode.getInputDescriptionsManager(), type);
    }

    /**
     * Returns all outputs of the specified {@link WorkflowNode}.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @return all outputs of the specified {@link WorkflowNode}
     */
    public static Set<EndpointDescription> getOutputs(WorkflowNode workflowNode) {
        return Collections.unmodifiableSet(workflowNode.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions());
    }

    /**
     * Returns the outputs of the specified {@link WorkflowNode} having the specified type.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @param type the desired type of outputs
     * @return the outputs of the specified {@link WorkflowNode} having the specified type
     */
    public static Set<EndpointDescription> getOutputs(WorkflowNode workflowNode, DataType type) {
        return getEndpointsByDataType(workflowNode.getOutputDescriptionsManager(), type);
    }

    private static Set<EndpointDescription> getEndpointsByDataType(EndpointDescriptionsManager endpointManager, final DataType type) {

        Set<EndpointDescription> result = new HashSet<EndpointDescription>();

        for (final EndpointDescription endpointDesc : endpointManager.getEndpointDescriptions()) {
            if (type == endpointDesc.getDataType()) {
                result.add(endpointDesc);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns, whether the specified {@link WorkflowNode} has a property with the specified key.
     * 
     * @param workflowNode the {@link WorkflowNode}
     * @param key the key of the property
     * @return the type of the property
     */
    public static boolean hasConfigurationValue(final WorkflowNode workflowNode, final String key) {
        return workflowNode.getConfigurationDescription()
            .getComponentConfigurationDefinition().getConfigurationKeys().contains(key);
    }

    /**
     * Returns, whether the specified property of the specified
     * {@link ReadableComponentInstanceConfiguration} is set.
     * 
     * @param workflowNode the {@link ReadableComponentInstanceConfiguration}
     * @param key the key of the property
     * @return true, if the property is set
     */
    public static boolean isConfigurationValueSet(final ComponentInstanceProperties workflowNode, final String key) {
        final boolean result = getConfigurationValue(workflowNode, key) != null;
        return result;
    }

    /**
     * Returns the value of the specified property of the specified
     * {@link ReadableComponentInstanceConfiguration}.
     * 
     * @param workflowNode the {@link ReadableComponentInstanceConfiguration}
     * @param key the key of the property
     * @return the value of the property
     */
    public static String getConfigurationValue(final ComponentInstanceProperties workflowNode, final String key) {
        return workflowNode.getConfigurationDescription().getConfigurationValue(key);
    }

}
