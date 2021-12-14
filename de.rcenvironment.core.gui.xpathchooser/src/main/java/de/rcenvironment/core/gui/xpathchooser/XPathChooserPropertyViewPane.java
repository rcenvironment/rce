/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * A "Properties" view tab for configuring dynamic endpoints. Allows new channels to be added via XPathChooser.
 * 
 * @author Markus Kunde
 * @author Markus Litz
 */
public class XPathChooserPropertyViewPane extends EndpointSelectionPane {

    protected EndpointSelectionPane[] allPanes;

    /**
     * Constructor.
     * 
     * @param genericEndpointTitle title of generic end point
     * @param direction direction of endpoint
     * @param typeSelectionFactory type selector
     * @param executor executor
     */
    public XPathChooserPropertyViewPane(String title, EndpointType direction, String dynEndpointIdToManage,
        String[] dynEndpointIdsToShow, String[] statEndpointNamesToShow, WorkflowNodeCommand.Executor executor) {
        super(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, statEndpointNamesToShow, executor);
    }

    public EndpointSelectionPane[] getAllPanes() {
        return allPanes;
    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.allPanes = allPanes;
    }

    @Override
    protected void onAddClicked() {
        EndpointEditDialog dialog =
            new XPathEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD, configuration,
                endpointType, dynEndpointIdToManage, false,
                icon, endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>());
        super.onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointEditDialog dialog =
            new XPathEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.EDIT, configuration,
                endpointType, dynEndpointIdToManage, isStaticEndpoint,
                icon, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        super.onEditClicked(name, dialog);
    }

}
