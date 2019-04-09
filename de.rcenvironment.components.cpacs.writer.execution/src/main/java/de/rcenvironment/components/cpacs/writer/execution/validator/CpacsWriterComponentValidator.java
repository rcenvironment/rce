/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.execution.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.components.cpacs.writer.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for CPACS Writer component.
 * 
 * @author Jan Flink
 * @author Jascha Riedel
 */
public class CpacsWriterComponentValidator extends AbstractComponentValidator {

    private static final String PROPERTY_LOCAL_FOLDER = "localFolder";

    @Override
    public String getIdentifier() {
        return CpacsWriterComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();
        if (!isPropertySet(componentDescription, PROPERTY_LOCAL_FOLDER)
                || getProperty(componentDescription, PROPERTY_LOCAL_FOLDER).isEmpty()) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, PROPERTY_LOCAL_FOLDER,
                    Messages.localFolderNotConfigured, Messages.localFolderNotConfiguredLong));
            return messages;
        }
        File targetFolder = new File(getProperty(componentDescription, PROPERTY_LOCAL_FOLDER));
        if (!targetFolder.isAbsolute()) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, PROPERTY_LOCAL_FOLDER,
                    Messages.localFolderPathNotAbsolute, Messages.localFolderPathNotAbsoluteLong));
        }

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

}
