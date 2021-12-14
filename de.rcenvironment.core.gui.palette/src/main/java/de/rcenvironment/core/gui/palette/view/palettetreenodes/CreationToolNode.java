/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Optional;

import org.eclipse.gef.Tool;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * {@link PaletteTreeNode} representing editor tools like selection and connection.
 * 
 * @author Jan Flink
 *
 */
public class CreationToolNode extends PaletteTreeNode {

    private Tool tool;

    private String contextID;

    /**
     * @param value
     */
    public CreationToolNode(PaletteTreeNode parent, String nodeName, Tool tool, Image icon) {
        super(parent, nodeName);
        setIcon(icon);
        setTool(tool);
    }

    private void setTool(Tool tool) {
        this.tool = tool;
    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        Optional<WorkflowEditor> optional = paletteView.getWorkflowEditor();
        if (!optional.isPresent()) {
            return;
        }

        WorkflowEditor editor = optional.get();
        editor.onPaletteDoubleClick(getTool());
    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        editor.getViewer().getEditDomain().setActiveTool(getTool());
    }

    public Tool getTool() {
        return tool;
    }

    @Override
    public void handleEditEvent() {
        // Intentionally left empty.
    }

    @Override
    public boolean canHandleEditEvent() {
        return false;
    }

    @Override
    public boolean isCustomized() {
        return false;
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.ofNullable(contextID);
    }

    public void setHelpContextID(String toolContextID) {
        this.contextID = toolContextID;
    }

}

