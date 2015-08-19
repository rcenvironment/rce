/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants.FileSourceType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * An adapted EndpointSelectionPane for the InputProvider Component.
 * 
 * @author Mark Geiger
 */
public class InputProviderEndpointSelectionPane extends EndpointSelectionPane {

    private Composite noteComposite;

    public InputProviderEndpointSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor,
        boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints);
    }

    @Override
    public Control createControl(Composite parent, String title, FormToolkit toolkit) {
        Control control = super.createControl(parent, title, toolkit);
        // empty label to get desired layout - feel free to improve
        new Label(client, SWT.READ_ONLY);
        noteComposite = toolkit.createComposite(client);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        noteComposite.setLayoutData(gridData);
        noteComposite.setLayout(new GridLayout(2, false));
        // TODO wrap text of the noteLabel - SWT.WRAP does not work (see Mantis issue 12257). - stam_mr, June 2015.
        CLabel noteLabel = new CLabel(noteComposite, SWT.NONE);
        noteLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
        noteLabel.setText(Messages.note);

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);

        return control;
    }

    @Override
    public void setConfiguration(ComponentInstanceProperties configuration) {
        if (configuration != null) {
            super.setConfiguration(configuration);
            setNoteVisible(areFilesOrDirectoriesDefined());
        }
    }

    private void setNoteVisible(boolean visible) {
        noteComposite.setVisible(visible);
    }

    @Override
    protected void onAddClicked() {
        Map<String, String> metaData = new HashMap<String, String>();

        InputProviderEndpointEditDialog dialog =
            new InputProviderEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.ADD, configuration, endpointType, endpointIdToManage, false,
                icon, endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(), metaData);

        if (dialog.open() == Dialog.OK) {
            String name = dialog.getChosenName();
            DataType type = dialog.getChosenDataType();
            metaData = dialog.getMetadataValues();
            if (metaData.containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
                && metaData.get(InputProviderComponentConstants.META_FILESOURCETYPE).equals(FileSourceType.atWorkflowStart.name())) {
                metaData.put(InputProviderComponentConstants.META_VALUE, "${" + name + "}");
            }
            executeAddCommand(name, type, metaData);
            setNoteVisible(areFilesOrDirectoriesDefined());
        }
    }

    private boolean areFilesOrDirectoriesDefined() {
        for (EndpointDescription desc : configuration.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
            if (desc.getDataType() == DataType.FileReference || desc.getDataType() == DataType.DirectoryReference) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        InputProviderEndpointEditDialog dialog =
            new InputProviderEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                endpointIdToManage, false, icon, endpoint.getDeclarativeEndpointDescription()
                    .getMetaDataDefinition(), newMetaData);

        onEditClicked(name, dialog, newMetaData);
        setNoteVisible(areFilesOrDirectoriesDefined());
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        if (metaData.containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && metaData.get(InputProviderComponentConstants.META_FILESOURCETYPE).equals(FileSourceType.atWorkflowStart.name())) {

            WorkflowNodeCommand command = new InputProviderAddDynamicEndpointCommand(endpointType, endpointIdToManage, name,
                type, metaData, this); // null = this
            execute(command);
        } else {
            super.executeAddCommand(name, type, metaData);
        }
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        if (oldDescription.getMetaData().containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && oldDescription.getMetaData().get(InputProviderComponentConstants.META_FILESOURCETYPE)
                .equals(FileSourceType.atWorkflowStart.name())
            || newDescription.getMetaData().containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && newDescription.getMetaData().get(InputProviderComponentConstants.META_FILESOURCETYPE)
                .equals(FileSourceType.atWorkflowStart.name())) {
            WorkflowNodeCommand command = new InputProviderEditDynamicEndpointCommand(endpointType, oldDescription, newDescription, this);
            execute(command);
        } else {
            super.executeEditCommand(oldDescription, newDescription);
        }
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new InputProviderRemoveDynamicEndpointCommand(endpointType,
            endpointIdToManage, names, null, this); // null = this
        execute(command);
        setNoteVisible(areFilesOrDirectoriesDefined());
    }

}
