/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierServiceImpl;
import de.rcenvironment.toolkit.utils.common.IdGenerator;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * Utility/factory class to encapsulate the creation of test node identifiers.
 * 
 * @author Robert Mischke
 */
public final class NodeIdentifierTestUtils {

    private static NodeIdentifierService sharedTestNodeIdentifierService;

    private static boolean allowImplicitNodeIdServiceCreation = true;

    private NodeIdentifierTestUtils() {}

    /**
     * @return a random instance id string as used in {@link InstanceNodeId} instances; for unit testing only
     */
    public static String createTestInstanceNodeIdString() {
        return IdGenerator.fastRandomHexString(CommonIdBase.INSTANCE_PART_LENGTH);
    }

    /**
     * @return an {@link InstanceNodeId} with a randomly generated instance id and a "null" session id part
     */
    public static InstanceNodeId createTestInstanceNodeId() {
        return getTestNodeIdentifierService().generateInstanceNodeId();
    }

    /**
     * @return a random instance session id string as used in {@link InstanceNodeSessionId} instances; for unit testing only
     */
    public static String createTestInstanceNodeSessionIdString() {
        return createTestInstanceNodeSessionId().getInstanceNodeSessionIdString();
    }

    /**
     * @return a {@link InstanceNodeSessionId} with a randomly generated instance id and a randomly generated session id part
     */
    public static InstanceNodeSessionId createTestInstanceNodeSessionId() {
        return getTestNodeIdentifierService()
            .generateInstanceNodeSessionId(getTestNodeIdentifierService().generateInstanceNodeId());
    }

    /**
     * @param instanceId the instance id to use
     * @return a {@link InstanceNodeSessionId} with the given instance id and a randomly generated session id part
     */
    public static InstanceNodeSessionId createTestInstanceNodeSessionId(String instanceId) {
        try {
            return getTestNodeIdentifierService().generateInstanceNodeSessionId(
                getTestNodeIdentifierService().parseInstanceNodeIdString(instanceId));
        } catch (IdentifierException e) {
            throw new RuntimeException("Error creating test instance session id", e);
        }
    }

    /**
     * @return a {@link InstanceNodeSessionId} with a randomly generated instance id and a randomly generated session id part; additionally,
     *         the given display name is associated with this instance id using an internal singleton map (which only makes this appropriate
     *         for unit/integration tests that do not rely on accurate multi-instance name data propagation)
     * 
     * @param displayName the display name to associate with this instance id
     */
    public static InstanceNodeSessionId createTestInstanceNodeSessionIdWithDisplayName(String displayName) {
        final InstanceNodeSessionId id = createTestInstanceNodeSessionId();
        getTestNodeIdentifierService().associateDisplayName(id, displayName);
        return id;
    }

    /**
     * @return a {@link InstanceNodeSessionId} with a randomly generated instance id and a randomly generated session id part; additionally,
     *         the given display name is associated with this instance id using an internal singleton map (which only makes this appropriate
     *         for unit/integration tests that do not rely on accurate multi-instance name data propagation)
     * 
     * @param instanceId the instance id to generate the session for
     * @param displayName the display name to associate with this instance id
     */
    public static InstanceNodeSessionId createTestInstanceNodeSessionIdWithDisplayName(InstanceNodeId instanceId, String displayName) {
        final InstanceNodeSessionId id = getTestNodeIdentifierService().generateInstanceNodeSessionId(instanceId);
        getTestNodeIdentifierService().associateDisplayName(id, displayName);
        return id;
    }

    /**
     * @return a {@link LogicalNodeId} with a random instance part and the default logical node part.
     */
    public static LogicalNodeId createTestDefaultLogicalNodeId() {
        final LogicalNodeId id = getTestNodeIdentifierService().generateInstanceNodeId().convertToDefaultLogicalNodeId();
        return id;
    }

