/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.components.xml.loader.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for XML Loader component.
 * 
 * @author Jan Flink
 * @author Jascha Riedel
 */
public class XmlLoaderComponentValidator extends AbstractComponentValidator {

    private static final String PROPERTY_XML_CONTENT = "xmlContent";

    @Override
    public String getIdentifier() {
        return XmlLoaderComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();
        validateXMLContent(componentDescription, messages);
        return messages;
    }

    private void validateXMLContent(ComponentDescription componentDescription, final List<ComponentValidationMessage> messages) {
        String xmlContent = getProperty(componentDescription, PROPERTY_XML_CONTENT);
        if (xmlContent == null || xmlContent.isEmpty()) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, PROPERTY_XML_CONTENT,
                Messages.noXmlFileLoaded, Messages.noXmlFileLoadedLong));
        }
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();
        validateXMLContent(componentDescription, messages);
        return messages;
    }

}
