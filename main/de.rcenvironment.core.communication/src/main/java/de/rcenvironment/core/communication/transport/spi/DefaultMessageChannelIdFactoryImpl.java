/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;

import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.communication.channel.MessageChannelIdFactory;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * A {@link MessageChannelIdFactory} implementation generating JVM-wide unique integer ids.
 * 
 * @author Robert Mischke
 */
public class DefaultMessageChannelIdFactoryImpl implements MessageChannelIdFactory {

    // assuming that 2^31 connections will suffice for now...
    private static AtomicInteger sequence = new AtomicInteger();

    @Override
    public String generateId(boolean selfInitiated) {
        // embed a flag that indicates whether this connection was self- or remote-initiated
        String directionFlag;
        // blame CheckStyle for the verbosity...
        if (selfInitiated) {
            directionFlag = "s";
        } else {
            directionFlag = "r";
        }
        // the running index is for easy identification in log output; the UUID part ensures
        // uniqueness; the leading "c" is to make it recognizable as a connection id -- misc_ro
        return String.format("c%d%s-%s", sequence.incrementAndGet(), directionFlag, IdGenerator.randomUUIDWithoutDashes());
    }
}
