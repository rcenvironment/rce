/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import de.rcenvironment.core.communication.common.impl.NodeIdentifierImpl;

/**
 * Factory class to encapsulate the creation of {@link NodeIdentifier}s.
 * 
 * @author Robert Mischke
 */
public final class NodeIdentifierFactory {

    private NodeIdentifierFactory() {
        // prevent instantiation
    }

    /**
     * Creates a {@link NodeIdentifier} from a host-and-number string, with an optional end-user
     * name. Input examples: "127.0.0.1:2", "127.0.0.1:5 (Test Platform)".
     * 
     * @param hostAndNumberString the platform definition string (see class JavaDoc)
     * @return a new {@link NodeIdentifier}
     */
    @Deprecated
    public static NodeIdentifier fromHostAndNumberString(String hostAndNumberString) {
        // backwards compatibility code
        return fromNodeId(hostAndNumberString);
    }

    /**
     * Creates a {@link NodeIdentifier} from separate host and number parameters, with an empty
     * end-user name.
     * 
     * @param host the String representation (IPv4 number or name) of the host
     * @param platformNumber the platform number
     * @return a new {@link NodeIdentifier}
     */
    @Deprecated
    public static NodeIdentifier fromHostAndNumber(String host, Integer platformNumber) {
        // backwards compatibility code
        return fromNodeId(host + ":" + platformNumber);
    }

    /**
     * Creates a {@link NodeIdentifier} from a persistent platform id.
     * 
     * @param id the persistent id to use
     * @return a new {@link NodeIdentifier}
     */
    public static NodeIdentifier fromNodeId(String id) {
        if (id == null) {
            throw new NullPointerException();
        }
        // TODO warn/fail on empty string?
        return new NodeIdentifierImpl(id);
    }

}
