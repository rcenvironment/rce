/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;

/**
 * Registry for received node property information.
 * 
 * IMPORTANT: This class performs no synchronization; this is expected to be done by the caller.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesRegistry {

    private final Map<CompositeNodePropertyKey, NodePropertyImpl> knowledgeMap;

    private final Log log = LogFactory.getLog(getClass());

    public NodePropertiesRegistry() {
        this.knowledgeMap = new HashMap<CompositeNodePropertyKey, NodePropertyImpl>();
    }

    /**
     * Returns the full property map for a single node. Modifications to the map do not cause any side effects.
     * 
     * @param nodeId the id of the target node
     * @return the property map
     */
    public Map<String, String> getNodeProperties(NodeIdentifier nodeId) {
        String nodeIdString = nodeId.getIdString();
        Map<String, String> result = new HashMap<String, String>();
        for (NodePropertyImpl entry : knowledgeMap.values()) {
            CompositeNodePropertyKey key = entry.getCompositeKey();
            if (key.getNodeIdString().equals(nodeIdString)) {
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
    public NodeProperty getNodeProperty(NodeIdentifier nodeId, String dataKey) {
        CompositeNodePropertyKey ckey = new CompositeNodePropertyKey(nodeId.getIdString(), dataKey);
        return knowledgeMap.get(ckey);
    }

    /**
     * Returns a single property value for a single node.
     * 
     * @param nodeId the id of the target node
     * @param dataKey the property key to look up
     * @return the value for the given key, or null if it does not exist
     */
    public String getNodePropertyValue(NodeIdentifier nodeId, String dataKey) {
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
     * @return the map of property maps as returned by {@link #getNodeProperties(NodeIdentifier)}
     */
    public Map<NodeIdentifier, Map<String, String>> getAllNodeProperties() {
        Map<NodeIdentifier, Map<String, String>> result = new HashMap<NodeIdentifier, Map<String, String>>();
        for (NodePropertyImpl entry : knowledgeMap.values()) {
            CompositeNodePropertyKey key = entry.getCompositeKey();
            NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(key.getNodeIdString());
            Map<String, String> nodeMap = result.get(nodeId);
            if (!result.containsKey(nodeId)) {
                nodeMap = new HashMap<String, String>();
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
     * @return the map of property maps as returned by {@link #getNodeProperties(NodeIdentifier)}
     */
    public Map<NodeIdentifier, Map<String, String>> getAllNodeProperties(Collection<NodeIdentifier> nodeIds) {
        Map<NodeIdentifier, Map<String, String>> result = new HashMap<NodeIdentifier, Map<String, String>>();
        for (NodeIdentifier nodeId : nodeIds) {
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
        for (NodePropertyImpl entry : update) {
            knowledgeMap.put(entry.getCompositeKey(), entry);
        }
    }

    /**
     * Updates the registry state with those {@link NodePropertyImpl}s that are newer than the data already present, and returns this set of
     * entries.
     * 
     * @param update the entries to merge selectively
     * @return the subset of the given entries that caused a change to the registry state
     */
    public Collection<NodePropertyImpl> mergeAndGetEffectiveSubset(Collection<NodePropertyImpl> update) {
        List<NodePropertyImpl> effectiveSubset = new ArrayList<NodePropertyImpl>();
        for (NodePropertyImpl entry : update) {
            CompositeNodePropertyKey ckey = entry.getCompositeKey();
            NodePropertyImpl existing = knowledgeMap.get(ckey);
            if (existing == null || existing.getSequenceNo() < entry.getSequenceNo()) {
                knowledgeMap.put(ckey, entry);
                effectiveSubset.add(entry);
            }
        }
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
    public Collection<NodePropertyImpl> getComplementingKnowledge(Collection<NodePropertyImpl> input) {
        // create set to avoid O(n*m) search over input keys
        Map<CompositeNodePropertyKey, NodePropertyImpl> inputMap = new HashMap<CompositeNodePropertyKey, NodePropertyImpl>();

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
        Collection<NodePropertyImpl> response = new ArrayList<NodePropertyImpl>();
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

}
