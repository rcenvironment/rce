/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;

/**
 * Supporting class for getting {@link ServiceCallSender} objects. This interface has to be
 * implemented and registers as an OSGi service by bundles providing a service call protocol and
 * thus a {@link ServiceCallSender} implementation.
 * 
 * @author Doreen Seider
 */
@Deprecated
public interface ServiceCallSenderFactory {

    /**
     * Key for a service property.
     */
    String PROTOCOL = "protocol";

    // TODO add "getProtocol" to allow or similar? - misc_ro

    /**
     * Creates a {@link ServiceCallSender} for the given contact.
     * 
     * @param contact The contact to communicate with by this communicator.
     * @return a {@link ServiceCallSender} ready for sending.
     * @throws CommunicationException if creating the service caller failed.
     */
    ServiceCallSender createServiceCallSender(NetworkContact contact) throws CommunicationException;
}
