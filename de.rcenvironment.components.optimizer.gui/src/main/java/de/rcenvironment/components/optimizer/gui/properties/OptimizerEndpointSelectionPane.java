/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerAddDynamicEndpointCommand;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerEditDynamicEndpointCommand;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerRemoveDynamicEndpointCommand;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.configuration.VariableNameVerifyListener;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager).
 * 
 * @author Sascha Zur
 */
public class OptimizerEndpointSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane[] allPanes;

    public OptimizerEndpointSelectionPane(String title, final EndpointType direction, String dynEndpointIdToManage,
        WorkflowNodeCommand.Executor executor, boolean readOnly) {
        super(title, direction, dynEndpointIdToManage, new String[] {}, new String[] {}, executor, readOnly, true);
    }

    @Override
    protected void fillTable() {
        super.fillTable();
        Display display = Display.getCurrent();
        for (TableItem i : table.getItems()) {
            if (i.getText(0).contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                i.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
            }
            if (i.getText(0).contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                i.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
            }
        }
    }

    @Override
    protected void updateButtonActivation() {
        super.updateButtonActivation();
        TableItem[] selection = table.getSelection();
        if (selection.length > 0 && (selection[0].getText(0).contains(OptimizerComponentConstants.GRADIENT_DELTA)
            || (selection[0].getText(0).contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)))) {
            if (buttonEdit != null) {
                buttonEdit.setEnabled(false);
            }
            if (buttonRemove != null) {
                buttonRemove.setEnabled(false);
            }
        }
    }

    @Override
    protected void onAddClicked() {
        EndpointEditDialog dialog =
            new OptimizerEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, dynEndpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
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
            new OptimizerEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                dynEndpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);

        super.onEditClicked(name, dialog);
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command =
            new OptimizerAddDynamicEndpointCommand(endpointType, name, dynEndpointIdToManage, type, metaData, allPanes);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command =
            new OptimizerEditDynamicEndpointCommand(endpointType, dynEndpointIdToManage, oldDescription, newDescription,
                true, true, allPanes);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new OptimizerRemoveDynamicEndpointCommand(endpointType, names, dynEndpointIdToManage, allPanes);
        execute(command);
    }

    public EndpointSelectionPane[] getAllPanes() {
        return allPanes;
    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.allPanes = allPanes;
    }

    /**
     * 
     * Implementation of {@link EndpointEditDialog}.
     * 
     * @author Sascha Zur
     */
    private class OptimizerEndpointEditDialog extends EndpointEditDialog {

        OptimizerEndpointEditDialog(Shell parentShell, EndpointActionType actionType,
            ComponentInstanceProperties configuration,
            EndpointType direction, String id, boolean isStatic, EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
            super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
        }

        @Override
        protected void createEndpointSettings(Composite parent) {
            super.createEndpointSettings(parent);
            textfieldName.addListener(SWT.Verify, new VariableNameVerifyListener(VariableNameVerifyListener.NO_UNDERSCORE,
                this.textfieldName));
        }

        @Override
        protected void validateInput() {
            super.validateInput();
            String text = textfieldName.getText();
            getButton(IDialogConstants.OK_ID).setEnabled(
                getButton(IDialogConstants.OK_ID).isEnabled()
                    && (text != null && !text.isEmpty() && !text.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)));

        }
    }
}
