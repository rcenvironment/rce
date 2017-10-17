/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.common;

import java.util.Objects;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;

/**
 * Utility methods for handling node identifiers and related exception. All methods that require a {@link NodeIdentifierService} instance
 * call {@link NodeIdentifierContextHolder#getDeserializationServiceForCurrentThread()} implicitly.
 * 
 * @author Robert Mischke
 */
public final class NodeIdentifierUtils {

    private NodeIdentifierUtils() {}

    /**
     * Recreates a {@link InstanceNodeId} from the string representation returned by {@link InstanceNodeSessionId#getInstanceNodeIdString()}
     * .
     * 
     * @see NodeIdentifierService#parseInstanceNodeIdString(String)
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    public static InstanceNodeId parseInstanceNodeIdString(String input) throws IdentifierException {
        return getNodeIdentifierServiceForCurrentThread().parseInstanceNodeIdString(input);
    }

    /**
     * Transitional method; forwards to {@link #parseInstanceNodeIdString(String)}, but maps any {@link IdentifierException} to a
     * RuntimeException.
     * 
     * TODO >= 8.0.0: replace calls to this with proper exception handling
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     */
    public static InstanceNodeId parseInstanceNodeIdStringWithExceptionWrapping(String input) {
        try {
            return parseInstanceNodeIdString(input);
        } catch (IdentifierException e) {
            throw wrapIdentifierException(e);
        }
    }

    /**
     * Recreates a {@link InstanceSessionIdId} from the string representation returned by
     * {@link InstanceNodeSessionId#getInstanceNodeSessionIdString()}.
     * 
     * @see NodeIdentifierService#parseInstanceNodeSessionIdString(String)
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    public static InstanceNodeSessionId parseInstanceNodeSessionIdString(String input) throws IdentifierException {
        return getNodeIdentifierServiceForCurrentThread().parseInstanceNodeSessionIdString(input);
    }

    /**
     * Transitional method; forwards to {@link #parseInstanceNodeIdString(String)}, but maps any {@link IdentifierException} to a
     * RuntimeException.
     * 
     * TODO >= 8.0.0: replace calls to this with proper exception handling
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     */
    public static InstanceNodeSessionId parseInstanceNodeSessionIdStringWithExceptionWrapping(String input) {
        try {
            return parseInstanceNodeSessionIdString(input);
        } catch (IdentifierException e) {
            throw wrapIdentifierException(e);
        }
    }

    /**
     * Recreates a {@link LogicalNodeId} from the string representation returned by {@link LogicalNodeSessionId#getLogicalNodeIdString()}.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    public static LogicalNodeId parseLogicalNodeIdString(String input) throws IdentifierException {
        return getNodeIdentifierServiceForCurrentThread().parseLogicalNodeIdString(input);
    }

    /**
     * As {@link #parseLogicalNodeIdString(String)}, but with implicit exception wrapping until migration is complete.
     * 
     * TODO >= 8.0.0: replace calls to this with proper exception handling
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     */
    public static LogicalNodeId parseLogicalNodeIdStringWithExceptionWrapping(String input) {
        try {
            return parseLogicalNodeIdString(input);
        } catch (IdentifierException e) {
            throw wrapIdentifierException(e);
        }
    }

    /**
     * Special backwards compatibility method. Behaves as {@link #parseLogicalNodeIdString()}, but also accepts other id strings, which are
     * mapped to the default {@link LogicalNodeId} for that {@link InstanceNodeId}.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    public static LogicalNodeId parseArbitraryIdStringToLogicalNodeId(String input) throws IdentifierException {
        // TODO (p2) move method into service?
        Objects.requireNonNull(input, "Cannot parse 'null' string to a node identifier"); // null ids are not allowed since 8.0
        final int length = input.length();
        if (length == CommonIdBase.INSTANCE_ID_STRING_LENGTH) {
            return parseInstanceNodeIdString(input).convertToDefaultLogicalNodeId();
        } else if (length == CommonIdBase.INSTANCE_SESSION_ID_STRING_LENGTH) {
            return parseInstanceNodeSessionIdString(input).convertToDefaultLogicalNodeId();
        } else {
            // logical node or logical node session id; determine by looking at the separator positions
            int pos = input.lastIndexOf(CommonIdBase.STRING_FORM_PART_SEPARATOR);
            if (pos == CommonIdBase.INSTANCE_ID_STRING_LENGTH) {
                // only one separator; the one after the instance part -> logical node id
                return parseLogicalNodeIdString(input);
            } else {
                if (pos < 0) {
                    throw new IdentifierException("Unexpected state while trying to parse arbitrary id string '" + input + "'");
                }
                // by exclusion, assuming a logical node id now
                return parseLogicalNodeSessionIdString(input).convertToLogicalNodeId();
            }
        }
    }

    /**
     * As {@link #parseArbitraryIdStringToLogicalNodeId(String)}, but with implicit exception wrapping until migration is complete.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     */
    public static LogicalNodeId parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(String input) {
        try {
            return parseArbitraryIdStringToLogicalNodeId(input);
        } catch (IdentifierException e) {
            throw wrapIdentifierException(e);
        }
    }

    /**
     * Recreates a {@link LogicalNodeSessionId} from the string representation returned by
     * {@link LogicalNodeSessionId#getLogicalNodeSessionIdString()}.
     * 
     * @param input the string representation to reconstruct the object from
     * @return the reconstructed object, if successful
     * @throws IdentifierException on failure, typically because of a null or malformed string representation
     */
    public static LogicalNodeSessionId parseLogicalNodeSessionIdString(String input) throws IdentifierException {
        return getNodeIdentifierServiceForCurrentThread().parseLogicalNodeSessionIdString(input);
    }

    /**
     * Central method to wrap {@link IdentifierException}s as {@link RuntimeException}s in places where proper exception handling is not
     * currently possible (as the old code did not cause such exceptions, instead failing silently or with undefined behavior). Obviously,
     * calls to this method should be eliminated better sooner than later.
     * 
     * @param e the exception to wrap
     * @return the wrapping {@link RuntimeException}
     */
    public static RuntimeException wrapIdentifierException(IdentifierException e) {
        LogFactory.getLog(NodeIdentifierUtils.class).error("Wrapping IdentifierException for backwards compatibility", e);
        return new RuntimeException("Wrapping identifier exception for backwards compatibility", e);
    }

    private static NodeIdentifierService getNodeIdentifierServiceForCurrentThread() {
        return NodeIdentifierContextHolder.getDeserializationServiceForCurrentThread();
    }
}
