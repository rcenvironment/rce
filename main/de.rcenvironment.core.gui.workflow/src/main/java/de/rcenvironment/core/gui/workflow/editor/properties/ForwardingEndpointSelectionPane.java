/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager). This one is for forwarding values.
 *
 * @author Sascha Zur
 */
public class ForwardingEndpointSelectionPane extends EndpointSelectionPane {

    public ForwardingEndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage,
        String[] dynEndpointIdsToShow, WorkflowNodeCommand.Executor executor) {
        super(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, new String[] {}, executor, false, true);
    }

    @Override
    protected void onAddClicked() {
        EndpointInputWithOutputEditDialog dialog =
            new EndpointInputWithOutputEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
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

        EndpointInputWithOutputEditDialog dialog =
            new EndpointInputWithOutputEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                dynEndpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        onEditClicked(name, dialog, newMetaData);
    }
}
