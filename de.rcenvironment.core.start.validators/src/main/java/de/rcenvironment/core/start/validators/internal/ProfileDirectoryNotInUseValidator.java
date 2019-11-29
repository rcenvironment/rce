/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator to prevent accidental use of same instance data directory if multiple RCE instances are running on the same machine. The check
 * is based on a lock file in the data directory of each instance, which is performed by the {@link ConfigurationService} implementation.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
@Component(service = InstanceValidator.class)
public class ProfileDirectoryNotInUseValidator extends DefaultInstanceValidator {

    private static ConfigurationService configService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Profile directory lock";

        if (!configService.isIntendedProfileDirectorySuccessfullyLocked()) {
            String errorMessage = StringUtils.format(Messages.instanceIdAlreadyInUse, configService
                .getOriginalProfileDirectory().getAbsolutePath());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                validationDisplayName, errorMessage, errorMessage);
        }

        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

    @Override
    public List<Class<? extends InstanceValidator>> getNecessaryPredecessors() {
        ArrayList<Class<? extends InstanceValidator>> predecessors = new ArrayList<Class<? extends InstanceValidator>>();
        // we need to make sure that the original profile has the correct version, otherwise this test might be executed first and produce a
        // misleading error message
        predecessors.add(ProfileDirectoryVersionValidator.class);
        return predecessors;
    }
}
