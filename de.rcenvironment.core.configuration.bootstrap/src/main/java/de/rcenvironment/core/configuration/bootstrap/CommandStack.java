/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

/**
 * A simple buffer to read command line arguments from. The purpose of turning this into a class is to avoid scattered
 * "has another argument" checks. With this class, the "getNext()" operation can throw a meaningful exception when another argument is
 * required, but the end of the command line is reached instead.
 * 
 * @author Robert Mischke
 */
public class CommandStack {

    private String[] args;

    private int next = 0;

    public CommandStack(String[] args) {
        this.args = args;
    }

    /**
     * @return True, if there are more tokens to consume.
     */
    public boolean hasNext() {
        return next < args.length;
    }

    /**
     * @return Returns the next token if there is one, or throws an IllegalStateException.
     */
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

    /**
     * Resets the next pointer to the previous position.
     * 
     * @return The position of the next pointer after the reset.
     */
    public int lastPos() {
        return next - 1;
    }

    /**
     * 
     * @return True, if there is another token to consume and it is a value.
     */
    public boolean hasNextIsValue() {

        return hasNext() && !args[next].startsWith("-");
    }
}
