/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Constants for standard keys and values of {@link NodeProperty}s.
 * 
 * @author Robert Mischke
 */
public final class NodePropertyConstants {

    /**
     * Property key for the {@link InstanceNodeSessionId} string of the publishing node.
     */
    public static final String KEY_NODE_ID = "nodeId";

    /**
     * Property key for the end-user display name of the publishing node.
     */
    public static final String KEY_DISPLAY_NAME = "displayName";

    /**
     * Property key for the millisecond timestamp (as returned by System.currentTimeMillis()) of
     * this node's startup time.
     */
    public static final String KEY_SESSION_START_TIME = "sessionStartTime";

    /**
     * Property key for the link state advertisement (routing information) of the publishing node.
     */
    public static final String KEY_LSA = "lsa";

    /**
     * Standard string representing a boolean "true".
     */
    public static final String VALUE_TRUE = "true";

    /**
     * Standard string representing a boolean "false".
     */
    public static final String VALUE_FALSE = "false";

    private NodePropertyConstants() {}

    /**
     * @param value a boolean value
     * @return the appropriate string representation
     */
    public static String wrapBoolean(boolean value) {
        if (value) {
            return VALUE_TRUE;
        } else {
            return VALUE_FALSE;
        }
    }

}
