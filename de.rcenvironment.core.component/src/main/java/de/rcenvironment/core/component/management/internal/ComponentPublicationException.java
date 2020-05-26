/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

/**
 * A {@link RuntimeException} for internal errors during component publication, as they should not occur in normal operation, and client
 * code can typically not react to them reasonably.
 *
 * @author Robert Mischke
 */
public class ComponentPublicationException extends RuntimeException {

    private static final long serialVersionUID = 1160982552317311442L;

    public ComponentPublicationException(String msg, Throwable e) {
        super(msg, e);
    }

}
