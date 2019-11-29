/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;


/**
 * Publishing types.
 *
 * @author Oliver Seebach
 */
public enum PublishingType {
    /**
     * Public access.
     */
    PUBLIC,
    /**
     * Local access only.
     */
    LOCAL,
    /**
     * Custom configured access groups.
     */
    CUSTOM
}
