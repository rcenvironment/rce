/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;

import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.communication.channel.MessageChannelIdFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * A {@link MessageChannelIdFactory} implementation generating JVM-wide unique integer ids.
 * 
 * @author Robert Mischke
 */
public class DefaultMessageChannelIdFactoryImpl implements MessageChannelIdFactory {

    private static final int CHANNEL_ID_LENGTH = 32;

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
        // the running index is for easy identification in log output; the random part ensures
        // uniqueness; the leading "c" is to make it recognizable as a connection id -- misc_ro
        return StringUtils.format("c%d%s-%s", sequence.incrementAndGet(), directionFlag,
            IdGenerator.fastRandomHexString(CHANNEL_ID_LENGTH));
    }
}
