/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierImpl;

/**
 * A minimal interface for resolving internal node identifiers to user-assigned display names.
 *
 * @author Robert Mischke
 */
public interface NodeNameResolver {

    /**
     * Requests the best known name resolution for the given id.
     * 
     * @param nodeId the id to resolve the name for
     * @param replaceNullWithDefaultName if no name data is available, {@link CommonIdBase#DEFAULT_DISPLAY_NAME} is returned if this is
     *        true; if this is false, null is returned
     * @return the resolved name, or one of the placeholders (see parameter description) if none is available
     */
    String getDisplayNameForNodeId(NodeIdentifierImpl nodeId, boolean replaceNullWithDefaultName);
}
