/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.endpoint.EndpointHelper;
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
    public XPathChooserPropertyViewPane(String genericEndpointTitle, final EndpointType direction,
        final WorkflowNodeCommand.Executor executor, String id) {
        super(genericEndpointTitle, direction, executor, false, id, false);
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
                endpointType, endpointIdToManage, false,
                icon, endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(), new HashMap<String, String>());
        super.onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = EndpointHelper.getStaticEndpointNames(endpointType, configuration).contains(name);
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointEditDialog dialog =
            new XPathEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.EDIT, configuration,
                endpointType, endpointIdToManage, isStaticEndpoint,
                icon, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(), newMetaData);

        super.onEditClicked(name, dialog, newMetaData);
    }

}
