/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Default {@link LiveNetworkIdResolutionService} implementation.
 * 
 * @author Robert Mischke
 */
public class LiveNetworkIdResolutionServiceImpl implements LiveNetworkIdResolutionService {

    private final Map<String, InstanceNodeSessionId> instanceNodeSessionIdMap = new HashMap<>();

    private String localInstanceNodeIdString;

    private String localInstanceNodeSessionIdString;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public InstanceNodeSessionId resolveInstanceNodeIdStringToInstanceNodeSessionId(String input) throws IdentifierException {
        final InstanceNodeSessionId result;
        synchronized (this) {
            result = instanceNodeSessionIdMap.get(input);
        }
        if (result != null) {
            return result;
        } else {
            throw new IdentifierException("Cannot resolve '" + input
                + "' to a known instance session id; that node has never been reachable, or is not reachable anymore");
        }

    }

    @Override
    public LogicalNodeSessionId resolveToLogicalNodeSessionId(ResolvableNodeId id) throws IdentifierException {
        final LogicalNodeSessionId result;
        result = resolveToLogicalNodeSessionIdInternal(id);
        if (verboseLogging) {
            log.debug(StringUtils.format("Resolved %s %s to %s", id.getType(), id, result));
        }
        return result;
    }

    private LogicalNodeSessionId resolveToLogicalNodeSessionIdInternal(ResolvableNodeId id) throws IdentifierException {
        // note: this method always resolves to the default (latest) session id, even if the parameter is already a session id itself
        final InstanceNodeSessionId resolvedInstanceNodeSessionId =
            resolveInstanceNodeIdStringToInstanceNodeSessionId(id.getInstanceNodeIdString());

        switch (id.getType()) {
        case INSTANCE_NODE_SESSION_ID:
            if (!resolvedInstanceNodeSessionId.isSameInstanceNodeSessionAs((InstanceNodeSessionId) id)) {
                log.debug(StringUtils.format("Resolved a given session id %s to the more recent session id %s for the same instance node",
                    id, resolvedInstanceNodeSessionId));
            }
            // intentional fall-through (no "break")
        case INSTANCE_NODE_ID:
            return resolvedInstanceNodeSessionId.convertToDefaultLogicalNodeSessionId();
        case LOGICAL_NODE_SESSION_ID:
            if (!resolvedInstanceNodeSessionId.getSessionIdPart().equals(((LogicalNodeSessionId) id).getSessionIdPart())) {
                log.debug(StringUtils.format("Resolved a given session id %s to the more recent session id %s for the same instance node",
                    id, resolvedInstanceNodeSessionId));
            }
            // TODO (p3) >8.0.0 could probably done more efficiently
            return ((LogicalNodeSessionId) id).convertToLogicalNodeId().combineWithInstanceNodeSessionId(resolvedInstanceNodeSessionId);
        case LOGICAL_NODE_ID:
            return ((LogicalNodeId) id).combineWithInstanceNodeSessionId(resolvedInstanceNodeSessionId);
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Registers the local {@link InstanceNodeSessionId} for its instance and instance session ids. This session id is special because it
     * should be protected against any subsequent modification (e.g. by stale information received from other nodes).
     * 
     * @param localINSId the local id to register
     */
    public void registerLocalInstanceNodeSessionId(InstanceNodeSessionId localINSId) {
        synchronized (this) {
            if (localInstanceNodeIdString != null || localInstanceNodeSessionIdString != null) {
                throw new IllegalStateException(); // expect exactly one initialization
            }

            this.localInstanceNodeIdString = localINSId.getInstanceNodeIdString();
            this.localInstanceNodeSessionIdString = localINSId.getInstanceNodeSessionIdString();

            if (instanceNodeSessionIdMap.put(localInstanceNodeIdString, localINSId) != null) {
                throw new IllegalStateException(); // must be the first id to register for this
            }
        }
    }

    /**
     * Registers a new {@link InstanceNodeSessionId} for its instance and instance session ids. If it replaces a previously set
     * {@link InstanceNodeSessionId} for either, log messages are generated.
     * 
     * @param newINSId the new id to register
     */
    public void registerInstanceNodeSessionId(InstanceNodeSessionId newINSId) {

        final String instanceNodeIdString = newINSId.getInstanceNodeIdString();
        final String instanceNodeSessionIdString = newINSId.getInstanceNodeSessionIdString();

        final InstanceNodeSessionId existingResolutionForINId;

        synchronized (this) {

            if (instanceNodeIdString.equals(localInstanceNodeIdString)
                && !instanceNodeSessionIdString.equals(localInstanceNodeSessionIdString)) {
                log.debug(StringUtils.format(
                    "Refused an attempt to replace the local instance's current session id (%s) with another one (%s); "
                        + "this may be caused by stale data received from another instance",
                    localInstanceNodeSessionIdString, instanceNodeSessionIdString));
                if (isAlphabeticallyMoreRecentThan(instanceNodeSessionIdString, localInstanceNodeSessionIdString)) {
                    log.debug(
                        "The conflicting session id received from the network (see above) is more recent than the local instance's "
                            + "session id; this may indicate a duplicate node id within the network, which may result from erronously "
                            + "copying and reusing a profile's internal settings");
                }
                return;
            }

            existingResolutionForINId = instanceNodeSessionIdMap.get(instanceNodeIdString);

            if (existingResolutionForINId == null) {
                instanceNodeSessionIdMap.put(instanceNodeIdString, newINSId);
                log.debug(StringUtils.format(
                    "Registered %s as the first known session id for instance node '%s'", newINSId, instanceNodeIdString));
            } else {
                if (!existingResolutionForINId.equals(newINSId)) {
                    // a new InstanceNodeSessionId became visible before the old one was unregistered
                    if (isAlphabeticallyMoreRecentThan(newINSId.getSessionIdPart(), existingResolutionForINId.getSessionIdPart())) {
                        log.info(StringUtils.format(
                            "Updated the default instance node session id for instance node '%s' from %s to %s; "
                                + "this means that a new session became visible before the old one unregistered from the network "
                                + "(e.g. after a crash or network disconnect)",
                            instanceNodeIdString, existingResolutionForINId, newINSId));
                        instanceNodeSessionIdMap.put(instanceNodeIdString, newINSId);
                    } else {
                        log.debug(StringUtils.format(
                            "Ignored an outdated session id for instance node '%s' (current: %s, ignored: %s) "
                                + "received as part of a remote node's network knowledge",
                            instanceNodeIdString, existingResolutionForINId, newINSId));
                    }
                }
            }
        }
    }

    /**
     * Unregisters a node that has disappeared from the known network.
     * 
     * @param oldINSId the node's session id to unregister
     */
    public void unregisterInstanceNodeSessionId(InstanceNodeSessionId oldINSId) {
        final String instanceNodeIdString = oldINSId.getInstanceNodeIdString();
        synchronized (this) {
            final InstanceNodeSessionId existingResolutionForINId = instanceNodeSessionIdMap.get(instanceNodeIdString);
            if (existingResolutionForINId != null) {
                if (existingResolutionForINId.isSameInstanceNodeSessionAs(oldINSId)) {
                    // the single known session id was unregistered; no known node-to-session resolution anymore
                    // TODO (p2) potential error case: an outdated id could be registered again after this; keep track of all active ids?
                    instanceNodeSessionIdMap.remove(instanceNodeIdString);
                } else {
                    // note: this is a very technical message for the INFO log level, but it should be visible to indicate that the previous
                    // WARNING log message has been resolved
                    log.debug(StringUtils.format(
                        "The outdated instance node session id %s for instance node '%s' has been properly unregistered now",
                        oldINSId, instanceNodeIdString));
                }
            }
        }
    }

    private boolean isAlphabeticallyMoreRecentThan(final String id1, String id2) {
        return id1.compareTo(id2) > 0;
    }

}
