/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.configuration.bootstrap.LogSystemConfigurator;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Ensures that the actual configuration values are applied.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adapted to new relocation approach)
 */
@Component(service = InstanceValidator.class)
public class LoggingConfiguredProperlyValidator extends DefaultInstanceValidator {

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Logging configuration";

        if (System.getProperty(LogSystemConfigurator.SYSTEM_PROPERTY_KEY_RELOCATION_SUCCESSFUL) != null) {
            return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
        } else {
            return createInstanceValidationResultForFailure(validationDisplayName);
        }
    }

    private InstanceValidationResult createInstanceValidationResultForFailure(String validationDisplayName) {
        // TODO (p2) - this message is obsolete, update it with any remaining failure cases and info - March 2018
        final String errorMessage1 = "Failed to initialize background logging properly."
            + " Most likely, because RCE was started from another directory than its installation directory. "
            + "(The installation directory is the directory that contains the 'rce' executable.)";
        final String errorMessage2 = " It is recommended to start RCE again from its installation directory.";
        return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(validationDisplayName,
            errorMessage1 + " " + errorMessage2, errorMessage1 + "\n\n" + errorMessage2);
    }

}
