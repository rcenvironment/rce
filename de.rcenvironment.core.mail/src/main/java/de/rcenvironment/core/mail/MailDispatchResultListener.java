/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

/**
 * This listener will receive information if a mail dispatch was successful.
 *
 * @author Tobias Rodehutskors
 */
public interface MailDispatchResultListener {

    /**
     * @param result The result of the mail dispatch.
     * @param message A message further explaining the MailDispatchResult or null.
     */
    void receiveResult(MailDispatchResult result, String message);
}
