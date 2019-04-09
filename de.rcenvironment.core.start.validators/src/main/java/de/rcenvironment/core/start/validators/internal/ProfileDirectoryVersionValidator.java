/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfile;
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Validator to prevent using a profile that has a version other than the current one. If the profile can be upgraded, ask the user whether
 * they want to do so. Otherwise, abort startup. Uses {@link ConfigurationService} implementation to retrieve the profile's location.
 * 
 * @author Oliver Seebach
 * @author Alexander Weinert (Validation failure with user confirmation)
 */
@Component
public class ProfileDirectoryVersionValidator implements InstanceValidator {

    private static final String PROFILE_VALIDATION_FAILED = "Profile directory version";

    private static ConfigurationService configService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public InstanceValidationResult validate() {

        // TODO this error message is contained in the BootstrapConfiguration class, deduplicate this!
        final CommonProfile commonProfile;
        try {
            final File commonProfileDirectory = ProfileUtils.getProfilesParentDirectory().toPath().resolve("common").toFile();
            commonProfile = new Profile.Builder(commonProfileDirectory).create(false).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            final String errorMessage = String.format("Could not open profile at \"%s\".", configService.getProfileDirectory());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(PROFILE_VALIDATION_FAILED,
                errorMessage);
        }
        final InstanceValidationResult commonProfileResult = validateProfile(commonProfile);
        
        if (!InstanceValidationResultType.PASSED.equals(commonProfileResult.getType())) {
            return commonProfileResult;
        }

        final Profile userProfile;
        try {
            userProfile = new Profile.Builder(configService.getProfileDirectory()).create(false).migrate(false).buildUserProfile();
        } catch (ProfileException e) {
            final String errorMessage = String.format("Could not open profile at \"%s\".", configService.getProfileDirectory());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(PROFILE_VALIDATION_FAILED,
                errorMessage);
        }

        return validateProfile(userProfile);
    }

    private InstanceValidationResult validateProfile(final CommonProfile profile) {
        final boolean profileHasCurrentVersion;
        try {
            profileHasCurrentVersion = profile.hasCurrentVersion();
        } catch (ProfileException e) {
            return onProfileException(profile, e);
        }

        if (!profileHasCurrentVersion) {
            final boolean profileHasUpgradeableVersion;
            try {
                profileHasUpgradeableVersion = profile.hasUpgradeableVersion();
            } catch (ProfileException e) {
                return onProfileException(profile, e);
            }
            if (profileHasUpgradeableVersion) {
                final int profileVersion;
                try {
                    profileVersion = profile.getVersion();
                } catch (ProfileException e) {
                    return onProfileException(profile, e);
                }
                final String queryMessage = String.format(
                    "Your \"%s\" profile is out of date. A version upgrade is required to start RCE. \n"
                        + "Do you wish to upgrade to current version? \n"
                        + "    \n"
                        + "Note: "
                        + "Upgrading causes the profile to be unusable for older versions of RCE. \n"
                        + "You might want to backup the profile folder located at "
                        + "\"%s\".",
                    profile.getName(), profile.getProfileDirectory().getAbsolutePath());
                final String logMessage =
                    String.format("Profile \"%s\" at \"%s\" has outdated version %d, queried user for upgrade confirmation.",
                        profile.getName(), profile.getProfileDirectory().getAbsolutePath(), profileVersion);
                return InstanceValidationResultFactory.createResultForFailureWhichRequiresUserConfirmation(PROFILE_VALIDATION_FAILED,
                    logMessage, queryMessage, () -> {
                        try {
                            profile.upgradeToCurrentVersion();
                        } catch (IOException e) {
                            final String errorMessage =
                                String.format("Could not upgrade profile \"%s\" located at \"%s\" to current version.", profile.getName(),
                                    profile.getProfileDirectory().getAbsolutePath());
                            throw new InstanceValidationResult.CallbackException(errorMessage, e);
                        }
                    });
            } else {
                final int profileVersion;
                try {
                    profileVersion = profile.getVersion();
                } catch (ProfileException e) {
                    return onProfileException(profile, e);
                }
                final String errorMessage = String.format("Profile \"%s\" has version %d, which cannot be upgraded to the current version.",
                    profile.getName(), profileVersion);
                return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(PROFILE_VALIDATION_FAILED,
                    errorMessage);
            }
        }

        // valid profile in use
        return InstanceValidationResultFactory.createResultForPassed("Profile directory has valid version.");
    }

    /**
     * To be called if a profile exception occurs, prints the relevant details to the log and returns an object denoting the failed
     * validation.
     * 
     * @param profile The profile that caused the exception
     * @param exception The exception thrown by the profile
     * @return An {@link InstanceValidationResult} denoting a failed validation which requires shutdown.
     */
    private InstanceValidationResult onProfileException(final CommonProfile profile, ProfileException exception) {
        final String errorMessage = String.format(
            "Could not determine version of profile \"%s\" due to exception. Refer to the log for more details.", profile.getName());
        log.error(errorMessage, exception);
        return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(PROFILE_VALIDATION_FAILED,
            errorMessage, errorMessage);
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

    @Override
    public List<Class<? extends InstanceValidator>> getNecessaryPredecessors() {
        ArrayList<Class<? extends InstanceValidator>> predecessors = new ArrayList<>();
        // we need to make sure that the original profile directory is accessible, since the fallback profile does not contain a version
        // number.
        predecessors.add(ProfileDirectoriesAccessibleValidator.class);
        return predecessors;
    }

}
