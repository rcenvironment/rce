/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.Serializable;

/**
 * This class represents identity information about an RCE node.
 * 
 * @author Robert Mischke
 */
public interface NodeIdentityInformation extends Serializable {

    /**
     * Returns the persistent globally unique identifier for the associated RCE platform. No two
     * platforms may use the same persistent identifier at any time. Data stored at an RCE platform
     * is expected to stay consistent as long as the persistent identifier remains the same.
     * 
     * The identifier (if it exists) must consist of 32 alphanumeric characters. Clients should not
     * make any assumptions about its content, but treat it as an opaque identifier instead.
     * 
     * @return the persistent identifier string
     */
    String getPersistentNodeId();

    /**
     * The X.509-encoded public key of this platform, if it exists.
     * 
     * @return the encoded public key, or null if no key exists
     */
    String getEncodedPublicKey();

    /**
     * The end-user display name of this platform. If no such name is defined, implementations
     * should return null. If set, the name should contain at least one printable character and be
     * free of surrounding whitespace. Its length must not exceed 128 unicode characters. (This
     * length restriction is set arbitrarily; it may be increased if necessary.)
     * 
     * @return the end-user display name, or null if no name is defined
     */
    String getDisplayName();

    /**
     * @return true if the local node is flagged to be a "workflow host"; TODO point to
     *         documentation/glossary
     * 
     *         TODO does not really belong in "basic" identify information; replace with distributed
     *         node metadata when available - misc_ro
     */
    boolean getIsWorkflowHost();
}
