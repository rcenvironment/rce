/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.eventlog.internal.EventLogService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A simple {@link EventLogService} implementation that forwards all events to the provided
 * Apache-Commons-Logging (ACL) implementation. Intended as a runtime placeholder until a more
 * sophisticated implementation is injected by OSGi-DS, and for low-dependency use in unit tests.
 * 
 * @author Robert Mischke
 * 
 */
public class EventLogServiceForwardToACLImpl implements EventLogService {

    private static final Log LOGGER = LogFactory.getLog(EventLogServiceForwardToACLImpl.class);

    @Override
    public void dispatchMessage(EventLogMessage event) {
        String formattedMessage = getFormattedMessage(event);
        // forward to ACL
        // TODO q&d output; could be improved by using log levels
        LOGGER.info(StringUtils.format("%s/%s/%s: %s", event.getContext(), event.getMessageType(), event.getSourceId(), formattedMessage));
    }

    private String getFormattedMessage(EventLogMessage event) {
        // determine message pattern
        String messagePattern;
        if (event.isLocalized()) {
            // TODO implement message localization
            messagePattern = event.getMessage();
        } else {
            messagePattern = event.getMessage();
        }
        // insert parameters into pattern
        return StringUtils.format(messagePattern, event.getParameters());
    }
}
