/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.bootstrap.CommandStack;

/**
 * Parses and saves the RCE console line arguments.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public final class CommandLineArguments {

    /**
     * The token to activate the headless run mode.
     */
    public static final String HEADLESS_MODE_TOKEN = "--headless";

    /**
     * The token for the "execute these commands" option.
     */
    public static final String EXEC_OPTION_TOKEN = "--exec";

    /**
     * The token for the "disable components" option.
     */
    public static final String DISABLE_COMPONENTS_TOKEN = "--disable-components";

    /**
     * The token for the "execute these commands in headless mode and terminate" option.
     */
    public static final String BATCH_OPTION_TOKEN = "--batch";

    /**
     * The token to activate the configuration shell mode.
     */
    public static final String CONFIGURATION_SHELL_TOKEN = "--configure";

    // this is null until configuration is complete; this serves as a safeguard against accessing undefined settings
    private static volatile CommandLineArguments configuration;

    private boolean showAdvancedTab;

    private boolean headlessModeRequested;

    private boolean batchModeRequested;

    private boolean configurationShellRequested;

    /**
     * @see #getExecCommandTokens()
     */
    private String[] execCommandTokens;

    private boolean doNotStartComponentsRequested;

    private boolean doNotStartNetworkRequested;

    private boolean isValid;

    private CommandLineArguments(String[] input) {
        CommandStack args = new CommandStack(input);
        while (args.hasNext()) {
            String option = args.getNext();
            if (option.equals(HEADLESS_MODE_TOKEN)) {
                setHeadlessModeRequested(true);
            } else if (option.equals(CONFIGURATION_SHELL_TOKEN)) {
                setConfigurationShellRequested(true);
            } else if (option.equals("--wf-run")) {
                assertNoExecCommandYet();
                String wfFile = args.getNext();
                // delegate by creating an equivalent --exec command string
                parseExecCommandTokens("wf run " + wfFile + " ; stop");
            } else if (option.equals(DISABLE_COMPONENTS_TOKEN)) {
                setDoNotStartComponentsRequested(true);
            } else if (option.equals(EXEC_OPTION_TOKEN)) {
                assertNoExecCommandYet();
                String cmdString = args.getNext();
                parseExecCommandTokens(cmdString);
            } else if (option.equals(BATCH_OPTION_TOKEN)) {
                assertNoExecCommandYet();
                String cmdString = args.getNext();
                parseExecCommandTokens(cmdString);
                setBatchModeRequested(true);
            } else if (option.equals("--showAdvancedTab")) {
                setShowAdvancedTab(true);
            } else if (option.equals("-p") || option.equals("--profile")) {
                // parameter is already handled by LaunchParameters class; ignore here
                if (args.hasNextIsValue()) { // there may be a following token
                    args.getNext(); // yes -> discard
                }
            } else {
                getTemporaryLogger().warn(
                    "Ignoring unrecognized command-line argument '" + option + "' at position " + args.lastPos());
            }
        }

        isValid = true;
    }

    /**
     * Parses and saves the RCE console line arguments. If an error is detected, a {@link IllegalArgumentException} is thrown.
     * 
     * @param input the program arguments
     */
    public static void initialize(String[] input) {
        configuration = new CommandLineArguments(input);
        getTemporaryLogger().debug("Parsed command-line options");
    }

    public static boolean isHeadlessModeRequested() {
        return getConfiguration().headlessModeRequested;
    }

    public static boolean isBatchModeRequested() {
        return getConfiguration().batchModeRequested;
    }

    public static boolean isConfigurationShellRequested() {
        return getConfiguration().configurationShellRequested;
    }

    /**
     * Method for various (background) services to check whether they should start running. Currently only disabled when running in text UI
     * configuration mode.
     * 
     * @return true if standard background services (e.g. network, monitoring) should start
     */
    public static boolean isNormalOperationRequested() {
        return !getConfiguration().configurationShellRequested;
    }

    public static boolean isDoNotStartComponentsRequested() {
        return getConfiguration().doNotStartComponentsRequested || getConfiguration().configurationShellRequested;
    }

    public static boolean isDoNotStartNetworkRequested() {
        return getConfiguration().doNotStartNetworkRequested || getConfiguration().configurationShellRequested;
    }

    private static CommandLineArguments getConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("A configuration value was accessed before the configuration was parsed");
        }
        return configuration;
    }

    /**
     * @return the boolean value whether the advanced tab should be shown or not
     */
    public static boolean isShowAdvancedTab() {
        return getConfiguration().showAdvancedTab;
    }

    /**
     * If --exec "&lt;cmdString&gt;" was used, this method returns the individual tokens of "cmdString"; otherwise, this method returns
     * null.
     * 
     * @return the array of tokens, or null if unused
     */
    public static String[] getExecCommandTokens() {
        return getConfiguration().execCommandTokens;
    }

    /**
     * @return true if there was a consistency error in the provided command-line arguments
     */
    public static boolean hasConfigurationErrors() {
        return !getConfiguration().isValid;
    }

    private void setHeadlessModeRequested(boolean value) {
        headlessModeRequested = value;
    }

    private void setConfigurationShellRequested(boolean value) {
        configurationShellRequested = value;
        // set implied other settings
        setHeadlessModeRequested(true);
        setDoNotStartComponentsRequested(true);
        setDoNotStartNetworkRequested(true);
    }

    private void setDoNotStartNetworkRequested(boolean value) {
        doNotStartNetworkRequested = value;
    }

    private void setDoNotStartComponentsRequested(boolean value) {
        doNotStartComponentsRequested = value;
    }

    private void setBatchModeRequested(boolean value) {
        batchModeRequested = value;
        // set implied other settings
        setHeadlessModeRequested(true);
    }

    private void setShowAdvancedTab(boolean value) {
        showAdvancedTab = value;
    }

    private void assertNoExecCommandYet() {
        if (execCommandTokens != null) {
            throw new IllegalArgumentException("Only one of \"--exec\", \"--batch\" or \"--wf-run\" can be used at the same time");
        }
    }

    private void parseExecCommandTokens(String cmdString) {
        execCommandTokens = cmdString.trim().split("\\s+");
    }

    private static Log getTemporaryLogger() {
        return LogFactory.getLog(CommandLineArguments.class);
    }

}
