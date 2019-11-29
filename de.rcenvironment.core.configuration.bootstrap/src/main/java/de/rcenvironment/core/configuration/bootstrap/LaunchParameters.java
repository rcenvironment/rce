/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides access to the configuration tokens that are the merged result of command-line arguments and .ini file entries.
 * 
 * @author Robert Mischke
 * @author Tobias Brieden
 * 
 */
public final class LaunchParameters {

    private static final String MESSAGE_CONFLICTING_PARAMETERS =
        "Invalid combination of command-line parameters: cannot specify the same parameter several times";

    /**
     * Standard parameters (followed by a value) that this class should ignore.
     */
    private static final String[] PARAMETER_TOKENS_TO_IGNORE = new String[] { "-launcher", "-name", "-application", "-data",
        "-configuration", "-dev", "-os", "-ws", "-arch", "-nl", "-startup", "-vm", "-exitdata", "-showsplash", "--launcher.XXMaxPermSize",
        "--launcher.library", "--launcher.overrideVmargs" };

    /**
     * Standard flags (without a value) that this class should ignore.
     */
    private static final String[] FLAG_TOKENS_TO_IGNORE = new String[] { "-consoleLog", "-console", "-clean", "-nosplash", "-noSplash" };

    /**
     * The singleton instance.
     */
    private static final LaunchParameters INSTANCE = new LaunchParameters();

    private List<String> tokens;

    private LaunchParameters() {
        readParameters();
    }

    public static LaunchParameters getInstance() {
        return INSTANCE;
    }

    /**
     * During normal operation the eclipse.commands should never change. However, if we execute multiple unit tests the parameters may
     * change between the single test cases. This method can be used to trigger a new evaluation of the supplied parameters.
     */
    public void readParameters() {
        String commandString = System.getProperty("eclipse.commands");
        if (commandString == null) { // typical case: unit tests
            commandString = "";
        }
        String[] tokenArray = commandString.trim().split("\\n");

        List<String> filteredTokens = new ArrayList<String>();

        final List<String> parametersToIgnore = Arrays.asList(PARAMETER_TOKENS_TO_IGNORE);
        final List<String> flagsToIgnore = Arrays.asList(FLAG_TOKENS_TO_IGNORE);

        CommandStack tokenStack = new CommandStack(tokenArray);
        while (tokenStack.hasNext()) {
            String next = tokenStack.getNext();
            if (parametersToIgnore.contains(next)) {
                // these parameters are passed in, but are irrelevant, so ignore them and their value
                // ensure that the next argument is not a parameter to avoid parser confusion on missing values
                if (tokenStack.hasNextIsValue()) {
                    tokenStack.getNext();
                }
            } else if (!flagsToIgnore.contains(next)) {
                filteredTokens.add(next);
            }

        }

        tokens = Collections.unmodifiableList(filteredTokens);
    }

    public List<String> getTokens() {
        return tokens;
    }

    /**
     * Fetches a typical command-line parameter identified by a preceding key token. Technically, this method looks for a token that is
     * equal to the provided string, and returns the following token if it exists. If no matching token exists, null is returned. If there
     * is no following token, null is returned. If the key is present more than once, a {@link ParameterException} is thrown.
     * 
     * Examples:
     * 
     * "a b c" -> getNamedParameter("a") = b
     * 
     * @param shortKey the short parameter key
     * @param longKey the long parameter key
     * @return the token following the given token in the list of command-line arguments
     * @throws ParameterException Thrown if the key is present more than once.
     */
    public String getNamedParameter(String shortKey, String longKey) throws ParameterException {
        boolean lastWasKey = false;
        boolean keyFound = false;
        String resultToken = null;
        for (String token : tokens) {
            if (lastWasKey && !token.startsWith("-")) {
                resultToken = token;
            }
            if (shortKey.equals(token) || longKey.equals(token)) {
                if (keyFound) {
                    throw new ParameterException(MESSAGE_CONFLICTING_PARAMETERS);
                }

                lastWasKey = true;
                keyFound = true;
            } else {
                lastWasKey = false;
            }
        }

        return resultToken;
    }

    /**
     * @param key the token to look for
     * @return true, if the given token exists in the launch parameters
     */
    public boolean containsToken(String key) {
        return tokens.contains(key);
    }

    /**
     * @param shortKey the short version of the token to look for
     * @param longKey the long version of the token to look for
     * @return true, if one of the given token exists in the launch parameters
     */
    public boolean containsToken(String shortKey, String longKey) {
        return tokens.contains(shortKey) || tokens.contains(longKey);
    }

}
