/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

/**
 * Factory interface for generating unique connection ids.
 * 
 * The design rationale is that (a) only successful {@link MessageChannelIdFactory}s should trigger
 * an id generation, (b) transports provide their own connection implementations, and (c) transports
 * should be able to make connection objects immutable. This is best achieved by passing an id
 * factory to the transport implementations.
 * 
 * @author Robert Mischke
 */
public interface MessageChannelIdFactory {

    /**
     * Generates a new, unique id. Uniqueness is only required within the same JVM. All
     * implementations must be thread-safe.
     * 
     * @param selfInitiated whether the (logical) connection resulted from a low-level connection
     *        initiated by the local node
     * 
     * @return the new id
     */
    String generateId(boolean selfInitiated);
}
