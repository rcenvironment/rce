/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.components.cpacs.vampzeroinitializer.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for CPACS Generator component.
 * 
 * @author Jan Flink
 * @author Jascha Riedel
 */
public class VampZeroInitializerComponentValidator extends AbstractComponentValidator {

    private static final String PROPERTY_XML_CONTENT = "xmlContent";

    @Override
    public String getIdentifier() {
        return VampZeroInitializerComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();
        if (!isPropertySet(componentDescription, PROPERTY_XML_CONTENT)) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, PROPERTY_XML_CONTENT,
                    Messages.noXmlContentGenerated, Messages.noXmlContentGeneratedLong));
        }
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

}
