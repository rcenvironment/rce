/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.LogicalNodeManagementService;
import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Default {@link LogicalNodeManagementService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class LogicalNodeManagementServiceImpl implements LogicalNodeManagementService {

    private static final String NODE_PROPERTIES_PREFIX_LOGICAL_NODE_NAMES = "nodeNames/";

    @Reference
    private PlatformService platformService;

    @Reference
    private NodePropertiesService nodePropertiesService;

    @Reference
    private NodeIdentifierService nodeIdentifierService;

    private InstanceNodeId localInstanceId;

    private InstanceNodeSessionId localInstanceSessionId;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * OSGi-DS activation method.
     */
    @Activate
    public void activate() {
        localInstanceId = platformService.getLocalInstanceNodeId();
        localInstanceSessionId = platformService.getLocalInstanceNodeSessionId();
        nodePropertiesService.addRawNodePropertiesChangeListener((newProperties) -> processNewNodeProperties(newProperties));
    }

    @Override
    public InstanceNodeSessionId getLocalInstanceSessionId() {
        return localInstanceSessionId;
    }

    @Override
    public LogicalNodeId createRecognizableLocalLogicalNodeId(String qualifier, String optionalDisplayName) {
        if (StringUtils.isNullorEmpty(qualifier)) {
            throw new IllegalArgumentException("Empty or null qualifier");
        }
        if (qualifier.length() > CommonIdBase.MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH) {
            throw new IllegalArgumentException("Qualifier exceeds allowed length");
        }
        final LogicalNodeId logicalNodeId =
            localInstanceId.expandToLogicalNodeId(CommonIdBase.RECOGNIZABLE_LOGICAL_NODE_PART_PREFIX + qualifier);
        registerOptionalDisplayName(logicalNodeId.getLogicalNodePart(), optionalDisplayName);
        return logicalNodeId;
    }

    @Override
    public void updateDisplayNameForLocalLogicalNodeId(LogicalNodeId logicalNodeId, String newDisplayName) {
        Objects.requireNonNull(newDisplayName);
        registerOptionalDisplayName(logicalNodeId.getLogicalNodePart(), newDisplayName);
    }

    @Override
    public LogicalNodeId createTransientLocalLogicalNodeId(String optionalDisplayName) {
        final LogicalNodeId logicalNodeId = localInstanceId.expandToLogicalNodeId(CommonIdBase.TRANSIENT_LOGICAL_NODE_PART_PREFIX
            + IdGenerator.fastRandomHexString(CommonIdBase.MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH));
        registerOptionalDisplayName(logicalNodeId.getLogicalNodePart(), optionalDisplayName);
        return logicalNodeId;
    }

    private void registerOptionalDisplayName(String logicalNodePart, String optionalDisplayName) {
        if (optionalDisplayName == null) {
            return;
        }
        log.debug(StringUtils.format("Announcing display name '%s' for local logical node :%s:", optionalDisplayName, logicalNodePart));
        // TODO add length limit?
        // TODO add support for encrypted node names
        // there is no need to actively associate the name as the property change will be picked up by the local property change listener
        nodePropertiesService.addOrUpdateLocalNodeProperty(NODE_PROPERTIES_PREFIX_LOGICAL_NODE_NAMES + logicalNodePart,
            optionalDisplayName);
    }

    @Override
    public void releaseLogicalNodeId(LogicalNodeId id) {
        // log.debug(StringUtils.format("Unregistering announced display name '%s' for local logical node :%s", optionalDisplayName,
        // logicalNodePart));
    }

    private void processNewNodeProperties(Collection<? extends NodeProperty> newProperties) {
        for (NodeProperty property : newProperties) {
            if (!property.getKey().startsWith(NODE_PROPERTIES_PREFIX_LOGICAL_NODE_NAMES)) {
                continue;
            }
            final InstanceNodeSessionId instanceNodeSessionId = property.getInstanceNodeSessionId();
            String logicalNodePart = property.getKey().substring(NODE_PROPERTIES_PREFIX_LOGICAL_NODE_NAMES.length());
            final LogicalNodeSessionId logicalNodeSessionId = instanceNodeSessionId.expandToLogicalNodeSessionId(logicalNodePart);
            String displayNameOrNull = property.getValue(); // may be null if the update was the removal of that property
            if (displayNameOrNull != null) {
                final String locationInfo;
                if (instanceNodeSessionId.isSameInstanceNodeAs(localInstanceId)) {
                    locationInfo = "local";
                } else {
                    locationInfo = "remote";
                }
                log.debug(StringUtils.format("Registering name '%s' for %s logical node %s", displayNameOrNull, locationInfo,
                    logicalNodeSessionId.getLogicalNodeSessionIdString()));
            } else {
                log.debug(StringUtils.format("Unregistering name association for logical node %s", displayNameOrNull,
                    logicalNodeSessionId));
            }
            // handles both registration and removal (via null name)
            nodeIdentifierService.associateDisplayNameWithLogicalNode(logicalNodeSessionId, displayNameOrNull);
        }
    }

}
