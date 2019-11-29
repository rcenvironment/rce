/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider.Endpoint;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class helping handling {@link Endpoint}s.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public final class EndpointHandlingHelper {

    private EndpointHandlingHelper() {}

    /**
     * Extracts {@link Endpoint}s for the given properties and returns them.
     * 
     * @param workflowNode {@link WorkflowNode}.
     * @param type Input oder output?
     * @return {@link Endpoint}s.
     */
    public static Collection<Endpoint> getEndpoints(WorkflowNode workflowNode, EndpointType type) {

        Collection<EndpointContentProvider.Endpoint> endpoints = new HashSet<EndpointContentProvider.Endpoint>();

        for (EndpointDescription endpointDesc : getEndpointDefinitions(workflowNode, type).getEndpointDescriptions()) {
            endpoints.add(new Endpoint(workflowNode, endpointDesc));
        }
        return endpoints;
    }

    private static EndpointDescriptionsManager getEndpointDefinitions(WorkflowNode workflowNode, EndpointType type) {

        if (type == EndpointType.INPUT) {
            return workflowNode.getComponentDescription().getInputDescriptionsManager();
        } else {
            return workflowNode.getComponentDescription().getOutputDescriptionsManager();
        }

    }

    /**
     * Checks if data type of endpoint can be changed.
     * 
     * @param endpointType input or output?
     * @param oldDesc old endpoint
     * @param newDataType target data type
     * @return <code>true</code> if data type can be changed to target data type, otherwise
     *         <code>false</code>
     */
    public static boolean editEndpointDataType(EndpointType endpointType, EndpointDescription oldDesc, DataType newDataType) {
        if (oldDesc.getDataType() != newDataType && !oldDesc.isDataTypeValid(newDataType)) {
            String source = Messages.input;
            String target = Messages.output;
            if (endpointType == EndpointType.OUTPUT) {
                source = target;
                target = Messages.input;
            }
            if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                Messages.invalidDataTypeDialogTitle,
                StringUtils.format(Messages.invalidDataTypeDialogMessage,
                    source, oldDesc.getName(), target.toLowerCase(), oldDesc.getName(), oldDesc.getDataType(), newDataType))) {
                return false;
            }
        }
        return true;
    }

}
