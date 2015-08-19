/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.draw2d.Label;
import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Command that changes the text of a {@link WorkflowLabel}.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelEditCommand extends Command {

    private final Label label;

    /** The new text. */
    private String text;

    /** The old text. */
    private String oldText;

    private final WorkflowLabel wLabel;

    public WorkflowLabelEditCommand(Label label2, WorkflowLabel wLabel) {
        label = label2;
        this.wLabel = wLabel;
    }

    @Override
    public void execute() {
        oldText = label.getText();
        redo();
    }

    public void setNewName(String newName) {
        text = newName;
    }

    @Override
    public void redo() {
        wLabel.setText(text);
        label.setText(text);
    }

    @Override
    public void undo() {
        wLabel.setText(oldText);
        label.setText(oldText);
    }
}
