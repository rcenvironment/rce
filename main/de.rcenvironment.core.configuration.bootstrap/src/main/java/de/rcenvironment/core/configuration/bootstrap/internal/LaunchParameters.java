/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides access to the configuration tokens that are the merged result of command-line arguments and .ini file entries.
 * 
 * @author Robert Mischke
 */
public final class LaunchParameters {

    private final List<String> tokens;

    public LaunchParameters() {
        String commandString = System.getProperty("eclipse.commands");
        if (commandString == null) { // typical case: unit tests
            commandString = "";
        }
        String[] tokenArray = commandString.trim().split("\\n");
        tokens = Collections.unmodifiableList(Arrays.asList(tokenArray));
    }

    public List<String> getTokens() {
        return tokens;
    }

    /**
     * Fetches a typical command-line parameter identified by a preceding key token. Technically, this method looks for a token that is
     * equal to the provided string, and returns the following token if it exists. If no matching token exists, or there is no following
     * token, null is returned.
     * 
     * Examples: "a -b c" -> getNamedParameter("a") = "-b", getNamedParameter("-b") = "c", getNamedParameter("c") = null
     * 
     * @param key the parameter key
     * @return the token following the given token in the list of command-line arguments
     */
    public String getNamedParameter(String key) {
        boolean lastWasKey = false;
        for (String token : tokens) {
            if (lastWasKey) {
                return token;
            }
            if (key.equals(token)) {
                lastWasKey = true;
            }
        }
        return null; // key not found, or
    }

    /**
     * @param key the token to look for
     * @return true, if the given token exists in the launch parameters
     */
    public boolean containsToken(String key) {
        return tokens.contains(key);
    }

}
