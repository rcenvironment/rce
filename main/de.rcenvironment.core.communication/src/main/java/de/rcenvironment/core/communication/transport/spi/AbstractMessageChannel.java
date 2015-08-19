/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Abstract base class for the {@link MessageChannel} implementations of network transports.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractMessageChannel implements MessageChannel {

    protected ServerContactPoint associatedSCP;

    private volatile MessageChannelState state = MessageChannelState.CONNECTING;

    private volatile boolean simulatingBreakdown = false;

    private InitialNodeInformation remoteNodeInformation;

    private boolean initiatedByRemote = false;

    private String associatedMirrorChannelId;

    private String connectionId;

    private boolean closedBecauseMirrorChannelClosed = false;

    @Override
    public MessageChannelState getState() {
        return state; // volatile
    }

    @Override
    public InitialNodeInformation getRemoteNodeInformation() {
        return remoteNodeInformation;
    }

    @Override
    public void setRemoteNodeInformation(InitialNodeInformation nodeInformation) {
        this.remoteNodeInformation = nodeInformation;
    }

    @Override
    public void setAssociatedSCP(ServerContactPoint networkContactPoint) {
        this.associatedSCP = networkContactPoint;
    }

    @Override
    public boolean getInitiatedByRemote() {
        return initiatedByRemote;
    }

    @Override
    public void setInitiatedByRemote(boolean value) {
        this.initiatedByRemote = value;
    }

    @Override
    public String getAssociatedMirrorChannelId() {
        return associatedMirrorChannelId;
    }

    @Override
    public boolean isClosedBecauseMirrorChannelClosed() {
        return closedBecauseMirrorChannelClosed;
    }

    @Override
    public void setAssociatedMirrorChannelId(String initiatingChannelId) {
        this.associatedMirrorChannelId = initiatingChannelId;
    }

    @Override
    public void markAsClosedBecauseMirrorChannelClosed() {
        closedBecauseMirrorChannelClosed = true;
    }

    @Override
    public String getChannelId() {
        return connectionId;
    }

    @Override
    public void setChannelId(String id) {
        if (connectionId != null) {
            throw new IllegalArgumentException("Duplicate id assignment");
        }
        connectionId = id;
    }

    @Override
    public synchronized void markAsEstablished() {
        MessageChannelState oldState = state;
        if (oldState != MessageChannelState.CONNECTING) {
            throw new IllegalStateException(oldState.toString());
        }
        state = MessageChannelState.ESTABLISHED;
    }

    @Override
    public boolean isReadyToUse() {
        return state == MessageChannelState.ESTABLISHED;
    }

    @Override
    public final synchronized boolean close() {
        MessageChannelState oldState = state;
        switch (oldState) {
        case ESTABLISHED:
            // standard case
            state = MessageChannelState.CLOSED;
            asyncFireOnClosedOrBroken();
            return true;
        case MARKED_AS_BROKEN:
            // actively closed after already marked as broken; ignore
            return false; // another call already marked this channel as broken, so it was already unregistered
        case CLOSED:
            // duplicate call; ignore
            return false;
        default:
            // should not happen; indicates consistency error
            throw new IllegalStateException(oldState.toString());
        }
    }

    @Override
    public synchronized boolean markAsBroken() {
        MessageChannelState oldState = state;
        switch (oldState) {
        case CONNECTING:
            // can occur on initial handshake; log warning and proceed
            LogFactory.getLog(getClass()).warn("Channel " + getChannelId() + " marked as broken while in state " + oldState.toString());
            return true;
        case ESTABLISHED:
            // standard case
            state = MessageChannelState.MARKED_AS_BROKEN;
            asyncFireOnClosedOrBroken();
            return true;
        case MARKED_AS_BROKEN:
            // duplicate call; ignore
            return false;
        case CLOSED:
            // as closing a connection can cause follow-up errors, they should be ignored and not change the channel's state - misc_ro
            return false; // another call already closed this channel, so it was already unregistered
        default:
            // should not happen; indicates consistency error
            throw new IllegalStateException(oldState.toString());
        }
    }

    private void asyncFireOnClosedOrBroken() {
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Asynchronous handling of connection breakdown")
            public void run() {
                onClosedOrBroken();
            }
        });
    }

    @Override
    public String toString() {
        // TODO improve
        String suffix = "";
        if (simulatingBreakdown) {
            suffix = "; simulating breakdown";
        }
        return String.format("Channel %s (%s%s)", connectionId, state, suffix);
    }

    protected abstract void onClosedOrBroken();

    // for integration testing
    public void setSimulatingBreakdown(boolean simulatingBreakdown) {
        this.simulatingBreakdown = simulatingBreakdown;
    }

    // for integration testing
    public boolean isSimulatingBreakdown() {
        return simulatingBreakdown;
    }
}
