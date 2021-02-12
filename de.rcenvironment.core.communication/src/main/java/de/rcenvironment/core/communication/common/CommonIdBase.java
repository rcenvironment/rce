/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import java.io.Serializable;

/**
 * An abstract identifier for nodes within a distributed network.
 * <p>
 * The current core of all identifiers is an <b>instance id</b> (currently randomly generated, 128 bit, represented as a 32 character hex
 * string), which is used as the quasi-unique marker for a given <b>instance</b>. The definition of an instance is intentionally left
 * abstract. The only common expectations are that different instances do not intentionally try use the same instance identifier, and that
 * restarting such an instance maintains its instance id. When an instance id changes, all application code should consider the represented
 * node as a new, formerly unknown instance.
 * <p>
 * With instance ids alone, there are two issues in a distributed system: handling (unintentional or malicious) instance id collisions, and
 * detecting instance restarts to prevent wrong assumptions about a remote instance's internal state. Both are addressed by adding a
 * <b>session id part</b> to the identifier. An instance id and a session id part together define an <b>instance session id</b>.
 * <p>
 * Collision detection: If two nodes with the same instance id but different session id parts are present in a network at the same time,
 * application code should assume that the instance id was unintentionally reused/copied between those instances. For this reason, Session
 * id parts are expected to be non-persistent and randomly generated on instance startup. (Note that additional measures are needed to
 * protect against malicious id collisions, but session ids are still useful in this use case, for example by requiring them to be
 * cryptographically signed by the instance.)
 * 
 * @author Robert Mischke
 */
public interface CommonIdBase extends Serializable {

    /**
     * The string (usually a single character) used to separate parts in the single-string representation of all id subclasses.
     */
    String STRING_FORM_PART_SEPARATOR = ":"; // note: must be safe for inclusion in regular expressions

    /**
     * The length, in characters, of the (mandatory) "instance id" part of any subclass implementing this interface.
     */
    int INSTANCE_PART_LENGTH = 32;

    /**
     * The length, in characters, of any non-null "session id part" of any subclass implementing this interface. Note that this part is
     * optional by default, but certain subinterfaces may make it mandatory as part of their external contract.
     */
    int SESSION_PART_LENGTH = 10;

    /**
     * The maximum length, in characters, of any "logical node qualifier", which is the "logical node id part" without the trailing type
     * identifier, which is a single character. Consequently, the maximum logical node id part length is this value plus one for the type
     * identifier.
     */
    int MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH = 32;

    /**
     * The maximum length, in characters, of any non-null, non-default "logical node id part" of any subclass implementing this interface.
     * Note that this part is optional by default, but certain subinterfaces may make it mandatory as part of their external contract.
     * <p>
     * The "+1" represents the trailing type identifier which is added to the actual "qualifier". The type identifier is currently only used
     * to distinguish between transient and recognizable logical node ids, but more types may be added in the future.
     */
    int MAXIMUM_LOGICAL_NODE_PART_LENGTH = MAXIMUM_LOGICAL_NODE_QUALIFIER_LENGTH + 1;

    /**
     * Derived total length of an {@link InstanceNodeId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int INSTANCE_ID_STRING_LENGTH = INSTANCE_PART_LENGTH;

    /**
     * Derived total length of an {@link InstanceNodeSessionId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int INSTANCE_SESSION_ID_STRING_LENGTH = INSTANCE_PART_LENGTH + 2 + SESSION_PART_LENGTH;

    /**
     * Derived length property of an {@link LogicalNodeId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int MINIMUM_LOGICAL_NODE_ID_STRING_LENGTH = INSTANCE_PART_LENGTH + 1;

    /**
     * Derived length property of an {@link LogicalNodeId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int MAXIMUM_LOGICAL_NODE_ID_STRING_LENGTH = INSTANCE_PART_LENGTH + 2 + MAXIMUM_LOGICAL_NODE_PART_LENGTH;

    /**
     * Derived length property of an {@link LogicalNodeSessionId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int MINIMUM_LOGICAL_NODE_SESSION_ID_STRING_LENGTH = INSTANCE_PART_LENGTH + 2 + SESSION_PART_LENGTH;

    /**
     * Derived length property of an {@link LogicalNodeSessionId}'s string form; provided here to centralize test and parsing assumptions.
     */
    int MAXIMUM_LOGICAL_NODE_SESSION_ID_STRING_LENGTH = INSTANCE_PART_LENGTH + 2 + MAXIMUM_LOGICAL_NODE_PART_LENGTH + SESSION_PART_LENGTH;

    /**
     * The default logical node part used to derive the default logical node id from a given instance session id. Note that this default
     * part may be shorter than {@link #MAXIMUM_LOGICAL_NODE_PART_LENGTH}, but must not be longer.
     */
    String DEFAULT_LOGICAL_NODE_PART = "0"; // note: string must be safe for inclusion in regular expressions

    /**
     * The prefix of all persistent/recognizable logical node parts.
     */
    String RECOGNIZABLE_LOGICAL_NODE_PART_PREFIX = "r";

    /**
     * The prefix of all transient logical node parts.
     */
    String TRANSIENT_LOGICAL_NODE_PART_PREFIX = "t";

    /**
     * The default display name that is returned by {@link InstanceNodeSessionId#getAssociatedDisplayName()} if no name was associated with
     * that id using {@link #associateDisplayName(InstanceNodeSessionId, String)} yet.
     */
    String DEFAULT_DISPLAY_NAME = "<unknown>";

    /**
     * The suffix that is attached to an instance's current session name when a name resolution is requested for an older session of that
     * instance.
     */
    String DISPLAY_NAME_SUFFIX_FOR_OUTDATED_SESSIONS = " <outdated session>";

    /**
     * Convenience method for acquiring the display name associated with the identified node.
     * 
     * @return the display name associated with the node, or null if no display name is available/known
     */
    String getRawAssociatedDisplayName();

    /**
     * Convenience method for acquiring the display name associated with the identified node.
     * 
     * @return the display name associated with the node, or {@link #DEFAULT_DISPLAY_NAME} if no display name is available/known
     */
    String getAssociatedDisplayName();

}
