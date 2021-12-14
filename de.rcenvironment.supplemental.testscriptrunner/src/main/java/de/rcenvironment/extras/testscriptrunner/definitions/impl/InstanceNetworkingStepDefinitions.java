/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;
import de.rcenvironment.core.instancemanagement.internal.SSHAccountParameters;
import de.rcenvironment.core.instancemanagement.internal.UplinkConnectionParameters;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.ConnectionOptionConstants;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.RegularConnectionOptions;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.RegularConnectionParameters;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.SSHConnectionOptions;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.UplinkConnectionOptions;

/**
 * Steps for setting up, changing or asserting the network connection(s) between instances.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on code from)
 * @author Matthias Y. Wagner
 */
public class InstanceNetworkingStepDefinitions extends InstanceManagementStepDefinitionBase {

    private static final ManagedInstance[] EMPTY_INSTANCE_ARRAY = new ManagedInstance[0];

    public InstanceNetworkingStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Configures the network connections defined by the given description string.
     * 
     * @param connectionsSetup the comma-separated list of configurations to configure. Has to follow the specific pattern: A-[type]->B
     *        [opt1 opt2], where type specifies which kind of connection shall be configured (reg[ular],ssh, upl[ink]) and a whitespace
     *        separated list consisting of these possible options: autoStart, serverNumber_<number>, role_<role> TODO which options to
     *        support?
     * @param cloneFlag for multiple clients to configure, indicate if they shall have the same "cloned" ID
     */
    @Given("^configured( cloned)? network connection[s]? \"([^\"]*)\"$")
    public void givenConfiguredNetworkConnections(String cloneFlag, String connectionsSetup) throws Exception {
        Boolean cloned = cloneFlag != null;
        printToCommandConsole(StringUtils.format("Configuring network connections (\"%s\", cloned: \"%s\")", connectionsSetup, cloned));
        // parse the string defining the intended network connections
        Pattern p = Pattern.compile("\\s*(\\w+)-(?:\\[(reg|ssh|upl)\\]-)?>(\\w+)\\s*(?:\\[([\\w\\s]*)\\])?\\s*");
        String clonedId = "";
        for (String connectionSetupPart : connectionsSetup.split(",")) {
            Matcher m = p.matcher(connectionSetupPart);
            if (!m.matches()) {
                fail("Syntax error in connection setup part: " + connectionSetupPart);
            }
            final ManagedInstance clientInstance = resolveInstance(m.group(1));

            if (cloned && clonedId.equals("")) {
                clonedId = clientInstance.getId();
            }

            final String connectionType;
            if (m.group(2) == null) {
                connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR; // supporting previous -> connections representing regular
                                                                                  // connections
            } else {
                connectionType = m.group(2);
            }

            final ManagedInstance serverInstance = resolveInstance(m.group(3));
            final String options = m.group(4);
            switch (connectionType) {
            case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
                setUpCompleteRegularConnection(clientInstance, serverInstance, options);
                break;
            case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
                setUpCompleteSSHConnection(clientInstance, serverInstance, options);
                break;
            case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
                setUpCompleteUplinkConnection(clientInstance, clonedId, serverInstance, options);
                break;
            default:
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
            }
        }
    }

    /**
     * TODO consider transforming to given step.
     * 
     * @param serverInstanceId server instance connected to
     * @param connectionType non-null represents a connection type other than regular
     * @param options list of whitespace separated options
     */
    @When("^configuring(?: instance)? \"([^\"]+)\" as a(?: (reg|ssh|upl))? server(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConfiguringInstanceAsServer(String serverInstanceId, String connectionType, String options)
        throws Exception {
        if (connectionType == null) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR;
        }
        final ManagedInstance serverInstance = resolveInstance(serverInstanceId);
        // currently only one option (servernumber) supported for configuring server this way. If this changes, creating a class
        // ServerOptions similar to ConnectionOptions should be considered and handled in a similar way.
        final int serverNumber;
        if (options == null) {
            serverNumber = 0;
        } else {
            serverNumber = Integer.parseInt(options.split(StepDefinitionConstants.OPTION_SEPARATOR)[1]);
        }
        switch (connectionType) {
        case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
            configureServer(serverInstance, serverNumber);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
        case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
            configureSSHServer(serverInstance, serverNumber);
            break;
        default:
            fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
        }
    }

