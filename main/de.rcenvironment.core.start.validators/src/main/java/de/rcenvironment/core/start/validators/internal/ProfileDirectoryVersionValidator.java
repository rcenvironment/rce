/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Validator to prevent to use a profile that has been used with a newer version of RCE with an older one. Uses {@link ConfigurationService}
 * implementation to retrieve the profile's location.
 * 
 * @author Oliver Seebach
 */
public class ProfileDirectoryVersionValidator implements InstanceValidator {

    private static final String PROFILE_VALIDATION_FAILED = "Profile directory version";

    private static ConfigurationService configService;

    @Override
    public InstanceValidationResult validate() {

        if (!configService.hasIntendedProfileDirectoryValidVersion()) {
            // error and shutdown
            String errorText1 =
                "The required version of the profile directory is " + BootstrapConfiguration.PROFILE_VERSION_NUMBER
                    + " but the profile directory's current version is newer. Most likely reason: It was used with"
                    + " a newer version of RCE before.";
            String errorText2 =
                "As downgrade is not supported, the configured profile directory cannot be used with this RCE version. "
                    + "Use a newer version of RCE or choose another profile directory."
                    + " (See the user guide for more information about the profile directory.)";
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                PROFILE_VALIDATION_FAILED,
                errorText1 + " " + errorText2, errorText1 + "\n\n" + errorText2);
        } else {
            // valid profile in use
            return InstanceValidationResultFactory.createResultForPassed("Profile directory has valid version.");
        }

        // Note: this code is outcommented but might be reused if profile version checking becomes more elaborated;
        // -- seeb_ol, October 2015
        
//        File internalFolder = new File(configService.getProfileDirectory(), PROFILE_INTERNAL_DATA_SUBDIR);
//        if (internalFolder != null && internalFolder.isDirectory()) {
//            File profileVersionNumberFile = new File(internalFolder, BootstrapConfiguration.PROFILE_VERSION_FILE_NAME);
//            if (profileVersionNumberFile.exists() && profileVersionNumberFile.isFile()) {
//                try {
//                    int currentProfilesVersionNumber = Integer.parseInt(FileUtils.readFileToString(profileVersionNumberFile));
//                    if (currentProfilesVersionNumber == BootstrapConfiguration.PROFILE_VERSION_NUMBER) {
//                        // equals
//                        return InstanceValidationResultFactory.createResultForPassed("Current and required profile version are equal.");
//                    } else if (currentProfilesVersionNumber < BootstrapConfiguration.PROFILE_VERSION_NUMBER) {
//                        // warning and go on
//                        String warningText =
//                            "The required profile version is "
//                                + BootstrapConfiguration.PROFILE_VERSION_NUMBER
//                                + " but the profile's current version is "
//                                + currentProfilesVersionNumber
//                                + ". The profile version will be updated. "
//                                + "Note that you cannot use this profile with an older RCE version anymore.";
//                        return InstanceValidationResultFactory.createResultForFailureWhichAllowesToProceed(PROFILE_VALIDATION_FAILED,
//                            warningText, warningText);
//                    } else if (currentProfilesVersionNumber > BootstrapConfiguration.PROFILE_VERSION_NUMBER) {
//                        // error and shutdown
//                        String errorText =
//                            "The required profile version is "
//                                + BootstrapConfiguration.PROFILE_VERSION_NUMBER
//                                + " but the profile's current version is "
//                                + currentProfilesVersionNumber
//                                + ". The profile cannot be used with this RCE version. "
//                                + "A newer version is required.";
//                        return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
//                            PROFILE_VALIDATION_FAILED,
//                            errorText, errorText);
//                    }
//                } catch (IOException | NumberFormatException e) {
//                    return InstanceValidationResultFactory
//                        .createResultForFailureWhichAllowesToProceed(
//                            PROFILE_VALIDATION_FAILED,
//                            "Error reading profile version file "
//                                + profileVersionNumberFile.getAbsolutePath()
//                                + ". A new profile version file will be created. "
//                                + "Please not that you cannot use this profile with an older version of RCE anymore.");
//                }
//            } else {
//                return InstanceValidationResultFactory.createResultForPassed("No profile version file found.");
//            }
//        }
//        return InstanceValidationResultFactory.createResultForPassed("Could not determine profile version.");
    }

    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

    @Override
    public List<Class<? extends InstanceValidator>> getNecessaryPredecessors() {
        ArrayList<Class<? extends InstanceValidator>> predecessors = new ArrayList<Class<? extends InstanceValidator>>();
        // we need to make sure that the original profile directory is accessible, since the fallback profile does not contain a version
        // number.
        predecessors.add(OriginalProfileDirectoryAccessibleValidator.class);
        return predecessors;
    }

}
