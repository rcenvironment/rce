/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common.impl;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdType;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierContextHolder;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.model.NodeInformationRegistry;
import de.rcenvironment.core.communication.model.SharedNodeInformationHolder;
import de.rcenvironment.core.communication.model.internal.SharedNodeInformationHolderImpl;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.ConsistencyChecks;

/**
 * A {@link InstanceNodeSessionId} based on a persistent, random, UUID-like identifier. All identity-defining information is immutable; the
 * associated display name is not.
 * 
 * @author Robert Mischke
 */
public class NodeIdentifierImpl implements CommonIdBase, ResolvableNodeId, InstanceNodeId, InstanceNodeSessionId, LogicalNodeId,
    LogicalNodeSessionId {

    private static final long serialVersionUID = 5233747521769167502L;

    private static final String REGEXP_GROUP_SESSION_ID_PART = "([0-9a-f]{" + CommonIdBase.SESSION_PART_LENGTH + "})";

    private static final String REGEXP_GROUP_INSTANCE_ID_PART = "([0-9a-f]{" + CommonIdBase.INSTANCE_PART_LENGTH + "})";

    // note: this assumes that #DEFAULT_LOGICAL_NODE_PART is regexp-safe, ie only consists of regexp literals
    private static final String REGEXP_GROUP_LOGICAL_NODE_PART = "(" + CommonIdBase.DEFAULT_LOGICAL_NODE_PART + "|[0-9a-f]{1,"
        + CommonIdBase.MAXIMUM_LOGICAL_NODE_PART_LENGTH + "}" + ")";

    private static final Pattern INSTANCE_ID_STRING_PARSE_PATTERN =
        Pattern.compile(REGEXP_GROUP_INSTANCE_ID_PART);

    private static final Pattern INSTANCE_SESSION_ID_STRING_PARSE_PATTERN =
        Pattern.compile(REGEXP_GROUP_INSTANCE_ID_PART + STRING_FORM_PART_SEPARATOR + STRING_FORM_PART_SEPARATOR
            + REGEXP_GROUP_SESSION_ID_PART);

    private static final Pattern LOGICAL_NODE_ID_STRING_PARSE_PATTERN =
        Pattern.compile(REGEXP_GROUP_INSTANCE_ID_PART + STRING_FORM_PART_SEPARATOR + REGEXP_GROUP_LOGICAL_NODE_PART);

    private static final Pattern LOGICAL_NODE_SESSION_ID_STRING_PARSE_PATTERN =
        Pattern.compile(REGEXP_GROUP_INSTANCE_ID_PART + STRING_FORM_PART_SEPARATOR + REGEXP_GROUP_LOGICAL_NODE_PART
            + STRING_FORM_PART_SEPARATOR + REGEXP_GROUP_SESSION_ID_PART);

    private final IdType idType;

    private final String instanceIdPart;

    private final String sessionIdPart; // null for INSTANCE_ID type

    private final String logicalNodePart; // null for INSTANCE_ID or INSTANCE_SESSION_ID type

    private final String fullIdString; // cached concatenation of defining parts; always created, as it is needed for lookup anyway

    private final transient NodeInformationRegistry nodeInformationRegistry;

    private final transient SharedNodeInformationHolder nodeInformationHolder;

    private transient byte tempDeserializationTypeMarker; // not synchronized as it is used from a single thread only

    private transient String tempDeserializationString; // not synchronized as it is used from a single thread only

    /**
     * Constructor for the temporary instances used during deserialization; see {@link #readResolve()} implementation.
     */
    protected NodeIdentifierImpl() {
        this.idType = null;
        this.instanceIdPart = null;
        this.sessionIdPart = null;
        this.logicalNodePart = null;
        this.fullIdString = null;
        this.nodeInformationRegistry = null;
        this.nodeInformationHolder = null;
    }

    /**
     * Public string-parsing constructor that recreates a {@link NodeIdentifierImpl} instance from a string matching one of the string forms
     * returned by {@link #getFullIdString()}. TODO document patterns here or at that method
     * 
     * @param input the output-type-specific "full id string" to recreate the instance from
     * @param nodeInformationRegistry the {@link SharedNodeInformationHolder} to attach to the resulting object
     * @param expectedInterface the {@link InstanceNodeSessionId} subinterface determining the output object's properties and guarantees,
     *        which also defines the accepted input pattern
     * @throws IdentifierException on malformed input
     */
    public NodeIdentifierImpl(String input, NodeInformationRegistry nodeInformationRegistry,
        IdType type) throws IdentifierException {
        Objects.requireNonNull(input, "Cannot parse 'null' string to a node identifier"); // not allowed since 8.0
        this.idType = type;
        // interface-dependent parsing
        final Matcher matcher; // declared outside switch() due to Java scoping rules in combination with CheckStyle
        switch (type) {
        case INSTANCE_NODE_ID:
            matcher = matchRegexpOrFail(INSTANCE_ID_STRING_PARSE_PATTERN, input, idType);
            this.instanceIdPart = matcher.group(1); // always defined and of INSTANCE_ID_LENGTH
            this.logicalNodePart = null; // never defined
            this.sessionIdPart = null; // never defined
            break;
        case INSTANCE_NODE_SESSION_ID:
            matcher = matchRegexpOrFail(INSTANCE_SESSION_ID_STRING_PARSE_PATTERN, input, idType);
            this.instanceIdPart = matcher.group(1); // always defined and of INSTANCE_ID_LENGTH
            this.logicalNodePart = null; // never defined
            this.sessionIdPart = matcher.group(2); // always defined and of SESSION_PART_LENGTH
            break;
        case LOGICAL_NODE_ID:
            matcher = matchRegexpOrFail(LOGICAL_NODE_ID_STRING_PARSE_PATTERN, input, idType);
            this.instanceIdPart = matcher.group(1); // always defined and of INSTANCE_ID_LENGTH
            this.logicalNodePart = matcher.group(2); // always defined and of SESSION_PART_LENGTH
            this.sessionIdPart = null; // never defined
            break;
        case LOGICAL_NODE_SESSION_ID:
            matcher = matchRegexpOrFail(LOGICAL_NODE_SESSION_ID_STRING_PARSE_PATTERN, input, idType);
            this.instanceIdPart = matcher.group(1); // always defined and of INSTANCE_ID_LENGTH
            this.logicalNodePart = matcher.group(2); // always defined and either the default part string, or
                                                     // 1..MAXIMUM_LOGICAL_NODE_PART_LENGTH chars long
            this.sessionIdPart = matcher.group(3); // always defined and of SESSION_PART_LENGTH
            break;
        default:
            throw new RuntimeException("Unexpected id type requested for deserialization: " + idType);
        }

        // common fields; do this after parsing to not pollute the registry with potentially invalid keys
        this.fullIdString = input;
        this.nodeInformationRegistry = nodeInformationRegistry;
        this.nodeInformationHolder = selectAppropriateNodeInformationHolder();
        checkBasicInternalConsistency();
    }

    /**
     * Internal low-validation constructor that creates the new "full id string" from the provided id parts. Should be used when no fitting
     * "full id string" already exists.
     * 
     * @param instanceIdPart the instance id part to use
     * @param logicalNodePart the logical node part to use (may be null)
     * @param sessionIdPart the session id part to use (may be null)
     * @param nodeInformationRegistry the {@link NodeInformationRegistry} to use
     */
    protected NodeIdentifierImpl(String instanceIdPart, String logicalNodePart, String sessionIdPart,
        NodeInformationRegistry nodeInformationRegistry, IdType type) {
        this.instanceIdPart = instanceIdPart;
        this.sessionIdPart = sessionIdPart;
        this.logicalNodePart = logicalNodePart;
        this.nodeInformationRegistry = nodeInformationRegistry;
        this.idType = type;

        // note: not using StringUtils.format here as the benefit is unclear for these simple concatenations (using constants)
        switch (idType) {
        case INSTANCE_NODE_ID:
            this.fullIdString = instanceIdPart; // no separator
            break;
        case LOGICAL_NODE_ID:
            this.fullIdString =
                instanceIdPart + STRING_FORM_PART_SEPARATOR + this.logicalNodePart; // one separator
            break;
        case INSTANCE_NODE_SESSION_ID:
            this.fullIdString =
                instanceIdPart + STRING_FORM_PART_SEPARATOR + STRING_FORM_PART_SEPARATOR + sessionIdPart; // two separators
            break;
        case LOGICAL_NODE_SESSION_ID:
            this.fullIdString =
                instanceIdPart + STRING_FORM_PART_SEPARATOR + this.logicalNodePart + STRING_FORM_PART_SEPARATOR + sessionIdPart; // two s.
            break;
        default:
            throw new IllegalStateException();
        }

        this.nodeInformationHolder = selectAppropriateNodeInformationHolder();
        checkBasicInternalConsistency();
    }

    /**
     * Internal low-validation constructor. Should be used when a fitting "full id string" already exists to avoid repeated construction.
     * 
     * @param instanceIdPart the instance id part to use
     * @param logicalNodePart the logical node part to use (may be null)
     * @param sessionIdPart the session id part to use (may be null)
     * @param fullIdString the full id string to use
     * @param nodeInformationRegistry the {@link NodeInformationRegistry} to use
     */
    protected NodeIdentifierImpl(String instanceIdPart, String logicalNodePart, String sessionIdPart, String fullIdString,
        NodeInformationRegistry nodeInformationRegistry, IdType type) {
        this.instanceIdPart = instanceIdPart;
        this.sessionIdPart = sessionIdPart;
        this.logicalNodePart = logicalNodePart;
        this.fullIdString = fullIdString;
        this.nodeInformationRegistry = nodeInformationRegistry;
        this.idType = type;
        this.nodeInformationHolder = selectAppropriateNodeInformationHolder();
        checkBasicInternalConsistency();
    }

    @Override
    public IdType getType() {
        return idType;
    }

    /**
     * @return an opaque string representation that can be used to reconstruct the identifier object as accurately as possible
     */
    public String getFullIdString() {
        return fullIdString;
    }

    @Override
    public String getInstanceNodeIdString() {
        return instanceIdPart;
    }

    @Override
    public String getSessionIdPart() {
        return sessionIdPart;
    }

    @Override
    public String getLogicalNodePart() {
        return logicalNodePart;
    }

    @Override
    public String getInstanceNodeSessionIdString() {
        switch (idType) {
        case INSTANCE_NODE_SESSION_ID:
            return fullIdString;
        case LOGICAL_NODE_SESSION_ID:
            // convert to InstanceNodeSessionId string without creating the intermediate id object
            return instanceIdPart + STRING_FORM_PART_SEPARATOR + STRING_FORM_PART_SEPARATOR + sessionIdPart;
        default:
            throw newInvalidIdTypeForThisCallException();
        }
    }

    @Override
    public String getLogicalNodeIdString() {
        if (idType != IdType.LOGICAL_NODE_ID) {
            throw newInvalidIdTypeForThisCallException();
        }
        return fullIdString;
    }

    @Override
    public String getLogicalNodeSessionIdString() {
        if (idType != IdType.LOGICAL_NODE_SESSION_ID) {
            throw newInvalidIdTypeForThisCallException();
        }
        return fullIdString;
    }

    @Override
    public String getLogicalNodeRecognitionPart() {
        if (idType != IdType.LOGICAL_NODE_ID && idType != IdType.LOGICAL_NODE_SESSION_ID) {
            throw newInvalidIdTypeForThisCallException();
        }
        if (isTransientLogicalNode() || DEFAULT_LOGICAL_NODE_PART.equals(DEFAULT_LOGICAL_NODE_PART)) {
            return null;
        }
        // consistency check
        if (!logicalNodePart.startsWith(RECOGNIZABLE_LOGICAL_NODE_PART_PREFIX)) {
            throw new IllegalStateException("internal consistency error");
        }
        return logicalNodePart.substring(RECOGNIZABLE_LOGICAL_NODE_PART_PREFIX.length());
    }

    @Override
    public InstanceNodeId convertToInstanceNodeId() {
        switch (idType) {// the "from" type
        case INSTANCE_NODE_ID:
            return this;
        case INSTANCE_NODE_SESSION_ID:
        case LOGICAL_NODE_ID:
        case LOGICAL_NODE_SESSION_ID:
            return new NodeIdentifierImpl(instanceIdPart, null, null, nodeInformationRegistry, IdType.INSTANCE_NODE_ID);
        default:
            throw new IllegalArgumentException(idType.toString());
        }
    }

    @Override
    public InstanceNodeSessionId convertToInstanceNodeSessionId() {
        switch (idType) { // the "from" type
        case LOGICAL_NODE_SESSION_ID:
            return new NodeIdentifierImpl(instanceIdPart, null, sessionIdPart, nodeInformationRegistry,
                IdType.INSTANCE_NODE_SESSION_ID);
        default:
            throw newInvalidConversionException(idType, IdType.INSTANCE_NODE_SESSION_ID);
        }
    }

    @Override
    public LogicalNodeId convertToLogicalNodeId() {
        switch (idType) { // the "from" type
        case LOGICAL_NODE_SESSION_ID:
            return new NodeIdentifierImpl(instanceIdPart, logicalNodePart, null, nodeInformationRegistry,
                IdType.LOGICAL_NODE_ID);
        default:
            throw newInvalidConversionException(idType, IdType.LOGICAL_NODE_ID);
        }
    }

    @Override
    public LogicalNodeId convertToDefaultLogicalNodeId() {
        switch (idType) {// the "from" type
        case INSTANCE_NODE_ID:
        case INSTANCE_NODE_SESSION_ID:
        case LOGICAL_NODE_ID:
            return new NodeIdentifierImpl(instanceIdPart, DEFAULT_LOGICAL_NODE_PART, null, nodeInformationRegistry,
                IdType.LOGICAL_NODE_ID);
        case LOGICAL_NODE_SESSION_ID:
            // not providing for other types as "default" semantics would be confusing; add a new method if needed
            throw new RuntimeException("Invalid conversion attempt to 'default' logical node id: " + this);
        default:
            throw newInvalidConversionException(idType, IdType.LOGICAL_NODE_ID);
        }
    }

    @Override
    public LogicalNodeId expandToLogicalNodeId(String nodeIdPart) {
        if (idType != IdType.INSTANCE_NODE_ID) {
            throw newInvalidIdTypeForThisCallException();
        }
        return new NodeIdentifierImpl(instanceIdPart, nodeIdPart, null, nodeInformationRegistry, IdType.LOGICAL_NODE_ID);
    }

    @Override
    public LogicalNodeSessionId convertToDefaultLogicalNodeSessionId() {
        switch (idType) {// the "from" type
        case INSTANCE_NODE_SESSION_ID:
            return new NodeIdentifierImpl(instanceIdPart, DEFAULT_LOGICAL_NODE_PART, sessionIdPart, nodeInformationRegistry,
                IdType.LOGICAL_NODE_SESSION_ID);
        case LOGICAL_NODE_ID:
        case LOGICAL_NODE_SESSION_ID:
            // not providing for other types as "default" semantics would be confusing; add a new method if needed
            throw new RuntimeException("Invalid conversion attempt to 'default' logical node id: " + this);
        default:
            // not providing for other types as "default" semantics would be confusing; add a new method if needed
            throw newInvalidConversionException(idType, IdType.LOGICAL_NODE_SESSION_ID);
        }
    }

    @Override
    public LogicalNodeSessionId combineWithInstanceNodeSessionId(InstanceNodeSessionId instanceSessionId) {
        if (idType != IdType.LOGICAL_NODE_ID) {
            throw newInvalidIdTypeForThisCallException();

        }
        if (!this.getInstanceNodeIdString().equals(instanceSessionId.getInstanceNodeIdString())) {
            ConsistencyChecks.reportFailure("The ids to combine cannot refer to different instances! " + this + " / " + instanceSessionId);
        }
        // combine
        return new NodeIdentifierImpl(instanceIdPart, logicalNodePart, instanceSessionId.getSessionIdPart(), nodeInformationRegistry,
            IdType.LOGICAL_NODE_SESSION_ID);
    }

    @Override
    public boolean isTransientLogicalNode() {
        return logicalNodePart != null && logicalNodePart.startsWith(TRANSIENT_LOGICAL_NODE_PART_PREFIX);
    }

    @Override
    public boolean isSameInstanceNodeAs(ResolvableNodeId otherId) {
        if (otherId == null) {
            throw new NullPointerException("The id to compare " + this + " against can not be null");
        }
        final NodeIdentifierImpl otherIdImpl = (NodeIdentifierImpl) otherId;
        return instanceIdPart.equals(otherIdImpl.instanceIdPart);
    }

    @Override
    public boolean isSameInstanceNodeSessionAs(InstanceNodeSessionId otherId) {
        final NodeIdentifierImpl otherIdImpl = (NodeIdentifierImpl) otherId;
        ConsistencyChecks.assertTrue(
            this.idType == IdType.INSTANCE_NODE_SESSION_ID || this.idType == IdType.LOGICAL_NODE_SESSION_ID,
            "Tried to compare session identity from a non-session identifier");
        if (otherIdImpl.idType != IdType.INSTANCE_NODE_SESSION_ID) {
            ConsistencyChecks.reportFailure("Unexpected parameter type: " + otherIdImpl.idType);
        }
        // note: fields cannot be null after internal consistency checks
        return instanceIdPart.equals(otherIdImpl.instanceIdPart) && sessionIdPart.equals(otherIdImpl.sessionIdPart);
    }

    @Override
    public String getRawAssociatedDisplayName() {
        return nodeInformationHolder.getDisplayName();
    }

    @Override
    public String getAssociatedDisplayName() {
        String displayName = nodeInformationHolder.getDisplayName();
        if (displayName == null) {
            displayName = DEFAULT_DISPLAY_NAME;
        }
        // TODO (p1) 9.0.0: preliminary solution, sufficient as these ids are currently only used for SSH forwarding; generalize later
        if (logicalNodePart != null && !DEFAULT_LOGICAL_NODE_PART.equals(logicalNodePart)) {
            // custom logical node id -> attach logical node part to name
            String suffix = getLogicalNodeRecognitionPart();
            if (suffix == null) {
                suffix = logicalNodePart; // fallback for transient logical node ids; not used in 8.0.0, but be prepared for updates
            }
            return StringUtils.format("%s [SSH forwarding #%s]", displayName, suffix);
        } else {
            // default case, including instance node id types
            return displayName;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NodeIdentifierImpl) {
            return fullIdString.equals(((NodeIdentifierImpl) other).fullIdString);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fullIdString.hashCode();
    }

    @Override
    public String toString() {
        return StringUtils.format("\"%s\" [%s]", getAssociatedDisplayName(), fullIdString);
    }

    /**
     * Access method for unit tests.
     * 
     * @return the assigned {@link SharedNodeInformationHolderImpl}
     */
    protected SharedNodeInformationHolder getMetaInformationHolder() {
        return nodeInformationHolder;
    }

    /**
     * Centralizes the selection of the proper SharedNodeInformationHolder. Requires {@link #nodeInformationRegistry} and all id part fields
     * to be initialized.
     */
    private SharedNodeInformationHolder selectAppropriateNodeInformationHolder() {
        switch (idType) {
        case INSTANCE_NODE_ID:
        case LOGICAL_NODE_ID:
            return nodeInformationRegistry.getNodeInformationHolder(instanceIdPart);
        case INSTANCE_NODE_SESSION_ID:
        case LOGICAL_NODE_SESSION_ID:
            return nodeInformationRegistry.getNodeInformationHolder(getInstanceNodeSessionIdString());
        default:
            throw new IllegalArgumentException();
        }
    }

    // custom serialization hook; see JavaDoc of java.lang.Serializable
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // write type byte; chose this approach over a simply Enum field for efficiency
        out.writeByte(idType.ordinal());
        out.writeUTF(fullIdString);
    }

    // custom deserialization hook; see JavaDoc of java.lang.Serializable
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // read the serialized data and store it for the following readResolve() call that creates the immutable instance
        tempDeserializationTypeMarker = in.readByte();
        tempDeserializationString = in.readUTF();
    }

    // object rewriting hook called after deserialization (see JavaDoc of java.lang.Serializable);
    // required to properly set final/immutable fields (see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6379948)
    private Object readResolve() throws ObjectStreamException {
        if (tempDeserializationString == null) {
            throw new IllegalStateException("Expected transient deserialization data");
        }
        // TODO transitional; only needed until serialization is phased out
        NodeIdentifierImpl actualImmutableInstance;
        try {
            final IdType targetIdType = IdType.values()[tempDeserializationTypeMarker];
            final NodeIdentifierService service = NodeIdentifierContextHolder.getDeserializationServiceForCurrentThread();
            actualImmutableInstance = (NodeIdentifierImpl) service.parseSelectableTypeIdString(tempDeserializationString, targetIdType);
            // could also be added as to verbose logging
            // LogFactory.getLog(getClass()).debug("Deserialized id " + actualImmutableInstance);
        } catch (IdentifierException e) {
            throw new InvalidObjectException("Deserialization failure: " + e);
        }
        tempDeserializationString = null; // clear field as a consistency check
        return actualImmutableInstance;
    }

    private Matcher matchRegexpOrFail(Pattern pattern, String input, IdType type)
        throws IdentifierException {
        final Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            throw new IdentifierException("'" + input + "' cannot be parsed to a valid " + type);
        }
        return matcher;
    }

    private void checkBasicInternalConsistency() {
        final boolean isValid;
        final int fullIdLength = fullIdString.length();

        switch (idType) {
        case INSTANCE_NODE_ID:
            isValid =
                (instanceIdPart != null) && (logicalNodePart == null) && (sessionIdPart == null)
                    && (fullIdLength == INSTANCE_ID_STRING_LENGTH);
            break;
        case INSTANCE_NODE_SESSION_ID:
            isValid =
                (instanceIdPart != null) && (logicalNodePart == null) && (sessionIdPart != null)
                    && (fullIdLength == INSTANCE_SESSION_ID_STRING_LENGTH);
            break;
        case LOGICAL_NODE_ID:
            isValid =
                (instanceIdPart != null) && (logicalNodePart != null) && (sessionIdPart == null)
                    && (fullIdLength >= MINIMUM_LOGICAL_NODE_ID_STRING_LENGTH)
                    && (fullIdLength <= MAXIMUM_LOGICAL_NODE_ID_STRING_LENGTH);
            break;
        case LOGICAL_NODE_SESSION_ID:
            isValid =
                (instanceIdPart != null) && (logicalNodePart != null) && (sessionIdPart != null)
                    && (fullIdLength >= MINIMUM_LOGICAL_NODE_SESSION_ID_STRING_LENGTH)
                    && (fullIdLength <= MAXIMUM_LOGICAL_NODE_SESSION_ID_STRING_LENGTH);
            break;
        default:
            isValid = false;
        }
        if (!isValid) {
            throw new IllegalStateException("Internal id consistency error: " + this.toString());
        }
    }

    private RuntimeException newInvalidConversionException(IdType from, IdType to) {
        // these are non-handleable coding errors, so using a RTE is appropriate
        return new RuntimeException("Converting from " + from + " to " + to
            + " cannot be done without active resolution; see " + LiveNetworkIdResolutionService.class.getName());
    }

    private IllegalStateException newInvalidIdTypeForThisCallException() {
        return new IllegalStateException("Invalid id type " + idType + " for this call; id object: " + this.toString());
    }

}
