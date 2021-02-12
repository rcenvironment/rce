/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.LogicalNodeManagementService;
import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.uplink.client.session.api.UplinkLogicalNodeMappingService;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link UplinkLogicalNodeMappingService}.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
@Component(immediate = true)
public class UplinkLogicalNodeMappingServiceImpl implements UplinkLogicalNodeMappingService {

    private final Map<String, LogicalNodeId> destinationIdToLogicalNodeMap;

    // Contains the display name announced by the publisher, not the derived name
    // Used to check if the name has changed
    private final Map<String, String> destinationIdToAnnouncedNameMap;

    @Reference
    private LogicalNodeManagementService logicalNodeManagementService;

    public UplinkLogicalNodeMappingServiceImpl() {
        destinationIdToLogicalNodeMap = Collections.synchronizedMap(new HashMap<String, LogicalNodeId>());
        destinationIdToAnnouncedNameMap = Collections.synchronizedMap(new HashMap<String, String>());
    }

    @Override
    public LogicalNodeId createOrGetLocalLogicalNodeIdForDestinationId(String destinationId, String announcedDisplayName) {
        if (destinationIdToLogicalNodeMap.containsKey(destinationId)) {
            return destinationIdToLogicalNodeMap.get(destinationId);
        }
        // If there is no local node yet representing the given remote node, create a new one
        final String logicalNodeRecognitionPart = deriveLogicalNodeRecognitionPart(destinationId);
        final String logicalNodeDisplayName = deriveLogicalNodeDisplayName(destinationId, announcedDisplayName);
        // TODO call logicalNodeManagementService#releaseLogicalNodeId() when no tool is published "from" it anymore
        LogicalNodeId logicalNodeId = logicalNodeManagementService.createRecognizableLocalLogicalNodeId(logicalNodeRecognitionPart,
            logicalNodeDisplayName);
        destinationIdToLogicalNodeMap.put(destinationId, logicalNodeId);
        destinationIdToAnnouncedNameMap.put(destinationId, announcedDisplayName);

        return logicalNodeId;
    }

    private String deriveLogicalNodeDisplayName(String destinationId, String announcedDisplayName) {

        // chars 0-7 with right side padding removed -> login name (e.g. the SSH account)
        final String loginName =
            org.apache.commons.lang3.StringUtils.removeEnd(
                destinationId.substring(0, UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS),
                UplinkProtocolConstants.DESTINATION_ID_PREFIX_PADDING_CHARACTER_AS_STRING);

        // chars 8-15 with right side padding removed -> the user-selected "session qualifier"/"client id"
        final String sessionQualifier = org.apache.commons.lang3.StringUtils.removeEnd(
            destinationId.substring(UplinkProtocolConstants.LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS,
                UplinkProtocolConstants.DESTINATION_ID_PREFIX_LENGTH),
            UplinkProtocolConstants.DESTINATION_ID_PREFIX_PADDING_CHARACTER_AS_STRING);

        // chars 16-31 -> the mapping id assigned by the remote Uplink client representing a node in its (otherwise hidden) network
        // final String nodeMappingId = destinationId.substring(UplinkProtocolConstants.DESTINATION_ID_PREFIX_LENGTH);

        // TODO (p1) 11.0 not safe against suffix impersonation yet, ie a local node maliciously naming itself "(via a/b) RemoteName"
        return StringUtils.format("%s (via %s/%s)", announcedDisplayName, loginName, sessionQualifier);
    }

    private String deriveLogicalNodeRecognitionPart(String destinationId) {
        if (StringUtils.isNullorEmpty(destinationId)) {
            throw new IllegalArgumentException("Empty or null destination ID");
        }
        String nodeIdPart = destinationId.replaceAll("[^0-9a-zA-Z]", "_");
        if (nodeIdPart.length() < CommonIdBase.MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH) {
            throw new IllegalArgumentException("Unexpected short node id");
        }

        // TODO apply hashing to the recognizable node id part?; omitted for development debugging
        return nodeIdPart.substring(0, CommonIdBase.MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH);
    }

    @Override
    public LogicalNodeId getLocalLogicalNodeIdForDestinationIdAndUpdateName(String destinationId, String announcedDisplayName) {
        if (destinationIdToLogicalNodeMap.containsKey(destinationId)) {
            updateDisplayNameIfNecessary(destinationId, announcedDisplayName);
            return destinationIdToLogicalNodeMap.get(destinationId);
        }
        return null;
    }

    @Override
    public String getDestinationIdForLogicalNodeId(LogicalNodeId logicalNodeId) {

        synchronized (destinationIdToLogicalNodeMap) {
            for (Entry<String, LogicalNodeId> entry : destinationIdToLogicalNodeMap.entrySet()) {
                if (entry.getValue().equals(logicalNodeId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    @Override
    public String getDestinationIdForLogicalNodeId(String logicalNodeId) {

        synchronized (destinationIdToLogicalNodeMap) {
            for (Entry<String, LogicalNodeId> entry : destinationIdToLogicalNodeMap.entrySet()) {
                if (entry.getValue().getLogicalNodeIdString().equals(logicalNodeId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void updateDisplayNameIfNecessary(String destinationId, String announcedDisplayName) {
        if (announcedDisplayName != null && (destinationIdToAnnouncedNameMap.get(destinationId) == null
            || !destinationIdToAnnouncedNameMap.get(destinationId).equals(announcedDisplayName))) {
            LogicalNodeId logicalNodeId = destinationIdToLogicalNodeMap.get(destinationId);
            final String logicalNodeDisplayName = deriveLogicalNodeDisplayName(destinationId, announcedDisplayName);
            logicalNodeManagementService.updateDisplayNameForLocalLogicalNodeId(logicalNodeId, logicalNodeDisplayName);
            destinationIdToAnnouncedNameMap.put(destinationId, announcedDisplayName);
        }
    }
}
