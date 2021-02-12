/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierImpl;
import de.rcenvironment.core.communication.common.impl.NodeNameDataHolder;
import de.rcenvironment.core.communication.model.NodeInformationRegistry;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Central registry for information gathered about nodes. Currently, the only managed data is the display name association for nodes and
 * node sessions.
 * 
 * @author Robert Mischke
 */
// TODO (p2) 10.1+: rename class and interface
public class NodeInformationRegistryImpl implements NodeInformationRegistry {

    private static final String LOG_PATTERN_CREATING_INITIAL_SESSION_DATA_HOLDER = "Creating initial name data holder for %s";

    private static final String LOG_PATTERN_REPLACING_SESSION_DATA_HOLDER =
        "Discarding name data for session %s as it will be replaced by session %s";

    private static final String LOG_PATTERN_SETTING_NAME_ASSOCIATION = "Setting initial name '%s' for session %s";

    private static final String LOG_PATTERN_REPLACING_NAME_ASSOCIATION = "Replacing name '%s' for session %s with '%s'";

    private static final String LOG_PATTERN_IGNORING_DUPLICATE_NAMING_REQUEST = "Ignoring request to set the name '%s' for %s again";

    private static final String LOG_PATTERN_IGNORING_NAME_CHANGE_FOR_NON_CURRENT_SESSION =
        "Ignoring name change request '%s' for non-current session %s";

    // Note that currently, all instance's data is kept forever, which is technically a memory leak. However, the amount of data stored is
    // small. If this still becomes a problem, a timeout/GC mechanism could be added. -- misc_ro, 2019-07
    private final Map<String, InstanceSessionData> currentSessionDataByInstanceId = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Represents the data associated with an instance's current session, i.e. the one with the "highest" encountered session id so far.
     *
     * @author Robert Mischke
     */
    private final class InstanceSessionData {

        private final InstanceNodeSessionId sessionId;

        private final String sessionIdPart; // stored separately again for convenience

        private final Map<String, NodeNameDataHolder> logicalNodeNames = new HashMap<>();

        private NodeNameDataHolder sessionNameDataHolder;

        InstanceSessionData(InstanceNodeSessionId sessionId) {
            this.sessionId = sessionId;
            this.sessionIdPart = sessionId.getSessionIdPart();
        }

        public boolean matchesSessionIdOf(CommonIdBase id) {
            NodeIdentifierImpl idImpl = (NodeIdentifierImpl) id;
            final String sessionIdPartOfParam = idImpl.getSessionIdPart();
            if (this.sessionIdPart == null || sessionIdPartOfParam == null) {
                return false;
            }
            return this.sessionIdPart.equals(sessionIdPartOfParam);
        }

        public boolean isObsoletedBy(InstanceNodeSessionId sessionIdParam) {
            // relies on id validation to ensure non-null parts
            return sessionIdParam.getSessionIdPart().compareTo(this.sessionIdPart) > 0;
        }

        public void setInstanceSessionNameHolder(NodeNameDataHolder dataHolder) {
            this.sessionNameDataHolder = dataHolder;
        }

        public void setOrRemoveLogicalNodeName(String logicalNodePart, String newName) {

            if (newName != null) {
                // TODO assuming only resolved names for now; add support for encrypted names
                logicalNodeNames.put(logicalNodePart, new NodeNameDataHolder(newName));
            } else {
                logicalNodeNames.remove(logicalNodePart);
            }
        }

        public Optional<NodeNameDataHolder> getMostSpecificNameHolder(NodeIdentifierImpl nodeId) {

            // if the given id refers to a non-default logical node, check for a specific name set for it
            String logicalNodePart = nodeId.getLogicalNodePart();
            if (logicalNodePart != null && !logicalNodePart.equals(CommonIdBase.DEFAULT_LOGICAL_NODE_PART)) {
                NodeNameDataHolder specificNameHolder = logicalNodeNames.get(logicalNodePart);
                if (specificNameHolder != null) {
                    return Optional.of(specificNameHolder);
                }
            }

            switch (nodeId.getType()) {
            case INSTANCE_NODE_ID:
            case LOGICAL_NODE_ID: // fallback: no specific logical node name found
                return Optional.ofNullable(sessionNameDataHolder);
            case INSTANCE_NODE_SESSION_ID:
            case LOGICAL_NODE_SESSION_ID: // fallback: no specific logical node name found
                final String requestedSessionIdPart = nodeId.getSessionIdPart();
                if (requestedSessionIdPart.compareTo(this.sessionIdPart) >= 0) {
                    // the requested session is either the current one, or a new session that was just discovered;
                    // in both cases, return the current session's name
                    return Optional.ofNullable(sessionNameDataHolder);
                } else {
                    // special case: name request for an outdated session
                    // current behavior: return a synthetic holder with the new session's name and a marker suffix
                    final String currentSessionName;
                    if (sessionNameDataHolder != null) {
                        currentSessionName = sessionNameDataHolder.getResolvedName(); // should always be resolved
                    } else {
                        currentSessionName = CommonIdBase.DEFAULT_DISPLAY_NAME;
                    }
                    return Optional.of(new NodeNameDataHolder(currentSessionName + CommonIdBase.DISPLAY_NAME_SUFFIX_FOR_OUTDATED_SESSIONS));
                }
            default:
                throw new IllegalArgumentException();
            }
        }

    }

