/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.start.common.Platform;
import de.rcenvironment.core.start.common.validation.internal.PlatformValidatorsRegistry;

/**
 * A manager class that manages the validation of the RCE platform thru the registered {@link PlatformValidator}s.
 * 
 * @author Christian Weiss
 */
// TODO >5.0.0: rename ("StartupValidationManager", maybe?) - misc_ro
public class PlatformValidationManager {

    private static final String VALIDATION_ERROR_LOG_PREFIX = "Validation error: ";

    private static final Log LOGGER = LogFactory.getLog(PlatformValidationManager.class);

    private PlatformValidatorsRegistry validatorsRegistry;

    /**
     * Returns the {@link PlatformValidatorsRegistry}.
     * 
     * @return the {@link PlatformValidatorsRegistry}
     */
    protected synchronized PlatformValidatorsRegistry getValidatorsRegistry() {
        if (validatorsRegistry == null) {
            validatorsRegistry = PlatformValidatorsRegistry.getDefaultInstance();
        }
        return validatorsRegistry;
    }

    /**
     * Sets the {@link PlatformValidatorsRegistry}.
     * 
     * @param validatorsRegistry the {@link PlatformValidatorsRegistry}
     */
    public void setValidatorsRegistry(PlatformValidatorsRegistry validatorsRegistry) {
        if (this.validatorsRegistry != null) {
            throw new IllegalStateException();
        }
        this.validatorsRegistry = validatorsRegistry;
    }

    /**
     * Validates the RCE platform.
     * 
     * @param headless true, if RCE is started in headless mode
     * @return the state of the RCE platform
     */
    public boolean validate(boolean headless) {
        final List<PlatformMessage> messages = new LinkedList<PlatformMessage>();
        final List<PlatformValidator> validators = getValidatorsRegistry().getValidators();
        for (final PlatformValidator validator : validators) {
            try {
                final Collection<PlatformMessage> validationMessages = validator.validatePlatform();
                messages.addAll(validationMessages);
            } catch (RuntimeException e) {
                LOGGER.error(String.format("The execution of the validator '%s' caused an exception",
                    validator.getClass().getName()), e);
                messages.add(new PlatformMessage(
                    PlatformMessage.Type.ERROR, "de.rcenvironment.rce.gui",
                    String.format("The execution of the validator '%s' caused an exception ('%s').",
                        validator.getClass().getName(), e.getLocalizedMessage())));
            }
        }
        if (messages.size() > 0) {
            boolean hasError = false;
            for (final PlatformMessage message : messages) {
                LOGGER.error(VALIDATION_ERROR_LOG_PREFIX + message.getMessage());
                if (message.getType() == PlatformMessage.Type.ERROR) {
                    hasError = true;
                }
            }
            Platform.getRunner().onValidationErrors(messages);
            return !hasError;
        }
        return true;
    }

}
