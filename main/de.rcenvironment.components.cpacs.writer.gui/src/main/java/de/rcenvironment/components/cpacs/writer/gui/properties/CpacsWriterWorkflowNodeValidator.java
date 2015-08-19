/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.properties;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for CPACS Writer component.
 * 
 * @author Jan Flink
 */
public class CpacsWriterWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private static final String PROPERTY_LOCAL_FOLDER = "localFolder";

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator#validate()
     */
    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        if (!isPropertySet(PROPERTY_LOCAL_FOLDER) || getProperty(PROPERTY_LOCAL_FOLDER).isEmpty()) {
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING, PROPERTY_LOCAL_FOLDER,
                Messages.localFolderNotConfigured,
                Messages.localFolderNotConfiguredLong));
            return messages;
        }
        File targetFolder = new File(getProperty(PROPERTY_LOCAL_FOLDER));
        if (!targetFolder.isAbsolute()) {
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING, PROPERTY_LOCAL_FOLDER,
                Messages.localFolderPathNotAbsolute,
                Messages.localFolderPathNotAbsoluteLong));
        }

        return messages;
    }
}
