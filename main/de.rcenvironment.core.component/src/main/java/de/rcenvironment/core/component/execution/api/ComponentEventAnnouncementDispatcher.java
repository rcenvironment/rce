/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Dispatches {@link ComponentEventAnnouncement}.
 * 
 * @author Doreen Seider
 */
public interface ComponentEventAnnouncementDispatcher {

    /**
     * Dispatches a {@link ComponentEventAnnouncement} via email.
     * 
     * @param recipients recipients (must be valid email addresses)
     * @param compEventAnnouncement component event to announce
     * @param errReceiver to receive error messages
     * 
     * @return <code>true</code> if mail was sent successfully, otherwise <code>false</code>
     */
    boolean dispatchWorkflowEventAnnouncementViaMail(String[] recipients, ComponentEventAnnouncement compEventAnnouncement,
        TextLinesReceiver errReceiver);

}
