/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.legacy.internal;

/**
 * Enumeration to decide between communication contacts and file transfer contacts.
 * 
 * @author Robert Mischke (extracted class out of CommunicationContactMap)
 */
public enum CommunicationType {

    /** CommunicationType for calling remote services. */
    SERVICE_CALL,

    /** CommunicationType for transferring remote files. */
    FILE_TRANSFER
}
