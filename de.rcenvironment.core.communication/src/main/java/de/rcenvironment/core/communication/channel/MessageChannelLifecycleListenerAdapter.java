/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import java.util.Set;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Default/empty {@link MessageChannelLifecycleListener} implementation.
 * 
 * @author Robert Mischke
 */
public abstract class MessageChannelLifecycleListenerAdapter implements MessageChannelLifecycleListener {

    @Override
    public void onOutgoingChannelEstablished(MessageChannel connection) {}

    @Override
    public void onOutgoingChannelTerminated(MessageChannel connection) {}

    @Override
    public void setInitialMessageChannels(Set<MessageChannel> currentChannels) {}

}
