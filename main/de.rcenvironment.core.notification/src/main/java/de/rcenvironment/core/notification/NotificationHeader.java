/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;
import java.util.Date;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.common.ComparatorUtils;

/**
 * A notification header is associated with a notification. It contains meta information such as the
 * the identifier, the edition, the creation date and platform where the notification was created.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class NotificationHeader implements Comparable<NotificationHeader>, Serializable {

    private static final long serialVersionUID = -7059564485221275362L;

    private long number;

    private Date timestamp = null;

    private String notificationId = null;

    private NodeIdentifier publishPlatform = null;

    /**
     * Creates a new {@link NotificationHeader} with the given information.
     * 
     * @param notificationIdentifier The identifier of the associated notification.
     * @param publisherPlatform The platform where the associated notification was created.
     * @param edition The edition of the associated notification. It is increased with each new
     *        notification and is required to compare two {@link NotificationHeader}.
     */
    public NotificationHeader(String notificationIdentifier, long edition, NodeIdentifier publisherPlatform) {
        timestamp = new Date();
        notificationId = notificationIdentifier;
        number = edition;
        publishPlatform = publisherPlatform;
    }

    /**
     * Returns the creation date of the associated notification.
     * 
     * @return the timestamp as a {@link Date}.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the edition. It is increased with each new notification and is required to compare
     * two headers.
     * 
     * @return the counter.
     */
    public long getNumber() {
        return number;
    }

    /**
     * Returns the identifier of the associated notification.
     * 
     * @return the identifier.
     */
    public String getNotificationIdentifier() {
        return notificationId;
    }

    /**
     * Returns the platform where the associated notification was created.
     * 
     * @return the platform.
     */
    public NodeIdentifier getPublishPlatform() {
        return publishPlatform;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(notificationId);
        builder.append("[");
        builder.append(number);
        builder.append("]");
        builder.append("@");
        builder.append(publishPlatform);
        builder.append(" - ");
        builder.append(timestamp);

        return new String(builder);
    }

    @Override
    public int compareTo(NotificationHeader header) {
        Date otherTimestamp = header.getTimestamp();
        int result = ComparatorUtils.compareLong(timestamp.getTime(), otherTimestamp.getTime());
        if (result != 0) {
            return result;
        }
        return ComparatorUtils.compareLong(number, header.getNumber());
    }

    @Override
    public boolean equals(Object object) {
        return toString().equals(object.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
