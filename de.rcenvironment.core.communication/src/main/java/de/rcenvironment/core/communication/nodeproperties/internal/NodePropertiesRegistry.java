/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Registry for received node property information.
 * 
 * IMPORTANT: This class performs no synchronization; this is expected to be done by the caller.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesRegistry {

    // Note that both of these maps are technically causing memory leaks at this time, as any instance id that has been observed once is
    // never fully removed. Unfortunately, this cannot be avoided for now without checking consistency on network splits and re-joins first.
    private final Map<CompositeNodePropertyKey, NodePropertyImpl> knowledgeMap = new HashMap<>();

    private final Map<String, String> mostRecentSessionIds = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Returns the full property map for a single node. Modifications to the map do not cause any side effects.
     * 
     * @param nodeId the id of the target node
     * @return the property map
     */
    public Map<String, String> getNodeProperties(InstanceNodeSessionId nodeId) {
        String nodeIdString = nodeId.getInstanceNodeSessionIdString();
        Map<String, String> result = new HashMap<>();
        for (NodePropertyImpl entry : knowledgeMap.values()) {
            CompositeNodePropertyKey key = entry.getCompositeKey();
            if (key.getInstanceNodeSessionIdString().equals(nodeIdString)) {
                result.put(key.getDataKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Returns a single property for a single node.
     * 
     * @param nodeId the id of the target node
     * @param dataKey the property key to look up
     * @return the value for the given key, or null if it does not exist
     */
    public NodeProperty getNodeProperty(InstanceNodeSessionId nodeId, String dataKey) {
        CompositeNodePropertyKey ckey = new CompositeNodePropertyKey(nodeId.getInstanceNodeSessionIdString(), dataKey);
        return knowledgeMap.get(ckey);
    }

    /**
     * Returns a single property value for a single node.
     * 
     * @param nodeId the id of the target node
     * @param dataKey the property key to look up
     * @return the value for the given key, or null if it does not exist
     */
    public String getNodePropertyValue(InstanceNodeSessionId nodeId, String dataKey) {
        NodeProperty property = getNodeProperty(nodeId, dataKey);
        if (property != null) {
            return property.getValue();
        } else {
            return null;
        }
    }

    /**
     * Returns the full property map for all known nodes.
     * 
     * @return the map of property maps as returned by {@link #getNodeProperties(InstanceNodeSessionId)}
     */
    public Map<InstanceNodeSessionId, Map<String, String>> getAllNodeProperties() {
        Map<InstanceNodeSessionId, Map<String, String>> result = new HashMap<>();
        for (NodePropertyImpl entry : knowledgeMap.values()) {
            CompositeNodePropertyKey key = entry.getCompositeKey();
            InstanceNodeSessionId nodeId = entry.getInstanceNodeSessionId();
            Map<String, String> nodeMap = result.get(nodeId);
            if (!result.containsKey(nodeId)) {
                nodeMap = new HashMap<>();
                result.put(nodeId, nodeMap);
            }
            nodeMap.put(key.getDataKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Returns the full property map for all given nodes.
     * 
     * @param nodeIds the ids of the relevant nodes
     * @return the map of property maps as returned by {@link #getNodeProperties(InstanceNodeSessionId)}
     */
    public Map<InstanceNodeSessionId, Map<String, String>> getAllNodeProperties(Collection<InstanceNodeSessionId> nodeIds) {
        Map<InstanceNodeSessionId, Map<String, String>> result = new HashMap<>();
        for (InstanceNodeSessionId nodeId : nodeIds) {
            result.put(nodeId, getNodeProperties(nodeId));
        }
        return result;
    }

    /**
     * Merges the given {@link NodePropertyImpl}s into the registry state. No timestamp checking is performed; it is assumed that all given
     * entries are up-to-date.
     * 
     * @param update the entries to merge
     */
    public void mergeUnchecked(Collection<NodePropertyImpl> update) {
        Map<String, String> updatedSessionsByInstanceNodeIds = new HashMap<>();
        for (NodePropertyImpl entry : update) {
            final boolean performActualMerge = processSessionTimestampOfIncomingEntry(entry, updatedSessionsByInstanceNodeIds);
            if (performActualMerge) {
                knowledgeMap.put(entry.getCompositeKey(), entry);
            }
        }
        purgeOutdatedEntries(updatedSessionsByInstanceNodeIds);
    }

    /**
     * Updates the registry state with those {@link NodePropertyImpl}s that are newer than the data already present, and returns this set of
     * entries.
     * 
     * @param update the entries to merge selectively
     * @return the subset of the given entries that caused a change to the registry state
     */
    public Collection<NodePropertyImpl> mergeAndGetEffectiveSubset(Collection<NodePropertyImpl> update) {
        final Map<String, String> updatedSessionsByInstanceNodeIds = new HashMap<>();
        final List<NodePropertyImpl> effectiveSubset = new ArrayList<>();
        for (NodePropertyImpl entry : update) {
            final boolean performActualMerge = processSessionTimestampOfIncomingEntry(entry, updatedSessionsByInstanceNodeIds);
            if (performActualMerge) {
                CompositeNodePropertyKey ckey = entry.getCompositeKey();
                NodePropertyImpl existing = knowledgeMap.get(ckey);
                if (existing == null || existing.getSequenceNo() < entry.getSequenceNo()) {
                    knowledgeMap.put(ckey, entry);
                    effectiveSubset.add(entry);
                }
            }
        }
        purgeOutdatedEntries(updatedSessionsByInstanceNodeIds);
        return effectiveSubset;
    }

    public Collection<NodePropertyImpl> getDetachedCopyOfEntries() {
        return Collections.unmodifiableCollection(new ArrayList<NodePropertyImpl>(knowledgeMap.values()));
    }

    /**
     * Returns the part of the registry state that is newer than or not present in the given entries.
     * 
     * This method is used to synchronize registries: Registry A sends its complete state to B, B merges this information and sends the
     * complementing set to A, which A merges into its state. After this exchange, both registries hold the same data.
     * 
     * @param input the entries representing the known state of the sender
     * @return the set of entries that newer than or not present in the given input
     */
    // TODO check whether this should filter to exclude the calling node, and/or exclude session data outdated by the input
    public Collection<NodePropertyImpl> getComplementingKnowledge(Collection<NodePropertyImpl> input) {
        // create set to avoid O(n*m) search over input keys
        Map<CompositeNodePropertyKey, NodePropertyImpl> inputMap = new HashMap<>();

        for (NodePropertyImpl entry : input) {
            CompositeNodePropertyKey ckey = entry.getCompositeKey();
            NodeProperty existing = inputMap.get(ckey);
            if (existing == null) {
                // typical case: one entry per key
                inputMap.put(ckey, entry);
            } else {
                // log warning and fall back to full knowledge response
                log.warn("Received node property update with more than one entry for key "
                    + ckey + "; falling back to full knowledge response");
                return getDetachedCopyOfEntries();
            }
        }

        // construct response
        Collection<NodePropertyImpl> response = new ArrayList<>();
        for (NodePropertyImpl ownEntry : knowledgeMap.values()) {
            CompositeNodePropertyKey ckey = ownEntry.getCompositeKey();

            NodePropertyImpl correspondingInput = inputMap.get(ckey);
            if (correspondingInput == null || correspondingInput.getSequenceNo() < ownEntry.getSequenceNo()) {
                // missing or older -> add own entry to response
                response.add(ownEntry);
            }
        }
        return Collections.unmodifiableCollection(response);
    }

    public int getEntryCount() {
        return knowledgeMap.size();
    }

    /**
     * @param entry the received {@link NodeProperty}
     * @param updatedSessionsByInstanceNodeIds a map for collecting changed session ids of observed/known nodes
     * @return true if the received node property is part of the current or a more recent session, and should be applied
     */
    private boolean processSessionTimestampOfIncomingEntry(NodePropertyImpl entry, Map<String, String> updatedSessionsByInstanceNodeIds) {
        final boolean performActualMerge;
        // extract node id and session id (which is its starting timestamp)
        InstanceNodeSessionId instanceNodeSessionId = entry.getInstanceNodeSessionId();
        final String instanceNodeIdString = instanceNodeSessionId.getInstanceNodeIdString();
        final String incomingSessionId = instanceNodeSessionId.getSessionIdPart();
        // fetch the most recent known session for that node
        final String previousSessionId = mostRecentSessionIds.get(instanceNodeIdString);
        final int incomingSessionComparedToPrevious;
        if (previousSessionId != null) {
            // actually compare them
            incomingSessionComparedToPrevious = compareSessionIdTimes(incomingSessionId, previousSessionId);
        } else {
            // if there is no previous session, set the comparison result to "newer"
            incomingSessionComparedToPrevious = 1;
        }
        if (incomingSessionComparedToPrevious < 0) {
            // old session data received -> ignore
            log.debug("Received a node property for the outdated node session " + instanceNodeSessionId + "; ignoring the update");
            performActualMerge = false;
        } else {
            if (incomingSessionComparedToPrevious > 0) {
                // newer session observed -> register it for purging the old session
                mostRecentSessionIds.put(instanceNodeIdString, incomingSessionId);
                updatedSessionsByInstanceNodeIds.put(instanceNodeIdString, incomingSessionId);
            }
            performActualMerge = true;
        }
        return performActualMerge;
    }

    private void purgeOutdatedEntries(Map<String, String> sessionPartsOfAffectedInstanceNodeIds) {
        final List<CompositeNodePropertyKey> propertyKeysToDelete = new ArrayList<>();
        for (Entry<CompositeNodePropertyKey, NodePropertyImpl> propertyEntry : knowledgeMap.entrySet()) {
            final CompositeNodePropertyKey propertyKey = propertyEntry.getKey();
            // note: it would be slightly more efficient if the key parts could be accessed without string operations,
            // but the benefit is too small compared to the risk of the required changes so close to release
            String propertyKeyInstanceNodeSessionId = propertyKey.getInstanceNodeSessionIdString();
            String propertyKeyInstanceNodeIdPart =
                propertyKeyInstanceNodeSessionId.substring(0, CommonIdBase.INSTANCE_PART_LENGTH);
            // null for unaffected nodes
            String incomingSessionPart = sessionPartsOfAffectedInstanceNodeIds.get(propertyKeyInstanceNodeIdPart);
            if (incomingSessionPart != null) {
                String propertyKeySessionPart =
                    propertyKeyInstanceNodeSessionId
                        .substring(propertyKeyInstanceNodeSessionId.length() - CommonIdBase.SESSION_PART_LENGTH);
                if (isSessionIdMoreRecentThan(incomingSessionPart, propertyKeySessionPart)) {
                    // TODO 9.0.0 (p1): consider disabling this log message for release
                    log.debug(StringUtils.format("Removing cached node property %s as the most recent session id is now %s",
                        propertyKey, incomingSessionPart));
                    propertyKeysToDelete.add(propertyKey);
                }
            }
        }
        for (CompositeNodePropertyKey key : propertyKeysToDelete) {
            knowledgeMap.remove(key);
        }
    }

    private boolean isSessionIdMoreRecentThan(String session1, String session2) {
        // alphabetic comparison of hex strings to avoid parsing
        return compareSessionIdTimes(session1, session2) > 0;
    }

    private int compareSessionIdTimes(String session1, String session2) {
        return session1.compareTo(session2);
    }

}
