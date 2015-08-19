/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * The state of a command, consisting of a token queue, an {@link TextOutputReceiver} and the original list of tokens.
 * 
 * @author Robert Mischke
 */
public final class CommandContext {

    private final List<String> originalTokens;

    private final Deque<String> remainingTokens;

    private final TextOutputReceiver outputReceiver;

    private Object invokerInformation;

    private boolean developerCommandSetEnabled = false;

    public CommandContext(List<String> originalTokens, TextOutputReceiver outputReceiver, Object invokerInformation) {
        this.originalTokens = originalTokens;
        this.remainingTokens = new LinkedList<String>(originalTokens);
        this.outputReceiver = outputReceiver;
        this.invokerInformation = invokerInformation;
    }

    /**
     * Convenience shortcut for "outputReceiver.processLine()".
     * 
     * @param line the text line to send to the configured {@link TextOutputReceiver}
     */
    public void println(Object line) {
        outputReceiver.addOutput(line.toString());
    }

    /**
     * Returns the first token from the token queue.
     * 
     * @return the first token, or null if the queue is empty
     */
    public String peekNextToken() {
        if (remainingTokens.isEmpty()) {
            return null;
        } else {
            return remainingTokens.peekFirst();
        }
    }

    /**
     * Removes and returns the first token from the token queue.
     * 
     * @return the first token, or null if the queue is empty
     */
    public String consumeNextToken() {
        if (remainingTokens.isEmpty()) {
            return null;
        } else {
            return remainingTokens.removeFirst();
        }
    }

    /**
     * Convenience method to check for command flags. If the next token is exactly equal to the given string, it is consumed and the method
     * returns "true". Otherwise, the token queue remains unmodified and the method returns "false".
     * 
     * @param expected the expected token string
     * @return true if the next token was equal to the the expected string
     */
    public boolean consumeNextTokenIfEquals(String expected) {
        if (expected.equals(remainingTokens.peek())) {
            consumeExpectedToken(expected);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes and returns all tokens from the token queue.
     * 
     * @return the list of all remaining tokens
     */
    public List<String> consumeRemainingTokens() {
        List<String> result = new ArrayList<String>(remainingTokens);
        remainingTokens.clear();
        return result;
    }

    public List<String> getOriginalTokens() {
        return Collections.unmodifiableList(originalTokens);
    }

    public TextOutputReceiver getOutputReceiver() {
        return outputReceiver;
    }

    /**
     * Consumes the next token and verifies that it is equal to the provided string. Otherwise, an {@link IllegalStateException} is thrown.
     * 
     * @param expected the expected token
     */
    public void consumeExpectedToken(String expected) {
        final String actual = consumeNextToken();
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Unexpected token '" + actual + "' (expected: '" + expected + "')");
        }

    }

    /**
     * @return true is the token queue is not empty
     */
    public boolean hasRemainingTokens() {
        return !remainingTokens.isEmpty();
    }

    public boolean isDeveloperCommandSetEnabled() {
        return developerCommandSetEnabled;
    }

    public void setDeveloperCommandSetEnabled(boolean developerCommandSetEnabled) {
        this.developerCommandSetEnabled = developerCommandSetEnabled;
    }

    public Object getInvokerInformation() {
        return invokerInformation;
    }

}
