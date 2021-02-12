/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Represents the basic states of instance (session) id presence (see {@link State}) with additional id information attached.
 *
 * @author Robert Mischke
 */
public class InstanceSessionNetworkStatus {

    /**
     * The basic states of instance (session) id presence.
     *
     * @author Robert Mischke
     */
    public enum State {
        /**
         * The id is present within the reachable/known network.
         */
        PRESENT,
        /**
         * The id is not present within the reachable/known network.
         */
        NOT_PRESENT,
        /**
         * The instance id is present within the reachable/known network, but with a different session id part; this is typically the case
         * after an instance was restarted and has reconnected to the same network.
         */
        PRESENT_WITH_DIFFERENT_SESSION,
        /**
         * The instance id is present at least twice within the reachable/known network, naturally with different session id parts
         * (otherwise, it would not be properly detectable). This typically happens when users erroneously copy entire profiles and use them
         * in parallel, leading to both instances using the same persistent instance id.
         */
        ID_COLLISION
    }

    private final InstanceNodeSessionId queriedId;

    private final State status;

    private final InstanceNodeSessionId otherId;

    public InstanceSessionNetworkStatus(InstanceNodeSessionId queriedId, State status, InstanceNodeSessionId otherId) {
        this.queriedId = queriedId;
        this.status = status;
        this.otherId = otherId;
    }

    public InstanceNodeSessionId getQueriedId() {
        return queriedId;
    }

    public State getState() {
        return status;
    }

    public InstanceNodeSessionId getOtherId() {
        return otherId;
    }

}
