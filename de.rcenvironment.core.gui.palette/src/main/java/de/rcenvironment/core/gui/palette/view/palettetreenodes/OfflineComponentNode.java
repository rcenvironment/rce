/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Optional;

import org.eclipse.jface.viewers.TreeNode;

import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;


/**
 * ComponentNode extension representing currently not accessible workflow components.
 *
 * @author Jan Flink
 */
public class OfflineComponentNode extends ComponentNode {

    public OfflineComponentNode(TreeNode parent, ToolIdentification toolIdentification) {
        super(parent, toolIdentification);
    }

    @Override
    public boolean canHandleEditEvent() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isCustomized() {
        return true;
    }

    @Override
    public boolean isAccessibleComponent() {
        return false;
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.empty();
    }

    @Override
    public void handleEditEvent() {
        // intentionally left empty

    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        // intenionally left empty
    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        // intentionally left empty
    }

}