    @Override
    public synchronized void associateDisplayName(InstanceNodeSessionId id, String newName) {
        InstanceSessionData currentSessionData = getOrReplaceCurrentSessionDataForInstance(id);
        String fullIdString = id.getInstanceNodeSessionIdString();
        if (!currentSessionData.matchesSessionIdOf(id)) {
            log.debug(StringUtils.format(LOG_PATTERN_IGNORING_NAME_CHANGE_FOR_NON_CURRENT_SESSION, newName, fullIdString));
            return;
        }

        NodeNameDataHolder oldNameHolder = currentSessionData.sessionNameDataHolder;
        if (oldNameHolder == null) {
            log.debug(StringUtils.format(LOG_PATTERN_SETTING_NAME_ASSOCIATION, newName, fullIdString));
        } else {
            // assuming here that session names are always resolved (i.e. unencrypted)
            final String oldName = oldNameHolder.getResolvedName();
            if (!newName.equals(oldName)) {
                log.debug(StringUtils.format(LOG_PATTERN_REPLACING_NAME_ASSOCIATION, oldName, fullIdString, newName));
            } else {
                // TODO make "verbose logging" only?
                log.debug(StringUtils.format(LOG_PATTERN_IGNORING_DUPLICATE_NAMING_REQUEST, newName, fullIdString));
            }
        }
        currentSessionData.setInstanceSessionNameHolder(new NodeNameDataHolder(newName));
    }

    @Override
    public synchronized void associateDisplayNameWithLogicalNode(LogicalNodeSessionId id, String newName) {
        InstanceSessionData currentSessionData = getOrReplaceCurrentSessionDataForInstance(id.convertToInstanceNodeSessionId());
        final String fullIdString = id.getLogicalNodeSessionIdString();
        if (!currentSessionData.matchesSessionIdOf(id)) {
            log.debug(
                StringUtils.format(LOG_PATTERN_IGNORING_NAME_CHANGE_FOR_NON_CURRENT_SESSION, newName, fullIdString));
            return;
        }

        currentSessionData.setOrRemoveLogicalNodeName(id.getLogicalNodePart(), newName);
    }

    @Override
    public synchronized String getDisplayNameForNodeId(NodeIdentifierImpl nodeId, boolean replaceNullWithDefaultName) {

        final InstanceSessionData instanceSessionData = currentSessionDataByInstanceId.get(nodeId.getInstanceNodeIdString());
        if (instanceSessionData == null) {
            if (replaceNullWithDefaultName) {
                return NodeIdentifierImpl.DEFAULT_DISPLAY_NAME;
            } else {
                return null;
            }
        }

        Optional<NodeNameDataHolder> lookupResult = instanceSessionData.getMostSpecificNameHolder(nodeId);
        if (lookupResult.isPresent()) {
            final NodeNameDataHolder nameHolder = lookupResult.get();
            if (nameHolder.isResolved()) {
                return nameHolder.getResolvedName();
            } else {
                return "<not resolved yet>"; // TODO implement decryption etc.
            }
        } else {
            if (replaceNullWithDefaultName) {
                return NodeIdentifierImpl.DEFAULT_DISPLAY_NAME;
            } else {
                return null;
            }
        }
    }

    @Override
    public synchronized void printAllNameAssociations(PrintStream output, String introText) {
        if (introText != null) {
            output.println(introText);
        }
        final Map<String, InstanceSessionData> sortedSnapshot = new TreeMap<>(currentSessionDataByInstanceId);
        for (Entry<String, InstanceSessionData> entry : sortedSnapshot.entrySet()) {
            final String stringValue = entry.getValue().sessionNameDataHolder.getResolvedName();
            if (stringValue != null) {
                output.println(StringUtils.format("  %s -> \"%s\"", entry.getKey(), stringValue));
            } else {
                output.println(StringUtils.format("  %s -> <null>", entry.getKey()));
            }
        }
    }

    private InstanceSessionData getOrReplaceCurrentSessionDataForInstance(InstanceNodeSessionId id) {
        final String instanceNodeIdString = id.getInstanceNodeIdString();
        InstanceSessionData currentSessionData = currentSessionDataByInstanceId.get(instanceNodeIdString);
        if (currentSessionData == null || currentSessionData.isObsoletedBy(id)) {
            if (currentSessionData == null) {
                // TODO make "verbose logging" only?
                log.debug(StringUtils.format(LOG_PATTERN_CREATING_INITIAL_SESSION_DATA_HOLDER, id.getInstanceNodeSessionIdString()));
            } else {
                log.debug(StringUtils.format(LOG_PATTERN_REPLACING_SESSION_DATA_HOLDER,
                    currentSessionData.sessionId, id.getSessionIdPart()));
            }
            // no previous session or the parameter session is newer than the stored one -> set as the new "current" session
            currentSessionData = new InstanceSessionData(id);
            currentSessionDataByInstanceId.put(instanceNodeIdString, currentSessionData);
        }
        return currentSessionData;
    }

}
