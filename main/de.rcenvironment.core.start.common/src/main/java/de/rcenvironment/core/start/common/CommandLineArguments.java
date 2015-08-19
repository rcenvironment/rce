/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.start.common;

import org.apache.commons.logging.LogFactory;

/**
 * Parses and saves the RCE console line arguments.
 * 
 * TODO add unit test -- misc_ro, April 2013
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public final class CommandLineArguments {

    /**
     * The token for the "execute these commands" option.
     */
    public static final String EXEC_OPTION_TOKEN = "--exec";

    /**
     * The token for the "execute these commands in headless mode and terminate" option.
     */
    public static final String BATCH_OPTION_TOKEN = "--batch";

    private static boolean showAdvancedTab;

    private static boolean headlessModeRequested;

    private static boolean batchModeRequested;

    private static String outputPath;

    /**
     * @see #getExecCommandTokens()
     */
    private static String[] execCommandTokens;

    private CommandLineArguments() {}

    /**
     * A simple buffer to read command line arguments from. The purpose of turning this into a class is to avoid scattered
     * "has another argument" checks. With this class, the "getNext()" operation can throw a meaningful exception when another argument is
     * required, but the end of the command line is reached instead.
     * 
     * @author Robert Mischke
     */
    private static class CommandStack {

        private String[] args;

        private int next = 0;

        public CommandStack(String[] args) {
            this.args = args;
        }

        public boolean hasNext() {
            return next < args.length;
        }

        public String getNext() {
            if (!hasNext()) {
                if (args.length == 0) {
                    throw new IllegalStateException("Expected at least one command-line argument");
                } else {
                    throw new IllegalStateException("Expected another command-line segment after " + args[next - 1]);
                }
            }
            return args[next++];
        }

        public int lastPos() {
            return next - 1;
        }
    }

    /**
     * Parses and saves the RCE console line arguments. If an error is detected, a {@link IllegalArgumentException} is thrown.
     * 
     * @param input the program arguments
     */
    public static void parseArguments(String[] input) {
        CommandStack args = new CommandStack(input);
        while (args.hasNext()) {
            String option = args.getNext();
            if (option.equals("--headless")) {
                headlessModeRequested = true;
            } else if (option.equals("--wf-run")) {
                assertNoExecCommandYet();
                String wfFile = args.getNext();
                // delegate by creating an equivalent --exec command string
                parseExecCommandTokens("wf run " + wfFile + " ; stop");
            } else if (option.equals(EXEC_OPTION_TOKEN)) {
                assertNoExecCommandYet();
                String cmdString = args.getNext();
                parseExecCommandTokens(cmdString);
            } else if (option.equals(BATCH_OPTION_TOKEN)) {
                assertNoExecCommandYet();
                String cmdString = args.getNext();
                parseExecCommandTokens(cmdString);
                headlessModeRequested = true;
                batchModeRequested = true;
            } else if (option.equals("--output")) {
                outputPath = args.getNext();
            } else if (option.equals("--launcher.XXMaxPermSize")) {
                // this option is passed in, but it is irrelevant; this "if" case serves to ignore
                // it and its value token to avoid "unrecognized argument" warnings -- misc_ro
                args.getNext();
            } else if (option.equals("--showAdvancedTab")) {
                showAdvancedTab = true;
            } else if (option.equals("-p") || option.equals("--profile")) {
                // parameter is already handled by LaunchParameters class; ignore here
                if (args.hasNext()) { // there should be a following token
                    args.getNext(); // yes -> discard
                } else {
                    LogFactory.getLog(CommandLineArguments.class).warn("Missing expected parameter after -p/--profile option");
                }
            } else {
                // TODO improve?
                LogFactory.getLog(CommandLineArguments.class).warn(
                    "Ignoring unrecognized command-line argument '" + option + "' at position " + args.lastPos());
            }
        }
    }

    private static void assertNoExecCommandYet() {
        if (execCommandTokens != null) {
            throw new IllegalArgumentException("Only one of \"--exec\", \"--batch\" or \"--wf-run\" can be used at the same time");
        }
    }

    private static void parseExecCommandTokens(String cmdString) {
        execCommandTokens = cmdString.trim().split("\\s+");
    }

    public static String getOutputPath() {
        return outputPath;
    }

    public static boolean isHeadlessModeRequested() {
        return headlessModeRequested;
    }

    public static boolean isBatchModeRequested() {
        return batchModeRequested;
    }

    /**
     * @return the boolean value whether the advanced tab should be shown or not
     */
    public static boolean isShowAdvancedTab() {
        return showAdvancedTab;
    }

    /**
     * If --exec "&lt;cmdString&gt;" was used, this method returns the individual tokens of "cmdString"; otherwise, this method returns
     * null.
     * 
     * @return the array of tokens, or null if unused
     */
    public static String[] getExecCommandTokens() {
        return execCommandTokens;
    }

}
