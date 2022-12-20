/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import java.io.PrintStream;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdType;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

/**
 * A service for creating, parsing, and converting instance, instance session, and logical node ids.
 * 
 * @author Robert Mischke
 */
public interface NodeIdentifierService {

    /**
     * Creates a new random {@link InstanceNodeId}.
     * 
     * @return the new instance
     */
    InstanceNodeId generateInstanceNodeId();

    /**
     * Creates a new {@link InstanceNodeSessionId} based on the provided {@link InstanceNodeId}, adding a random session id part.
     * 
     * @param instanceId the {@link InstanceNodeId} to create the session id for
     * @return the new instance
     */
    InstanceNodeSessionId generateInstanceNodeSessionId(InstanceNodeId instanceId);

    /**
     * Constructs an new {@link InstanceNodeId} from the string returned by either {@link InstanceNodeId#getFullIdString()} or a custom
     * method of a subinterface of {@link CommonIdBase}.
     * 
     * @param instanceIdString the full string representation of an {@link InstanceNodeId}; see the main JavaDoc for the methods to acquire
     *        it
     * @param targetIdType the id type to parse the given string as
     * @return the new instance
     * 
     * @throws IdentifierException on invalid input
     */
    CommonIdBase parseSelectableTypeIdString(String instanceIdString, IdType targetIdType) throws IdentifierException;

    /**
     * Recreates a {@link InstanceNodeId} from the string representation returned by {@link InstanceNodeSessionId#getInstanceNodeIdString()}
     * .
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    InstanceNodeId parseInstanceNodeIdString(String input) throws IdentifierException;

    /**
     * Recreates a {@link InstanceNodeSessionId} from the string returned by {@link InstanceNodeSessionId#getInstanceNodeSessionIdString()}.
     * 
     * @param instanceSessionIdString the full string representation of an {@link InstanceNodeId}; see the main JavaDoc for the methods to
     *        acquire it
     * @return the new instance
     * 
     * @throws IdentifierException on invalid input
     */
    InstanceNodeSessionId parseInstanceNodeSessionIdString(String instanceSessionIdString) throws IdentifierException;

    /**
     * Recreates a {@link LogicalNodeId} from the string representation returned by {@link LogicalNodeSessionId#getLogicalNodeIdString()}.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    LogicalNodeId parseLogicalNodeIdString(String input) throws IdentifierException;

    /**
     * Recreates a {@link LogicalNodeSessionId} from the string representation returned by
     * {@link LogicalNodeSessionId#getLogicalNodeSessionIdString()}.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    LogicalNodeSessionId parseLogicalNodeSessionIdString(String input) throws IdentifierException;

    /**
     * Performs a similar operation as {@link #associateDisplayName(InstanceNodeSessionId, String)}, but sets a hardcoded default display
     * name, and the call comes with the added context information that the node in question is the local node. The implementation can use
     * this to adapt accordingly.
     * 
     * @param localInstanceSessionId the instance session id of the local node
     */
    void setDefaultDisplayNameForLocalNode(InstanceNodeSessionId localInstanceSessionId);

    /**
     * Sets the display name to associate with the given {@link InstanceNodeSessionId}. If the specified session is the most "recent" one,
     * it will be considered as the canonical session for the given instance, and the given name will also propagate as the default display
     * name for that instance.
     * <p>
     * Display names are always shared between all instances with the same internal canonical string representation.
     * 
     * @param id the id to set the display name for
     * @param displayName the display name to set
     */
    void associateDisplayName(InstanceNodeSessionId id, String displayName);

    /**
     * Sets the display name to associate with the given {@link LogicalNodeSessionId}. Note that this assignment will be ignored if the
     * session that this {@link LogicalNodeSessionId} belongs to is not the "current" session (i.e. the one with the most "recent" id
     * encountered) that has been registered at this service so far.
     * <p>
     * Display names are always shared between all instances with the same internal canonical string representation.
     * 
     * @param id the id to set the display name for
     * @param displayName the display name to set
     */
    void associateDisplayNameWithLogicalNode(LogicalNodeSessionId id, String displayName);

    /**
     * Debug method; prints all registered id-to-displayName associations.
     * 
     * @param output the output to write to
     * @param introText an optional line to be printered before the output; set to null to disable
     */
    void printAllNameAssociations(PrintStream output, String introText);

}
