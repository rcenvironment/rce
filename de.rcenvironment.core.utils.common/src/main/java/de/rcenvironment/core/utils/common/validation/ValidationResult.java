/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple container for validation results. Right now, no distinction between different message
 * types is made - typically, all entries are considered to be errors.
 * 
 * @author Robert Mischke
 * 
 */
public class ValidationResult extends Exception {

    private static final long serialVersionUID = -1712797691202438686L;

    private List<String> messages;

    public ValidationResult() {
        this.messages = new ArrayList<String>();
    }

    public ValidationResult(String singleMessage) {
        this.messages = new ArrayList<String>(1);
        this.messages.add(singleMessage);
    }

    /**
     * Adds a validation feedback message. Right now, no distinction of message types is made; all
     * messages are considered validation errors.
     * 
     * @param message the message text to add
     */
    public void addMessage(String message) {
        messages.add(message);
    }

    /**
     * @return an immutable copy of the stored messages
     */
    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int getMessageCount() {
        return messages.size();
    }

}
