/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
     * @throws Throwable on failure
     */
    @Given("^configured network connection[s]? \"([^\"]*)\"$")
    public void givenConfiguredNetworkConnections(String connectionsSetup) throws Throwable {
        printToCommandConsole(StringUtils.format("Configuring network connections (\"%s\")", connectionsSetup));
        // parse the string defining the intended network connections
        Pattern p = Pattern.compile("\\s*(\\w+)-(?:\\[(reg|ssh|upl)\\]-)?>(\\w+)\\s*(?:\\[([\\w\\s]*)\\])?\\s*");
        for (String connectionSetupPart : connectionsSetup.split(",")) {
            Matcher m = p.matcher(connectionSetupPart);
            if (!m.matches()) {
                fail("Syntax error in connection setup part: " + connectionSetupPart);
            }
            final ManagedInstance clientInstance = resolveInstance(m.group(1));
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
                setUpCompleteUplinkConnection(clientInstance, serverInstance, options);
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
     * @throws Throwable on failure
     */
    @When("^configuring(?: instance)? \"([^\"]+)\" as a(?: (reg|ssh|upl))? server(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConfiguringInstanceAsServer(String serverInstanceId, String connectionType, String options)
        throws Throwable {
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
     * @throws Throwable on failure
     */
    @When("^adding( disabled)? ssh account with user role \"([^\"]+)\", user name \"([^\"]+)\" and password \"([^\"]+)\""
        + " to(?: (?:instance|server))? \"([^\"]+)\"$")
    public void whenAddingSSHAccount(String disabledFlag, String userRole, String userName, String password, String serverInstanceId)
        throws Throwable {
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
     * @throws Throwable on failure
     */
    @When("^connecting(?: instance)? \"([^\"]+)\" to(?: (?:instance|server))? \"([^\"]+)\"(?: via(reg|ssh|upl))?"
        + "(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConnectingInstances(String clientInstanceId, String serverInstanceId, String connectionType,
        String options) throws Throwable {
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
     * @throws Throwable on failure
     */
    @Then("^all auto-start network connections should be ready within (\\d+) seconds$")
    public void thenAutoStartConnectionsReadyIn(int maxWaitTimeSeconds) throws Throwable {
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
     * @throws Throwable on failure
     */
    @Then("^the visible network of \"([^\"]*)\" should (consist of|contain) \"([^\"]*)\"$")
    public void thenVisibleNetworkConsistOf(String instanceId, String testType, String listOfExpectedVisibleInstances)
        throws Throwable {

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
            //check if all instances in output are expected
            for (String line : commandOutput.split(StepDefinitionConstants.LINEBREAK_REGEX)) {
                if (line.contains("Reachable network nodes")) {
                    continue; //line indicating list of rechable nodes follows. Must not be checked.
                }
                if (line.contains("Message channels")) {
                    break; //line indicating list of reachable nodes is finished. It and following lines must not be checked.
                }
                if (!isExpectedVisibleInstance(line, expectedVisibleInstances)) {
                    fail(StringUtils.format("Instance %s could see unexpected instances: \n %s", instanceId, line));
                }
            }
            //check if all instances expected are present in output
            thenVisibleNetworkConsistOf(instanceId, "contain", listOfExpectedVisibleInstances);
            break;
        default:
            fail(StringUtils.format("Test type %s is not supported.", testType));
        }
        printToCommandConsole("Verified the visible network of instance \"" + instanceId + "\"");
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
        throws Throwable {
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
        throws Throwable {
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
        throws Throwable {
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
        throws Throwable {
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

    private void setUpCompleteUplinkConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Throwable {
        UplinkConnectionOptions connectionOptions = parseUplinkConnectionOptions(options);
        final Integer serverPort = configureSSHServer(serverInstance, connectionOptions.getServerNumber());
        final SSHAccountParameters accountParametersUpl = SSHAccountParameters.builder()
            .userRole(connectionOptions.getUserRole().orElse("uplink_client"))
            .userName(connectionOptions.getUserName())
            .password(connectionOptions.getUserName())
            .isEnabled(true)
            .build();
        addSSHAccount(serverInstance, accountParametersUpl);

        final UplinkConnectionParameters uplinkParameters = UplinkConnectionParameters.builder()
            .connectionId(
                StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), connectionOptions.getUserName()))
            .host(ConnectionOptionConstants.HOST_DEFAULT)
            .port(serverPort)
            .clientId(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, clientInstance.getId(), serverInstance.getId()))
            .gateway(connectionOptions.getGateway())
            .autoStart(connectionOptions.getAutoStart())
            .autoRetry(connectionOptions.getAutoRetry())
            .userName(connectionOptions.getUserName())
            .password(connectionOptions.getUserName())
            .build();
        configureUplinkConnection(clientInstance, uplinkParameters);
    }

    private void setUpPartialUplinkConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Throwable {
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
        throws Throwable {
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

    private void configureRelay(ManagedInstance serverInstance, boolean isRelay) throws Throwable {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(
            serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().setRelayFlag(isRelay),
            getTextoutReceiverForIMOperations());
    }

    private void configureSSHConnection(final ManagedInstance clientInstance, final String connectionId, final String displayName,
        final String host, final int port, final String loginName) throws Throwable {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addSshConnection(connectionId, displayName, host, port,
                loginName),
            getTextoutReceiverForIMOperations());

    }

    private void configureUplinkConnection(final ManagedInstance clientInstance, final UplinkConnectionParameters uplinkParameters)
        throws Throwable {
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

    private Integer configureSSHServer(final ManagedInstance serverInstance, final int serverNumber) throws Throwable {
        Integer serverPort = getServerPort(serverInstance, serverNumber, StepDefinitionConstants.CONNECTION_TYPE_SSH);
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().enableSshServer(ConnectionOptionConstants.HOST_DEFAULT,
                serverPort),
            getTextoutReceiverForIMOperations());
        return serverPort;
    }

    private void addSSHAccount(final ManagedInstance serverInstance, final SSHAccountParameters parameters)
        throws Throwable {
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
            instance.setServerPort(connectionType, serverNumber, serverPort);
        }
        return serverPort;
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
