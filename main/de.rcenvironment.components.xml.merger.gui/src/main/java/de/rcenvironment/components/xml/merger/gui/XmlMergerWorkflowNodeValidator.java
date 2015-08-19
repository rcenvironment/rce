/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for XML Merger component.
 * 
 * @author Jan Flink
 */
public class XmlMergerWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private static final String PROPERTY_MAPPING_TYPE = "mappingType";

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
        if (!isPropertySet(PROPERTY_MAPPING_TYPE)) {
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, PROPERTY_MAPPING_TYPE,
                Messages.unknownMappingType,
                Messages.unknownMappingTypeLong));
        }
        return messages;
    }
}
