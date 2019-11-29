/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.components.xml.merger.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Validator for XML Merger component.
 * 
 * @author Jan Flink
 * @author Jascha Riedel
 */
public class XmlMergerComponentValidator extends AbstractComponentValidator {

    private static final String PROPERTY_MAPPING_TYPE = "mappingType";

    private static final String PROPERTY_XML_CONTENT = "xmlContent";

    @Override
    public String getIdentifier() {
        return XmlMergerComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();
        boolean mappingFileInputExists = getInputs(componentDescription, DataType.FileReference).size() == 3;
        // TODO : add messages
        if (!mappingFileInputExists && !isPropertySet(componentDescription, PROPERTY_XML_CONTENT)) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, PROPERTY_XML_CONTENT,
                    Messages.noXmlFileLoaded, Messages.noXmlFileLoadedLong));
        }
        if (!mappingFileInputExists && !isPropertySet(componentDescription, PROPERTY_MAPPING_TYPE)) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, PROPERTY_MAPPING_TYPE,
                    Messages.unknownMappingType, Messages.unknownMappingTypeLong));
        }
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

}
