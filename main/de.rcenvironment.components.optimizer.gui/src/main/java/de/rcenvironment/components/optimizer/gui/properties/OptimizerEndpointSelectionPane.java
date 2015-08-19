/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerAddDynamicEndpointCommand;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerEditDynamicEndpointCommand;
import de.rcenvironment.components.optimizer.gui.properties.commands.OptimizerRemoveDynamicEndpointCommand;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager). 
 * @author Sascha Zur
 */
public class OptimizerEndpointSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane[] allPanes;

    /**
     * @param genericEndpointTitle the display text describing individual endpoints (like "Input" or
     *        "Output"); used in dialog texts
     */
    public OptimizerEndpointSelectionPane(String genericEndpointTitle, final EndpointType direction,
        final WorkflowNodeCommand.Executor executor, String id, boolean readonly) {
        super(genericEndpointTitle, direction, executor, readonly, id, true, false);
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
            buttonEdit.setEnabled(false);
            buttonRemove.setEnabled(false);
        }
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command =
            new OptimizerAddDynamicEndpointCommand(endpointType, name, endpointIdToManage, type, metaData, allPanes);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command =
            new OptimizerEditDynamicEndpointCommand(endpointType, endpointIdToManage, oldDescription, newDescription,
                true, true, allPanes);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new OptimizerRemoveDynamicEndpointCommand(endpointType, names, endpointIdToManage, allPanes);
        execute(command);
    }

    public EndpointSelectionPane[] getAllPanes() {
        return allPanes;
    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.allPanes = allPanes;
    }

}
