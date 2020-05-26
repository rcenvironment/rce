/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Encapsulates conversions between network/node ids and their data management string representations.
 * 
 * @author Robert Mischke
 */
public final class DataManagementIdMapping {

    private DataManagementIdMapping() {}

    /**
     * Determines the string to reference a node/instance within the data management (the logical node on which a component 
     * is running).
     * 
     * @param logicalNodeId the id of the node to reference
     * @return the string to store in the data management
     */
    public static String mapLogicalNodeIdToDbString(LogicalNodeId logicalNodeId) {
        return logicalNodeId.getLogicalNodeIdString();
    }

    /**
     * Generates a string representation of a random node/instance for unit/integration testing.
     * 
     * @return the generated id
     */
    public static String createDummyNodeIdStringForTesting() {
        return NodeIdentifierTestUtils.createTestDefaultLogicalNodeId().getLogicalNodeIdString();
    }
}