    /**
     * @return a {@link InstanceNodeSessionId} with all id parts being randomly generated
     * 
     * @param useDefaultLogicalNode if a default logical node should be generated
     */
    public static LogicalNodeSessionId createTestLogicalNodeSessionId(boolean useDefaultLogicalNode) {
        if (!useDefaultLogicalNode) {
            throw new IllegalArgumentException("Not supported yet");
        } else {
            LogicalNodeSessionId id = createTestInstanceNodeSessionId().convertToDefaultLogicalNodeSessionId();
            return id;
        }
    }

    /**
     * @return a {@link InstanceNodeSessionId} with all id parts being randomly generated; additionally, the given display name is
     *         associated with this id
     * 
     * @param displayName the display name to associate with this instance id
     * @param useDefaultLogicalNode if a default logical node should be generated
     */
    public static LogicalNodeSessionId createTestLogicalNodeSessionIdWithDisplayName(String displayName,
        boolean useDefaultLogicalNode) {
        if (!useDefaultLogicalNode) {
            throw new IllegalArgumentException("Not supported yet");
        } else {
            final InstanceNodeSessionId instanceNodeSessionId = createTestInstanceNodeSessionId();
            LogicalNodeSessionId id = instanceNodeSessionId.convertToDefaultLogicalNodeSessionId();
            getTestNodeIdentifierService().associateDisplayName(instanceNodeSessionId, displayName);
            return id;
        }
    }

    /**
     * Getter for unit tests that need a direct reference to the implicitly created {@link NodeIdentifierService}.
     * 
     * @return the {@link NodeIdentifierService} singleton instance
     */
    public static synchronized NodeIdentifierService getTestNodeIdentifierService() {
        if (sharedTestNodeIdentifierService == null) {
            if (!allowImplicitNodeIdServiceCreation) {
                throw new RuntimeException("Received a method call that requires a NodeIdentifierService, "
                    + "but implicit instance creation was forbidden");
            }
            LogFactory.getLog(NodeIdentifierTestUtils.class).debug(
                "Creating implicit NodeIdentifierService; this message should only apppear during test execution");
            sharedTestNodeIdentifierService = new NodeIdentifierServiceImpl(IdGeneratorType.FAST);
        }
        return sharedTestNodeIdentifierService;
    }

    /**
     * Utility method for unit/integration tests to ensure that no previous service instance is used.
     * 
     * @param allowImplicitCreation true to allow normal usage of {@link NodeIdentifierTestUtils} by unit tests; false to make accidental
     *        calls to it throw an exception
     */
    public static synchronized void resetTestNodeIdentifierService(boolean allowImplicitCreation) {
        sharedTestNodeIdentifierService = null;
        allowImplicitNodeIdServiceCreation = allowImplicitCreation;
    }

    /**
     * Test convenience method; when using this, {@link #removeTestNodeIdentifierServiceFromCurrentThread()} should typically be used as
     * well.
     */
    public static void attachTestNodeIdentifierServiceToCurrentThread() {
        NodeIdentifierService current = NodeIdentifierContextHolder.getRawDeserializationServiceForCurrentThread();
        if (current != null) {
            if (current != getTestNodeIdentifierService()) {
                throw new IllegalStateException("There is already another " + NodeIdentifierService.class.getSimpleName()
                    + " attached to the current test thread");
            } else {
                throw new IllegalStateException("The shared test " + NodeIdentifierService.class.getSimpleName()
                    + " has already been attached to the current test thread; "
                    + "most likely, a previous test did not call the clean up method after setting it");
            }
        }
        NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(getTestNodeIdentifierService());
    }

    /**
     * Test convenience method; should be called whenever {@link #attachTestNodeIdentifierServiceToCurrentThread()} was used, for example in
     * a test teardown method, to avoid accidentally polluting other tests.
     */
    public static void removeTestNodeIdentifierServiceFromCurrentThread() {
        NodeIdentifierService current = NodeIdentifierContextHolder.getRawDeserializationServiceForCurrentThread();
        if (current != null && current != getTestNodeIdentifierService()) {
            throw new IllegalStateException("Unexpected " + NodeIdentifierService.class.getSimpleName()
                + " found attached to the current test thread");
        }
        NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(null);
    }
}
