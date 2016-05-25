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
import de.rcenvironment.core.gui.utils.common.endpoint.EndpointHelper;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager).
 * This one is for forwarding values.
 *
 * @author Sascha Zur
 */
public class ForwardingEndpointSelectionPane extends EndpointSelectionPane {

    public ForwardingEndpointSelectionPane(String genericEndpointTitle, EndpointType direction,
        final WorkflowNodeCommand.Executor executor, boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints, true);
    }

    public ForwardingEndpointSelectionPane(String genericEndpointTitle, EndpointType direction,
        final WorkflowNodeCommand.Executor executor, boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints,
        boolean showInputExecutionConstraint) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints,
            showInputExecutionConstraint);
    }

    @Override
    protected void onAddClicked() {
        EndpointInputWithOutputEditDialog dialog =
            new EndpointInputWithOutputEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, endpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>());
        onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = EndpointHelper.getStaticEndpointNames(endpointType, configuration).contains(name);
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointInputWithOutputEditDialog dialog =
            new EndpointInputWithOutputEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                endpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        onEditClicked(name, dialog, newMetaData);
    }
}
