/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.validators.internal;

import java.util.Collection;
import java.util.LinkedList;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator to prevent accidental use of same instance data directory if multiple RCE instances are running on the same machine. The check
 * is based on a lock file in the data directory of each instance, which is performed by the {@link ConfigurationService} implementation.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
public class InstanceDataDirectoryAcquiredValidator implements PlatformValidator {

    private static ConfigurationService configService;

    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

    @Override
    public Collection<PlatformMessage> validatePlatform() {
        final Collection<PlatformMessage> result = new LinkedList<PlatformMessage>();

        if (!configService.isUsingIntendedProfileDirectory()) {
            result.add(new PlatformMessage(PlatformMessage.Type.ERROR,
                ValidatorsBundleActivator.bundleSymbolicName, StringUtils.format(Messages.instanceIdAlreadyInUse, configService
                    .getOriginalProfileDirectory().getAbsolutePath())));
        }

        return result;
    }
}
