/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.endpoint.EndpointHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * .
 * 
 * @author Sascha Zur
 */
public class OutputWriterEndpointSelectionPane extends EndpointSelectionPane {

    public OutputWriterEndpointSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor, boolean readonly,
        String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints);
    }

    @Override
    protected void onAddClicked() {
        Set<String> paths = new TreeSet<String>();
        for (String endpointName : EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage,
            configuration, showOnlyManagedEndpoints)) {
            paths.add(getMetaData(endpointName).get(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING));
        }
        OutputWriterEndpointEditDialog dialog =
            new OutputWriterEndpointEditDialog(Display.getDefault().getActiveShell(),
                String.format(de.rcenvironment.core.gui.workflow.editor.properties.Messages.newMessage, endpointType), configuration,
                endpointType,
                endpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(), new HashMap<String, String>(), paths);

        onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        Set<String> paths = new TreeSet<String>();
        for (String endpointName : EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage,
            configuration, showOnlyManagedEndpoints)) {
            paths.add(getMetaData(endpointName).get(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING));
        }
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = EndpointHelper.getStaticEndpointNames(endpointType, configuration).contains(name);
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        OutputWriterEndpointEditDialog dialog =
            new OutputWriterEndpointEditDialog(Display.getDefault().getActiveShell(),
                String.format(de.rcenvironment.core.gui.workflow.editor.properties.Messages.editMessage, endpointType), configuration,
                endpointType,
                endpointIdToManage, isStaticEndpoint, endpoint.getDeclarativeEndpointDescription()
                    .getMetaDataDefinition(), newMetaData, paths);

        onEditClicked(name, dialog, newMetaData);
    }

    @Override
    protected void fillTable() {
        super.fillTable();
        if (table.getColumnCount() < 4) {
            TableColumn col = new TableColumn(table, SWT.NONE);
            col.setText("Target folder");
            final int columnWeight = 20;
            tableLayout.setColumnData(col, new ColumnWeightData(columnWeight, true));
        }
        final List<String> dynamicEndpointNames = EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage,
            configuration, showOnlyManagedEndpoints);
        Collections.sort(dynamicEndpointNames);
        int i = 0;
        for (String endpoint : dynamicEndpointNames) {
            if (table.getItemCount() > i) {
                table.getItem(i).setText(3, getMetaData(endpoint).get("folderForSaving"));
            }
            i++;
        }
    }
}
