/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * A notification consists of a {@link NotificationHeader} and a body. Access to this fields is
 * available through the corresponding get()-method. The generic return value for getBody() is
 * {@link Object}. Subscriber has to be aware of the type of the notification in order to handle the
 * body correctly.
 * 
 * @author Doreen Seider
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = -1148551583045246749L;

    private static final String ASSERT_MUST_NOT_BE_NULL = " must not be null!";

    private final NotificationHeader header;
    private final Serializable body;

    /**
     * Creates the {@link NotificationHeader} for this notification.
     * 
     * @param identifier Identifier of the notification.
     * @param sequenceNumber Edition of the notification. It must be greater or equal than 0.
     * @param nodeId The platform where the notification was created.
     * @param body The payload of this notification.
     */
    public <T extends Serializable> Notification(String identifier, long sequenceNumber, InstanceNodeSessionId nodeId, T body) {
        Assertions.isDefined(identifier, "The notification identifier" + ASSERT_MUST_NOT_BE_NULL);
        final int sequenceBarrier = -1;
        Assertions.isBiggerThan(sequenceNumber, sequenceBarrier,
            "The sequence number for the first notification is 0 - it needs to be increaded with each new one.");
        Assertions.isDefined(nodeId, "The platform identifier the notification was created on" + ASSERT_MUST_NOT_BE_NULL);

        header = new NotificationHeader(identifier, sequenceNumber, nodeId);
        this.body = body;
    }

    public NotificationHeader getHeader() {
        return header;
    }

    public Serializable getBody() {
        return body;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Notification) {
            return header.equals(((Notification) object).getHeader())
                && body.equals(((Notification) object).getBody());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return header.hashCode() + body.hashCode();
    }
    
    @Override
    public String toString() {
        return header.toString() + "_" + body.toString();
    }

}
