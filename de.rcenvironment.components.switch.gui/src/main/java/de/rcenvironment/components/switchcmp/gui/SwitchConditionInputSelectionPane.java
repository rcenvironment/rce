/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * 
 * Implementation of {@link EndpointSelectionPane}.
 * 
 * @author David Scholz
 */
public class SwitchConditionInputSelectionPane extends EndpointSelectionPane {

    public SwitchConditionInputSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, Executor executor) {
        super(title, direction, dynEndpointIdToManage, new String[] {}, new String[] {}, executor);
    }

    @Override
    protected void onAddClicked() {
        SwitchEndpointEditDialog dialog =
            new SwitchEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, dynEndpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>());
        onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        SwitchEndpointEditDialog dialog =
            new SwitchEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                dynEndpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        onEditClicked(name, dialog);
    }
}
