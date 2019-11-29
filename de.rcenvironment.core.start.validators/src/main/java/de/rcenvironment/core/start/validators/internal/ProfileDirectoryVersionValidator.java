/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.Callback;
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

    private static final String VALIDATOR_DISPLAY_NAME = "Profile directory version validation";

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
            final String errorMessage = String.format(Messages.profileNotAccessibleError, configService.getProfileDirectory());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATOR_DISPLAY_NAME,
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
            final String errorMessage = String.format(Messages.profileNotAccessibleError, configService.getProfileDirectory());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATOR_DISPLAY_NAME,
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
        
        if (profileHasCurrentVersion) {
            return InstanceValidationResultFactory.createResultForPassed(Messages.profileVersionValidationSuccess);
        }

        final boolean profileHasUpgradeableVersion;
        try {
            profileHasUpgradeableVersion = profile.hasUpgradeableVersion();
        } catch (ProfileException e) {
            return onProfileException(profile, e);
        }

        final String profileID = profile.getName();
        final int profileVersion;
        try {
            profileVersion = profile.getVersion();
        } catch (ProfileException e) {
            return onProfileException(profile, e);
        }

        if (profileHasUpgradeableVersion) {
            return buildUpgradeAfterConfirmationResult(profile, profileID, profileVersion);
        } else {
            return buildFailureResult(profileID, profileVersion);
        }
    }

    /**
     * @param profile The profile that is being validated.
     * @param profileID The ID of the profile hat is being validated.
     * @param profileVersion The version of the profile that is being validated.
     * @return An {@link InstanceValidationResult} that denotes that the validated profile does not have the current version, but that it
     *         can be upgraded to the current version after user confirmation.
     */
    private InstanceValidationResult buildUpgradeAfterConfirmationResult(final CommonProfile profile, final String profileID,
        final int profileVersion) {
        final String profileDirectoryPath = profile.getProfileDirectory().getAbsolutePath();

        final String queryMessage = String.format(Messages.profileUpgradeQuery,
            profileID, profileDirectoryPath);
        final String logMessage = String.format(Messages.profileUpgradeLogMessage,
                profileID, profileDirectoryPath, profileVersion);
        final String userHint = String.format(Messages.profileUpgradeNoQueryUserHint,
                profileID, profileDirectoryPath, profileVersion);
        final Callback performUpgrade = () -> {
            try {
                profile.upgradeToCurrentVersion();
            } catch (IOException e) {
                final String errorMessage = String.format(Messages.profileUpgradeTriedAndFailedError,
                        profileID, profileDirectoryPath);
                throw new InstanceValidationResult.CallbackException(errorMessage, e);
            }
        };

        return InstanceValidationResultFactory.createResultForFailureWhichRequiresUserConfirmation(VALIDATOR_DISPLAY_NAME,
            logMessage, queryMessage, userHint, performUpgrade);
    }

    /**
     * @param profileID The ID of the profile that is validated.
     * @param profileVersion The version of the profile that is validated.
     * @return An {@link InstanceValidationResult} that denotes a failed validation of the profile version from which this validator cannot
     *         recover.
     */
    private InstanceValidationResult buildFailureResult(final String profileID, final int profileVersion) {
        final String errorMessage = String.format(Messages.profileUpgradeNotPossibleError,
            profileID, profileVersion);
        return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATOR_DISPLAY_NAME,
            errorMessage);
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
        final String errorMessage = String.format(Messages.profileVersionNotDeterminedError,
            profile.getName());
        log.error(errorMessage, exception);
        return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATOR_DISPLAY_NAME,
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
