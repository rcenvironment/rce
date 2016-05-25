/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.gui.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for XML Loader component.
 * 
 * @author Jan Flink
 */
public class XmlLoaderWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private static final String PROPERTY_XML_CONTENT = "xmlContent";

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator#validate()
     */
    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        if (!isPropertySet(PROPERTY_XML_CONTENT)) {
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, PROPERTY_XML_CONTENT,
                Messages.noXmlFileLoaded,
                Messages.noXmlFileLoadedLong));
        }

        return messages;
    }
}
