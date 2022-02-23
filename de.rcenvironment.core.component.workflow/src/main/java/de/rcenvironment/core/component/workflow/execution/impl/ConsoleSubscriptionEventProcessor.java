/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.GenericSubscriptionEventProcessor;
import de.rcenvironment.core.component.workflow.execution.internal.ConsoleRowProcessor;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;

/**
 * Subscriber for all console notifications in the overall system.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ConsoleSubscriptionEventProcessor extends GenericSubscriptionEventProcessor {

    private static final long serialVersionUID = 5521705555312627039L;

    private final transient Log log = LogFactory.getLog(getClass());

    private final transient AsyncOrderedCallbackManager<ConsoleRowProcessor> callbackManager;

    public ConsoleSubscriptionEventProcessor(ConsoleRowProcessor... processors) {
        callbackManager =
            ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);
        for (ConsoleRowProcessor processor : processors) {
            callbackManager.addListener(processor);
        }
    }

    /**
     * Extract all {@link ConsoleRow}s contained in the batch of notifications and asynchronously send them to all listeners.
     */
    @Override
    protected void processCollectedNotifications(List<Notification> notifications) {
        final List<ConsoleRow> consoleRows = new ArrayList<ConsoleRow>();
        for (Notification notification : notifications) {
            Serializable body = notification.getBody();
            if (body instanceof ConsoleRow) {
                ConsoleRow consoleRow = ((ConsoleRow) notification.getBody());
                consoleRow.setIndex(notification.getHeader().getNumber());
                consoleRows.add(consoleRow);
            } else {
                log.warn("Received unexpected notification of type " + body.getClass() + " for topic "
                    + notification.getHeader().getNotificationIdentifier());
            }
        }

        callbackManager.enqueueCallback(new AsyncCallback<ConsoleRowProcessor>() {

            @Override
            public void performCallback(ConsoleRowProcessor listener) {
                listener.processConsoleRows(consoleRows);
            }
        });
    }
}