    /**
     * 
     * @param disabledFlag non-null value if ssh account is to be initialized as disabled
     * @param userRole user role of added ssh account
     * @param userName user name of added ssh account
     * @param password password of added ssh account
     * @param serverInstanceId instance where ssh account is added to
     */
    @When("^adding( disabled)? ssh account with user role \"([^\"]+)\", user name \"([^\"]+)\" and password \"([^\"]+)\""
        + " to(?: (?:instance|server))? \"([^\"]+)\"$")
    public void whenAddingSSHAccount(String disabledFlag, String userRole, String userName, String password, String serverInstanceId)
        throws Exception {
        boolean enabled = disabledFlag == null;
        final SSHAccountParameters accountParameters = SSHAccountParameters.builder()
            .userRole(userRole)
            .userName(userName)
            .password(password)
            .isEnabled(enabled)
            .build();
        addSSHAccount(resolveInstance(serverInstanceId), accountParameters);
    }

    /**
     * 
     * @param clientInstanceId client instance
     * @param serverInstanceId server instance, needs to be configured beforehand
     * @param connectionType type of connection used for connecting. Viable is reg[ular] ssh and upl[ink]. Omitting this leads to a regular
     *        connection
     * @param options list of whitespace separated options
     */
    @When("^connecting(?: instance)? \"([^\"]+)\" to(?: (?:instance|server))? \"([^\"]+)\"(?: via(reg|ssh|upl))?"
        + "(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConnectingInstances(String clientInstanceId, String serverInstanceId, String connectionType,
        String options) throws Exception {
        if (connectionType == null) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR;
        }
        ManagedInstance clientInstance = resolveInstance(clientInstanceId);
        ManagedInstance serverInstance = resolveInstance(serverInstanceId);
        switch (connectionType) {
        case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
            setupPartialRegularConnection(clientInstance, serverInstance, options);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
            setUpPartialSSHConnection(clientInstance, serverInstance, options);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
            setUpPartialUplinkConnection(clientInstance, serverInstance, options);
            break;
        default:
            fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
        }
    }

    /**
     * Checks each of the connections configured using {@link #configuredNetworkConnections()} until it is in the CONNECTED state, or the
     * given timeout is reached.
     * 
     * @param maxWaitTimeSeconds the maximum time to wait for connections to be established
     */
    @Then("^all auto-start network connections should be ready within (\\d+) seconds$")
    public void thenAutoStartConnectionsReadyIn(int maxWaitTimeSeconds) throws Exception {
        printToCommandConsole("Waiting for all auto-start network connections to complete");

        final Set<ManagedInstance> pendingInstances = new HashSet<>();
        for (ManagedInstance instance : executionContext.getEnabledInstances()) {
            if (!instance.accessConfiguredAutostartConnectionIds().isEmpty()) {
                pendingInstances.add(instance);
            }
        }

        final long maximumTimestampForStandardAttempts = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitTimeSeconds);

        // printToCommandConsole("Waiting for " + maxWaitTimeSeconds + " seconds(s)...");
        while (!pendingInstances.isEmpty() && System.currentTimeMillis() <= maximumTimestampForStandardAttempts) {
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, false);
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
            if (!pendingInstances.isEmpty()) {
                Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
            }
        }

        if (!pendingInstances.isEmpty()) {
            // at least one instance was not successfully validated yet; make one final attempt and log any failures
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, true); // true = print failure info
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
        }

        if (!pendingInstances.isEmpty()) {
            fail("On " + pendingInstances.size() + " instance(s), the configured outgoing connections "
                + "were not established after waiting/retrying for " + maxWaitTimeSeconds + " second(s)");
        }
    }

    /**
     * Verifies that a given set of instances is present in a given instance's visible network.
     * 
     * @param instanceId the node that should have its visible network inspected
     * @param testType whether to test for "should contain" or "should consist of" the given list of instances
     * @param listOfExpectedVisibleInstances the comma-separated list of expected instances
     */
    @Then("^the visible network of \"([^\"]*)\" should (consist of|contain) \"([^\"]*)\"$")
    public void thenVisibleNetworkConsistsOf(String instanceId, String testType, String listOfExpectedVisibleInstances) {

        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "net info", false);

        List<String> expectedVisibleInstances = parseCommaSeparatedList(listOfExpectedVisibleInstances);
        switch (testType) {
        case "contain":
            for (String expectedInstanceId : expectedVisibleInstances) {
                if (!commandOutput.contains(expectedInstanceId)) {
                    fail("The visible network of instance " + instanceId + " does not contain the expected instance " + expectedInstanceId);
                }
            }
            break;
        case "consist of":
            // check if all instances in output are expected
            for (String line : commandOutput.split(StepDefinitionConstants.LINEBREAK_REGEX)) {
                if (line.contains("Reachable network nodes")) {
                    continue; // line indicating list of reachable nodes follows. Must not be checked.
                }
                if (line.contains("Message channels")) {
                    break; // line indicating list of reachable nodes is finished. It and following lines must not be checked.
                }
                if (!isExpectedVisibleInstance(line, expectedVisibleInstances)) {
                    fail(StringUtils.format("Instance %s could see unexpected instances: \n %s", instanceId, line));
                }
            }
            // check if all instances expected are present in output
            thenVisibleNetworkConsistsOf(instanceId, "contain", listOfExpectedVisibleInstances);
            break;
        default:
            fail(StringUtils.format("Test type %s is not supported.", testType));
        }
        printToCommandConsole("Verified the visible network of instance \"" + instanceId + "\"");
    }

    /**
     * Verifies that a given set of instances is present in a given instance's visible network.
     * 
     * @param instanceId the node that should have its visible uplink network inspected
     * @param testType if the specified node is currently connected
     * @param listOfExpectedUplinkInstances the comma-separated list of expected instances (i. e. uplink servers)
     */
    @Then("^the visible uplink network of \"([^\"]*)\" should (contain|not contain|be connected to|not be connected to) \"([^\"]*)\"$")
    public void thenUplinkNetworkConsistsOf(String instanceId, String testType, String listOfExpectedUplinkInstances) {

        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "uplink list", false);
        List<String> expectedUplinkInstances = parseCommaSeparatedList(listOfExpectedUplinkInstances);
        /**
         * NOTE THAT: From the test case we get only the "simple" name of an instance (e. g. "Uplink"). To be able to check the availability
         * of instances, we need the "Id" of instance, as it is constructed in the test steps which are "using the default build" which are
         * used for the "configures network connections" (e. g. "Uplink_userName"). So we build here a new List where the Ids are
         * constructed as in the described test steps.
         * 
         * !If in the initial steps we do not use the defaults as described, this will not work! mwag: This could be a major TODO to provide
         * easy access to these default values from everywhere
         * 
         * @see InstanceInstantiationStepDefinitions.givenInstancesUsingBuild()
         * @see givenConfiguredNetworkConnections()
         */
        ArrayList<String> expectedUplinkInstanceIds = new ArrayList<String>();
        for (String instance : expectedUplinkInstances) {
            String formattedInstance =
                StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, instance, ConnectionOptionConstants.USER_NAME_DEFAULT);
            expectedUplinkInstanceIds.add(formattedInstance);
        }

        String[] outputLines = commandOutput.split(StepDefinitionConstants.LINEBREAK_REGEX);
        // process the console output into a easier processable map
        Map<String, Boolean> uplinkInstances = getUplinkInstances(outputLines);

        switch (testType) {
        case "be connected to":
            for (String expectedInstanceId : expectedUplinkInstanceIds) {
                if (!uplinkInstances.getOrDefault(expectedInstanceId, false)) {
                    fail(StringUtils.format("Instance %s is not connected to %s.", instanceId, expectedInstanceId));
                }
            }
            break;
        case "not be connected to":
            for (String expectedInstanceId : expectedUplinkInstanceIds) {
                if (uplinkInstances.getOrDefault(expectedInstanceId, false)) {
                    fail(StringUtils.format("Instance %s should not be connected to %s.", instanceId, expectedInstanceId));
                }
            }
            break;
        case "contain":
            for (String expectedInstanceId : expectedUplinkInstanceIds) {
                if (!uplinkInstances.containsKey(expectedInstanceId)) {
                    fail(StringUtils.format("Uplink server %s is not visible for instance %s", expectedInstanceId, instanceId));
                }
            }
            break;
        case "not contain":
            for (String expectedInstanceId : expectedUplinkInstanceIds) {
                if (uplinkInstances.containsKey(expectedInstanceId)) {
                    fail(StringUtils.format("Uplink server %s is not expected to be visible from instance %s.", expectedInstanceId,
                        instanceId));
                }
            }
            break;

        default:
            fail(StringUtils.format("Test type %s is not supported.", testType));
        }

        printToCommandConsole("Verified network of instance \"" + instanceId + "\" to " + testType + " " + listOfExpectedUplinkInstances);
    }

    /**
     * Performs an ungrateful shutdown of an instance.
     * 
     * @param instanceId the node that is to be shut down.
     */
    @When("^instance \"([^\"]*)\" crashes$")
    public void thenUplinkNetworkConsistsOf(String instanceId) {

        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "force-crash 0", false);
        printToCommandConsole(commandOutput);

        printToCommandConsole("Hard shutdown of instance \"" + instanceId);
    }

    /**
     * Extracts the uplink instances from the output of the console command "uplink list".
     * 
     * @param outputLines the command output split in lines
     * @return a Map with the uplink instances and their respective state of connected (true/false).
     */
    private Map<String, Boolean> getUplinkInstances(String[] outputLines) {
        Map<String, Boolean> uplinkMap = new HashMap<String, Boolean>();
        String foundInstances = "";
        for (String line : outputLines) {
            if (line.startsWith("Finished executing command") || line.length() == 0) {
                // empty line or synthetic final line; not part of the actual output
                continue;
            }
            int positionOfId = line.indexOf("(id:");
            final int minusOne = -1;
            if (positionOfId == minusOne) {
                fail(StringUtils.format("Unexpected result from command \"uplink list: \n %s", line));
            } else {
                String foundInstanceId = line.substring(positionOfId + 5, line.indexOf(")", positionOfId));
                // Alternatively we may want to use the format as when it is originally created:
                // foundInstanceId = StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, foundInstanceId,
                // ConnectionOptionConstants.USER_NAME_DEFAULT);
                if (!foundInstances.equals("")) {
                    foundInstances += ", ";
                }
                foundInstances += foundInstanceId;

                if (line.contains("CONNECTED: true")) {
                    uplinkMap.put(foundInstanceId, true);
                } else if (line.contains("CONNECTED: false")) {
                    uplinkMap.put(foundInstanceId, false);
                } else {
                    printToCommandConsole(
                        StringUtils.format("Line of output from command \\\"uplink list: \\n %s has no connected status", line));
                }
            }
            // TODO sind 4 ebenen zuviel?
        }
        printToCommandConsole("Found instance(s) " + foundInstances);
        return uplinkMap;
    }

    private boolean isExpectedVisibleInstance(String line, List<String> expectedVisibleInstances) {
        for (String expectedInstanceId : expectedVisibleInstances) {
            if (line.contains(expectedInstanceId)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> extractValueFromOption(String option) {
        String[] keyValuePair = option.split(StepDefinitionConstants.OPTION_SEPARATOR);
        switch (keyValuePair.length) {
        case (1):
            return Optional.empty();
        case (2):
            return Optional.of(keyValuePair[1]);
        default:
            fail(StringUtils.format("Option %s contains multiple %s", option, StepDefinitionConstants.OPTION_SEPARATOR));
            return null; // never reaches, since fail breaks
        }
    }

    private RegularConnectionOptions parseRegularConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return RegularConnectionOptions.builder().build();
        }
        RegularConnectionOptions.Builder builder = RegularConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.AUTO_START_FLAG):
                    builder.autoStart(true);
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_INITIAL_DELAY):
                    builder.autoRetryInitialDelay(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_MAX_DELAY):
                    builder.autoRetryMaxDelay(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_DELAY_MULTIPLIER):
                    builder.autoRetryDelayMultiplier(Float.parseFloat(value.get()));
                    break;
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.RELAY):
                    builder.relay(true);
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a regular connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    private SSHConnectionOptions parseSSHConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return SSHConnectionOptions.builder().build();
        }
        SSHConnectionOptions.Builder builder = SSHConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.DISPLAY_NAME):
                    builder.displayName(value.get());
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.USER_NAME):
                    builder.userName(value.get());
                    break;
                case (ConnectionOptionConstants.USER_ROLE):
                    builder.userRole(value.get());
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a SSH connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    private UplinkConnectionOptions parseUplinkConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return UplinkConnectionOptions.builder().build();
        }
        UplinkConnectionOptions.Builder builder = UplinkConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.AUTO_RETRY_FLAG):
                    builder.autoRetry(true);
                    break;
                case (ConnectionOptionConstants.AUTO_START_FLAG):
                    builder.autoStart(true);
                    break;
                case (ConnectionOptionConstants.CLIENT_ID):
                    builder.clientId(value.get());
                    break;
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.GATEWAY_FLAG):
                    builder.gateway(true);
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PASSWORD):
                    builder.password(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.USER_NAME):
                    builder.userName(value.get());
                    break;
                case (ConnectionOptionConstants.USER_ROLE):
                    builder.userRole(value.get());
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a uplink connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    // TODO rework naming scheme with regular and partial setup
    private void setUpCompleteRegularConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        RegularConnectionOptions connectionOptions = parseRegularConnectionOptions(options);
        final Integer serverPort = configureServer(serverInstance, connectionOptions.getServerNumber());
        if (connectionOptions.isRelay()) {
            configureRelay(serverInstance, true);
        }
        final RegularConnectionParameters connectionParameters = RegularConnectionParameters.builder()
            .connectionId(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), serverPort))
            .host(ConnectionOptionConstants.HOST_DEFAULT)
            .port(serverPort)
            .autoStartFlag(connectionOptions.getAutoStartFlag())
            .autoRetryInitDelay(connectionOptions.getAutoRetryInitialDelay())
            .autoRetryMaxDelay(connectionOptions.getAutoRetryMaxDelay())
            .autoRetryDelayMultiplier(connectionOptions.getAutoRetryDelayMultiplier())
            .build();
        configureRegularConnection(clientInstance, connectionParameters);
    }

    private void setupPartialRegularConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        RegularConnectionOptions connectionOptions = parseRegularConnectionOptions(options);
        final RegularConnectionParameters connectionParameters = RegularConnectionParameters.builder()
            .connectionId(connectionOptions.getConnectionName()
                .orElse(
                    StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), connectionOptions.getPort())))
            .host(connectionOptions.getHost())
            .port(connectionOptions.getPort()
                .orElse(
                    getServerPort(serverInstance, connectionOptions.getServerNumber(), StepDefinitionConstants.CONNECTION_TYPE_REGULAR)))
            .autoStartFlag(connectionOptions.getAutoStartFlag())
            .autoRetryInitDelay(connectionOptions.getAutoRetryInitialDelay())
            .autoRetryMaxDelay(connectionOptions.getAutoRetryMaxDelay())
            .autoRetryDelayMultiplier(connectionOptions.getAutoRetryDelayMultiplier())
            .build();
        configureRegularConnection(clientInstance, connectionParameters);
    }

    private void setUpCompleteSSHConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        SSHConnectionOptions connectionOptions = parseSSHConnectionOptions(options);
        final Integer serverPort = configureSSHServer(serverInstance, connectionOptions.getServerNumber());
        final SSHAccountParameters accountParametersSSH = SSHAccountParameters.builder()
            .userRole(connectionOptions.getUserRole().orElse("ra_demo"))
            .userName(connectionOptions.getUserName())
            .password(connectionOptions.getUserName())
            .isEnabled(true)
            .build();
        addSSHAccount(serverInstance, accountParametersSSH);
        configureSSHConnection(clientInstance,
            StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), connectionOptions.getPort()),
            clientInstance.getId(), ConnectionOptionConstants.HOST_DEFAULT, serverPort, connectionOptions.getUserName());
    }

    private void setUpPartialSSHConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        SSHConnectionOptions connectionOptions = parseSSHConnectionOptions(options);
        configureSSHConnection(clientInstance,
            connectionOptions.getConnectionName()
                .orElse(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    connectionOptions.getUserName())),
            connectionOptions.getDisplayName()
                .orElse(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    connectionOptions.getUserName())),
            connectionOptions.getHost(),
            connectionOptions.getPort().orElse(
                getServerPort(serverInstance, connectionOptions.getServerNumber(), StepDefinitionConstants.CONNECTION_TYPE_REGULAR)),
            connectionOptions.getUserName());
    }

    /**
     * Sets up the uplink connections with the same "cloned" ID for all clientInstances. The ID is optionally cloned from the first client
     * in the array clientInstances and used for all instances.
     * 
     * @param clientInstance the client instances to be connected to the uplink server
     * @param commonClientId the client ID if the ID of the client instances should be the same for all ("cloned"), otherwise ""
     * @param serverInstance
     * @param options
     */
    private void setUpCompleteUplinkConnection(ManagedInstance clientInstance, String commonClientId, ManagedInstance serverInstance,
        String options)
        throws Exception {
        UplinkConnectionOptions connectionOptions = parseUplinkConnectionOptions(options);
        final Integer serverPort = configureSSHServer(serverInstance, connectionOptions.getServerNumber());
        final SSHAccountParameters accountParametersUpl = SSHAccountParameters.builder()
            .userRole(connectionOptions.getUserRole().orElse("uplink_client"))
            .userName(connectionOptions.getUserName())
            .password(connectionOptions.getUserName())
            .isEnabled(true)
            .build();
        addSSHAccount(serverInstance, accountParametersUpl);

        final String clientId;
        if (commonClientId.equals("")) {
            clientId = clientInstance.getId();
        } else {
            clientId = commonClientId;
        }

        final UplinkConnectionParameters uplinkParameters = UplinkConnectionParameters.builder()
            .connectionId(
                StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    connectionOptions.getUserName()))
            .host(ConnectionOptionConstants.HOST_DEFAULT)
            .port(serverPort)
            .clientId(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, clientId, serverInstance.getId()))
            .gateway(connectionOptions.getGateway())
            .autoStart(connectionOptions.getAutoStart())
            .autoRetry(connectionOptions.getAutoRetry())
            .userName(connectionOptions.getUserName())
            .password(connectionOptions.getUserName())
            .build();
        configureUplinkConnection(clientInstance, uplinkParameters);
    }

    private void setUpPartialUplinkConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        UplinkConnectionOptions connectionOptions = parseUplinkConnectionOptions(options);
        final UplinkConnectionParameters uplinkParameters = UplinkConnectionParameters.builder()
            .autoRetry(connectionOptions.getAutoRetry())
            .autoStart(connectionOptions.getAutoStart())
            .clientId(connectionOptions.getClientId())
            .connectionId(connectionOptions.getConnectionName()
                .orElse(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    connectionOptions.getUserName())))
            .gateway(connectionOptions.getGateway())
            .host(connectionOptions.getHost())
            .password(connectionOptions.getPassword())
            .port(connectionOptions.getPort()
                .orElse(
                    getServerPort(serverInstance, connectionOptions.getServerNumber(), StepDefinitionConstants.CONNECTION_TYPE_REGULAR)))
            .userName(connectionOptions.getUserName())
            .build();
        configureUplinkConnection(clientInstance, uplinkParameters);
    }

    private void configureRegularConnection(final ManagedInstance clientInstance, final RegularConnectionParameters parameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(
            clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addNetworkConnection(
                parameters.getConnectionId(), parameters.getHost(), parameters.getPort(), parameters.isAutoStart(),
                parameters.getAutoRetryInitDelay(), parameters.getAutoRetryMaxDelay(), parameters.getAutoRetryDelayMultiplier()),
            getTextoutReceiverForIMOperations());
        if (parameters.isAutoStart()) {
            // note: as of release 8.1.0 and before, "cn list" does not output the connection id provided via IM configuration, but
            // "ip:port" for each connection; so this is the string needed to detect the connection's state from the output --
            // misc_ro
            clientInstance.accessConfiguredAutostartConnectionIds()
                .add(StringUtils.format("%s:%d", parameters.getHost(), parameters.getPort()));
        }
    }

    private void configureRelay(ManagedInstance serverInstance, boolean isRelay) throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(
            serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().setRelayFlag(isRelay),
            getTextoutReceiverForIMOperations());
    }

    private void configureSSHConnection(final ManagedInstance clientInstance, final String connectionId, final String displayName,
        final String host, final int port, final String loginName) throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addSshConnection(connectionId, displayName, host, port,
                loginName),
            getTextoutReceiverForIMOperations());

    }

    private void configureUplinkConnection(final ManagedInstance clientInstance, final UplinkConnectionParameters uplinkParameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addUplinkConnection(uplinkParameters),
            getTextoutReceiverForIMOperations());
    }

    private Integer configureServer(final ManagedInstance serverInstance, final int serverNumber)
        throws InstanceConfigurationException, IOException {
        Integer serverPort = getServerPort(serverInstance, serverNumber, StepDefinitionConstants.CONNECTION_TYPE_REGULAR);
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence()
                .addServerPort(
                    StringUtils.format(StepDefinitionConstants.HOST_PORT_FORMAT, ConnectionOptionConstants.HOST_DEFAULT, serverPort),
                    ConnectionOptionConstants.HOST_DEFAULT, serverPort),
            getTextoutReceiverForIMOperations());
        return serverPort;
    }

    private Integer configureSSHServer(final ManagedInstance serverInstance, final int serverNumber) throws Exception {
        Integer serverPort = getServerPort(serverInstance, serverNumber, StepDefinitionConstants.CONNECTION_TYPE_SSH);
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().enableSshServer(ConnectionOptionConstants.HOST_DEFAULT,
                serverPort),
            getTextoutReceiverForIMOperations());
        return serverPort;
    }

    private void addSSHAccount(final ManagedInstance serverInstance, final SSHAccountParameters parameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addSshAccount(parameters),
            getTextoutReceiverForIMOperations());
    }

    private Integer getServerPort(final ManagedInstance instance, final int serverNumber, String connectionType) {
        if (connectionType.equals(StepDefinitionConstants.CONNECTION_TYPE_UPLINK)) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_SSH;
        }
        Integer serverPort = instance.getServerPort(connectionType, serverNumber);
        if (serverPort == null) {
            serverPort = PORT_NUMBER_GENERATOR.incrementAndGet();
            while (!isPortAvailable(serverPort)) {
                serverPort = PORT_NUMBER_GENERATOR.incrementAndGet();
            }
            instance.setServerPort(connectionType, serverNumber, serverPort);
        }
        return serverPort;
    }

    private boolean isPortAvailable(int portNumber) {
        ServerSocket socket = null;
        // Solution for checking port availability inspired by https://stackoverflow.com/a/435579
        try {
            socket = new ServerSocket(portNumber);
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

    private boolean testIfConfiguredOutgoingConnectionsAreConnected(final ManagedInstance instance, boolean isFinalAttempt) {
        final List<String> connectionIds = instance.accessConfiguredAutostartConnectionIds();

        String commandOutput = executeCommandOnInstance(instance, "cn list", false);
        int matches = 0;
        for (String connectionId : connectionIds) {
            Matcher matcher = Pattern.compile("'" + connectionId + "'.*?- (\\w+)").matcher(commandOutput);
            if (!matcher.find()) {
                if (isFinalAttempt) {
                    fail(StringUtils.format(
                        "Unexpected state: Attempted to verify the state of connection \"%s\" on \"%s\", "
                            + "but it did not appear in the output of \"cn list\" at all; full command output:\n%s",
                        connectionId, instance, commandOutput));
                } else {
                    continue; // most likely, the tested instance has simply not registered the connection(s) yet, so just retry
                }
            }
            String state = matcher.group(1);
            if (matcher.find()) {
                fail(StringUtils.format(
                    "Unexpected state: Found more than one entry for connection \"%s\" on \"%s\" "
                        + "in the output of \"cn list\"; full command output:\n%s",
                    connectionId, instance, commandOutput));
            }
            if ("CONNECTED".equals(state)) {
                matches++;
            } else {
                if (isFinalAttempt) {
                    printToCommandConsole(StringUtils.format(
                        "Failed expectation: the connection \"%s\" on \"%s\" should be in state \"CONNECTED\", but is \"%s\"",
                        connectionId, instance, state));
                }
            }
        }
        boolean success = matches == connectionIds.size();
        return success;
    }

    private ManagedInstance[] detachedIterableCopy(final Collection<ManagedInstance> pendingInstances) {
        return pendingInstances.toArray(EMPTY_INSTANCE_ARRAY);
    }

    private String extractKeyFromOption(String option) {
        return option.split(StepDefinitionConstants.OPTION_SEPARATOR)[0];
    }

}
