/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.exec.OS;

import cucumber.api.java.en.Given;
import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.instancemanagement.internal.DeploymentOperationsImpl;
import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Steps controlling and querying the RCE "instance management", which is used to provision and run test instances using configured RCE
 * installations.
 *
 * @author Robert Mischke
 * @author Lukas Rosenbach
 * @author Marlon Schroeter
 * @author Kathrin Schaffert (minor bugfix in regex #17471)
 */
public class InstanceInstantiationStepDefinitions extends InstanceManagementStepDefinitionBase {

    private final DeploymentOperationsImpl deploymentOperations = new DeploymentOperationsImpl();

    public InstanceInstantiationStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Test step that sets up a single shared installation and one or more instances, while also registering this installation for these
     * instances.
     * 
     * NOTE: The (default) values (e. g. user name, password) are taken from extras.testscriptrunner.definitions.helper.ConnectionOptionConstants
     * 
     * TODO add instance configuration; currently they are only registered
     * 
     * @param keepProfile an optional phrase that prevents the profile to be wiped, resetting only the configuration file
     * @param autoStartPhrase an optional phrase that triggers auto-starting the instances if present
     * @param instanceList comma-separated list of instance names
     * @param buildShort indicates whether the default (current), legacy (previous major release), base (first version of current major
     *        release) or a specific (when null) build shall be used
     * @param buildExplicit the URL part (e.g. "snapshots/trunk") defining the build to use, or a symbolic installation id (e.g. ":self" or
     *        "local:...")
     * @param imMasterRole The SSH role to be used for the IM master account
     */
    @Given("^(?:the )?(same )?(running )?instance[s]? \"([^\"]*)\" using (?:the (default|legacy|base) build|build \"([^\"]*)\")(?: with im master role \"([^\"]*)\")?$")
    public void givenInstancesUsingBuild(String keepProfile, String autoStartPhrase, String instanceList, String buildShort,
        String buildExplicit, String imMasterRole)
            throws Exception {

        final boolean wipeProfile = keepProfile == null;
        final PrefixingTextOutForwarder imOperationOutputReceiver = getTextoutReceiverForIMOperations();
        final String installationId = parseInstallationId(buildShort, buildExplicit, imOperationOutputReceiver);
        TestContext.setTestedInstanceInstallationRoot(installationId);

        final List<String> instanceDefinitionParts = parseCommaSeparatedList(instanceList);
        final List<String> instanceIds = new ArrayList<>();
        for (String instanceDefinition : instanceDefinitionParts) {
            final Matcher matcher = StepDefinitionConstants.INSTANCE_DEFINITION_PATTERN.matcher(instanceDefinition);
            if (!matcher.matches()) {
                fail("Invalid instance definition part: " + instanceDefinition);
            }

            String instanceId = matcher.group(1);
            instanceIds.add(instanceId);

            String optionString = matcher.group(2);
            if (optionString == null) {
                optionString = "";
            }

            final ManagedInstance instance = new ManagedInstance(instanceId, installationId, INSTANCE_MANAGEMENT_SERVICE);
            executionContext.putInstance(instanceId, instance);
            executionContext.addInstance(instance);
            printToCommandConsole(StringUtils.format("Configuring test instance \"%s\"", instanceId));

            int imSshPortNumber = PORT_NUMBER_GENERATOR.incrementAndGet();
            while (!isPortAvailable(imSshPortNumber)) {
                imSshPortNumber = PORT_NUMBER_GENERATOR.incrementAndGet();
            }
            instance.setServerPort(StepDefinitionConstants.CONNECTION_TYPE_SSH, 0, imSshPortNumber);

            configureInstance(imOperationOutputReceiver, instanceId, optionString, imSshPortNumber, wipeProfile, imMasterRole);
        }
        if (autoStartPhrase != null) {
            printToCommandConsole(StringUtils.format("Auto-starting instance(s) \"%s\"", instanceList));
            INSTANCE_MANAGEMENT_SERVICE.startInstance(installationId, instanceIds, imOperationOutputReceiver,
                TimeUnit.SECONDS.toMillis(StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS), false, "");
        }
    }

    private boolean isPortAvailable(int imSshPortNumber) {
        ServerSocket socket = null;
        // Solution for checking port availability inspired by https://stackoverflow.com/a/435579
        try {
            socket = new ServerSocket(imSshPortNumber);
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException innerException) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    private void configureInstance(final PrefixingTextOutForwarder imOperationOutputReceiver, final String instanceId,
        final String optionString, final int imSshPortNumber, final boolean wipe, String imMasterRole)
        throws InstanceConfigurationException, IOException {
        InstanceConfigurationOperationSequence setupSequence = INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence();
        if (wipe) {
            setupSequence = setupSequence.wipeConfiguration();
        } else {
            setupSequence = setupSequence.resetConfiguration();
        }
        if (optionString.contains("Relay")) {
            setupSequence = setupSequence.setRelayFlag(true);
        }
        if (optionString.contains("WorkflowHost") || optionString.contains("WfHost") || optionString.contains("WFHost")) {
            setupSequence = setupSequence.setWorkflowHostFlag(true);
        }
        Matcher idMatcher = StepDefinitionConstants.INSTANCE_DEFINITION_ID_SUBPATTERN.matcher(optionString);
        String customNodeId = null;
        if (idMatcher.find()) {
            customNodeId = idMatcher.group(1);
        }
        if (imMasterRole == null) {
            setupSequence = setupSequence.enableImSshAccessWithDefaultRole(imSshPortNumber);
        } else {
            setupSequence = setupSequence.enableImSshAccessWithRole(imSshPortNumber, imMasterRole);
        }
        setupSequence = setupSequence.setName(instanceId);
        if (customNodeId != null) {
            setupSequence = setupSequence.setCustomNodeId(customNodeId);
        }
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(instanceId, setupSequence, imOperationOutputReceiver);
    }

    // Standard build ids are converted to implicit installation ids by removing all special characters;
    // special build ids are passed on as they are.
    private String parseInstallationId(String buildShort, String buildExplicit, final PrefixingTextOutForwarder imOperationOutputReceiver)
        throws IOException {

        if (buildShort != null) {
            String buildUnderTestId = executionContext.getBuildUnderTestId();
            switch (buildShort) {
            case "default":
                // build under test is supposed to be used
                return parseInstallationId(imOperationOutputReceiver, buildUnderTestId);
            case "legacy":
                // gets the latest version of RCE9. Should be upgraded to get get RCE10, once RCE11 is released.
                return installPreviousBuild(
                    StepDefinitionConstants.LEGACY_VERSION,
                    StepDefinitionConstants.INSTALLATION_SUBFOLDER_NAME_LEGACY,
                    StepDefinitionConstants.LEGACY_URL,
                    imOperationOutputReceiver);
            case "base":
                // gets the earliers release version of RCE10. Should be upgraded to get get RCE11, once RCE11 is released.
                return installPreviousBuild(
                    StepDefinitionConstants.BASE_VERSION,
                    StepDefinitionConstants.INSTALLATION_SUBFOLDER_NAME_BASE,
                    StepDefinitionConstants.BASE_URL,
                    imOperationOutputReceiver);
            default:
                fail(StringUtils.format("%s is not a supported as a shortcut leading to an actual installation id", buildShort));
                return ""; // never reached
            }
        } else {
            return parseInstallationId(imOperationOutputReceiver, buildExplicit);
        }
    }

    private String installPreviousBuild(int version, String installationFolderName, String url,
        final PrefixingTextOutForwarder imOperationOutputReceiver) throws IOException {
        File downloadFile = new File(INSTANCE_MANAGEMENT_SERVICE.getDownloadsCacheDir(),
            StepDefinitionConstants.SLASH + installationFolderName + version + StepDefinitionConstants.ZIP);
        String replacement;
        if (OS.isFamilyWindows()) {
            replacement = StepDefinitionConstants.REPLACEMENT_WINDOWS;
        } else {
            replacement = StepDefinitionConstants.REPLACEMENT_LINUX;
        }
        url = url.replace(StepDefinitionConstants.REPLACEMENT_CHAR, replacement);
        if (!downloadFile.exists()) {
            deploymentOperations.setUserOutputReceiver(imOperationOutputReceiver);
            deploymentOperations.downloadFile(url, downloadFile, true, true,
                (int) TimeUnit.SECONDS.toMillis(StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS));
        }
        File targetDir = new File(INSTANCE_MANAGEMENT_SERVICE.getInstallationsRootDir(),
            installationFolderName + StepDefinitionConstants.SLASH + version);
        if (!targetDir.exists()) {
            deploymentOperations.installFromProductZip(downloadFile, targetDir);
        }
        return installationFolderName + StepDefinitionConstants.SLASH + version;
    }

    private String parseInstallationId(final PrefixingTextOutForwarder imOperationOutputReceiver, String buildOrInstallationId)
        throws IOException {
        if (INSTANCE_MANAGEMENT_SERVICE.isSpecialInstallationId(buildOrInstallationId)) {
            return buildOrInstallationId;
        } else {
            final String installationId = deriveImplicitInstallationIdFromBuildId(buildOrInstallationId);
            printToCommandConsole(
                StringUtils.format("Setting up installation \"%s\" using build \"%s\"", installationId, buildOrInstallationId));
            INSTANCE_MANAGEMENT_SERVICE.setupInstallationFromUrlQualifier(installationId, buildOrInstallationId,
                InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT,
                imOperationOutputReceiver, TimeUnit.SECONDS.toMillis(StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS));
            return installationId;
        }
    }

    private String deriveImplicitInstallationIdFromBuildId(String buildId) {
        return buildId.replaceAll("[^\\w]", "_");
    }
}
