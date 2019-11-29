/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess;

import java.util.Optional;

import de.rcenvironment.core.utils.common.SizeValidatedDataSource;

/**
 * Service for remote access on tools that are published via uplink connections. Registers each remote tool as a component on the local
 * node.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public interface UplinkToolAccessClientService {

    /**
     * Downloads the tool documentation for a given tool identifier and node id.
     * 
     * @param toolIdAndVersion the tool identifier/version string
     * @param nodeId the node id
     * @param hashValue the hash value of the documentation
     * @return The data source to read the documentation data from, or an empty {@link Optional} if no such documentation exists
     */
    Optional<SizeValidatedDataSource> downloadToolDocumentation(String toolIdAndVersion, String nodeId, String hashValue);

}
