/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.exception;

import java.io.IOException;

/**
 * A generic exception for indicating that a requesting operation failed, if the type of the failure is not relevant.
 * <p>
 * For example, if a service's setting is changed through a service method, and that service tries to persist it internally, this write
 * operation may fail. If this happens, then it is irrelevant for the method caller that there was an internal {@link IOException} - the
 * only relevant information is that changing the setting has unexpectedly failed.
 * <p>
 * If you are unsure whether you should use this generic exception or a more specific one (or several specific ones), consider whether the
 * caller can do anything useful based on the type of exception that has occurred.
 *
 * @author Robert Mischke
 */
public class OperationFailureException extends Exception {

    private static final long serialVersionUID = -1739027521746902903L;

    public OperationFailureException(String message) {
        super(message);
    }

    public OperationFailureException(String message, Throwable cause) {
        super(message, cause);
    }

}
