/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.util.List;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Objects that implement this interface can be registered as subscribers with the notification service. The described method is called when
 * a new notification represented by a specified identifier is available.
 * 
 * This interface extends {@link CallbackObject} to support remote subscription by simply passing an object of the implementing class.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface NotificationSubscriber extends CallbackObject {

    /**
     * Called by the notification service to transfer a batch of notifications. Usually implemented by the RCE framework.
     * 
     * @param notifications the list of {@link Notification}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    @CallbackMethod
    void receiveBatchedNotifications(List<Notification> notifications) throws RemoteOperationException;

}
