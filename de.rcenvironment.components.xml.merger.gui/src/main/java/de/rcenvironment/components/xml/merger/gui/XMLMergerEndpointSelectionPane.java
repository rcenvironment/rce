/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;
import de.rcenvironment.core.gui.xpathchooser.XPathChooserPropertyViewPane;

/**
 * EndpointSelectionPane for XMLMerger.
 *
 * @author Brigitte Boden
 */
public class XMLMergerEndpointSelectionPane extends XPathChooserPropertyViewPane {

    public XMLMergerEndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, String[] dynEndpointIdsToShow,
        String[] statEndpointNamesToShow, Executor executor) {
        super(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, statEndpointNamesToShow, executor);
    }

    @Override
    public void setConfiguration(ComponentInstanceProperties configuration) {
        super.setConfiguration(configuration);
        endpointManager.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateTable();
            }
        });
    }

    @Override
    protected void onAddClicked() {
        EndpointEditDialog dialog =
            new XMLMergerEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD, configuration,
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
            new XMLMergerEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.EDIT, configuration,
                endpointType, dynEndpointIdToManage, isStaticEndpoint,
                icon, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        super.onEditClicked(name, dialog, newMetaData);
    }

}
