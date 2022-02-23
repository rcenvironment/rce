/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common.impl;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.NodeNameResolver;
import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdType;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.model.NodeInformationRegistry;
import de.rcenvironment.core.communication.model.internal.NodeInformationRegistryImpl;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.DefaultTimeSource;
import de.rcenvironment.toolkit.utils.common.IdGenerator;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;
import de.rcenvironment.toolkit.utils.common.TimeSource;

/**
 * {@link NodeIdentifierService} implementation.
 * 
 * @author Robert Mischke
 */
public final class NodeIdentifierServiceImpl implements NodeIdentifierService {

    private static final int TWO_EXTRA_BYTES_MULTIPLIER = 256;

    // note: must be static to prevent collisions even if multiple virtual instances instantiate this service individually
    private static final AtomicLong sharedSequentialSessionPartIncrement = new AtomicLong();

    private final NodeInformationRegistry nodeInformationRegistry = new NodeInformationRegistryImpl();

    private final NodeNameResolver nodeNameResolver = nodeInformationRegistry;

    private final IdGeneratorType idGeneratorType;

    private final TimeSource timeSource = new DefaultTimeSource(); // used to easily support mock testing later

    private final Log log = LogFactory.getLog(getClass());

    public NodeIdentifierServiceImpl() {
        this(IdGeneratorType.SECURE); // default to secure id generation
    }

    public NodeIdentifierServiceImpl(IdGeneratorType idGeneratorPreference) {
        this.idGeneratorType = idGeneratorPreference;
    }

    @Override
    public InstanceNodeId generateInstanceNodeId() {
        final String instanceIdString = createRandomHexString(CommonIdBase.INSTANCE_PART_LENGTH);
        final String fullIdString = instanceIdString; // collision-free with session ids, even if the session part was empty
        return new NodeIdentifierImpl(instanceIdString, null, null, fullIdString, nodeNameResolver, IdType.INSTANCE_NODE_ID);
    }

    @Override
    public CommonIdBase parseSelectableTypeIdString(String instanceIdString, IdType targetIdType) throws IdentifierException {
        return new NodeIdentifierImpl(instanceIdString, nodeNameResolver, targetIdType);
    }

    @Override
    public InstanceNodeId parseInstanceNodeIdString(String input) throws IdentifierException {
        return new NodeIdentifierImpl(input, nodeNameResolver, IdType.INSTANCE_NODE_ID);
    }

    @Override
    public InstanceNodeSessionId parseInstanceNodeSessionIdString(String input) throws IdentifierException {
        return new NodeIdentifierImpl(input, nodeNameResolver, IdType.INSTANCE_NODE_SESSION_ID);
    }

    @Override
    public LogicalNodeId parseLogicalNodeIdString(String input) throws IdentifierException {
        return new NodeIdentifierImpl(input, nodeNameResolver, IdType.LOGICAL_NODE_ID);
    }

    @Override
    public LogicalNodeSessionId parseLogicalNodeSessionIdString(String input) throws IdentifierException {
        return new NodeIdentifierImpl(input, nodeNameResolver, IdType.LOGICAL_NODE_SESSION_ID);
    }

    @Override
    public InstanceNodeSessionId generateInstanceNodeSessionId(InstanceNodeId instanceId) {
        final String instanceIdString = instanceId.getInstanceNodeIdString(); // assumed to be validated
        final String sessionIdPart = createTimestampHexString(CommonIdBase.SESSION_PART_LENGTH);
        final String fullIdString =
            StringUtils.format("%s" + CommonIdBase.STRING_FORM_PART_SEPARATOR + CommonIdBase.STRING_FORM_PART_SEPARATOR + "%s",
                instanceIdString, sessionIdPart);
        return new NodeIdentifierImpl(instanceIdString, null, sessionIdPart, fullIdString, nodeNameResolver,
            IdType.INSTANCE_NODE_SESSION_ID);
    }

    @Override
    public void associateDisplayName(InstanceNodeSessionId id, String newName) {
        nodeInformationRegistry.associateDisplayName(id, newName);
    }

    @Override
    public void associateDisplayNameWithLogicalNode(LogicalNodeSessionId id, String newName) {
        nodeInformationRegistry.associateDisplayNameWithLogicalNode(id, newName);
    }

    @Override
    public void printAllNameAssociations(PrintStream output, String introText) {
        nodeInformationRegistry.printAllNameAssociations(output, introText);
    }

    private String createRandomHexString(int length) {
        return IdGenerator.createRandomHexString(length, idGeneratorType);
    }

    private String createTimestampHexString(int totalLength) {
        // uses timestamp with int32 + 2 extra bytes accuracy
        final long scaledTimestamp = timeSource.getCurrentTimeMillis() / 4; // approximately *TWO_EXTRA_BYTES_MULTIPLIER/1000
        if (scaledTimestamp < 0 || scaledTimestamp > (long) Integer.MAX_VALUE * TWO_EXTRA_BYTES_MULTIPLIER) {
            throw new IllegalStateException();
        }
        // adjust with running index to ensure unique session ids in unit tests without artificial wait times
        long adjustedTimestampValue = scaledTimestamp + sharedSequentialSessionPartIncrement.incrementAndGet();
        final String hexString = Long.toHexString(adjustedTimestampValue);
        if (hexString.length() == totalLength) {
            // default case
            return hexString;
        } else {
            // only relevant for mock time sources, which may return time values close to zero
            // left pad with zeros for proper lexical comparison
            return "0000000000".substring(0, totalLength - hexString.length()) + hexString;
        }
    }

}
