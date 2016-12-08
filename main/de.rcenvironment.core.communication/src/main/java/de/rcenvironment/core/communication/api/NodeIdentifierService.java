/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
     * Sets the display name to associate with the given id, which can be of any subtype of {@link CommonIdBase}; note that certain subtypes
     * may cause the same display name to be implicitly associated for other ids as well. The given display name is shared between all
     * instances with the same internal canonical string representation.
     * 
     * @param id the id to set the display name for
     * @param displayName the display name to set
     */
    void associateDisplayName(CommonIdBase id, String displayName);

    /**
     * Debug method; prints all registered id-to-displayName associations.
     * 
     * @param output the output to write to
     * @param introText an optional line to be printered before the output; set to null to disable
     */
    void printAllNameAssociations(PrintStream output, String introText);

}
