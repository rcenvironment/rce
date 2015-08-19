/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.protocol;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.model.NetworkMessage;

/**
 * Semantic wrapper around the key-value metadata map of a {@link NetworkMessage}.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class MessageMetaData {

    private static final String KEY_MESSAGE_TYPE = "type";

    private static final String KEY_MESSAGEID = "id";

    private static final String KEY_SENDER = "src";

    private static final String KEY_FINAL_RECIPIENT = "dst";

    private static final String KEY_HOPCOUNT = "hopcount";

    private static final String KEY_TRACE = "trace";

    private Map<String, String> properties;

    private final Log log = LogFactory.getLog(MessageMetaData.class);

    public MessageMetaData() {
        this(new HashMap<String, String>());
    }

    public MessageMetaData(Map<String, String> metaData) {
        properties = metaData;
    }

    /**
     * @return An empty instance.
     */
    public static MessageMetaData create() {
        return new MessageMetaData();
    }

    /**
     * @param metaData The data structure.
     * @return The instance created from a given data structure.
     */
    public static MessageMetaData wrap(Map<String, String> metaData) {
        return new MessageMetaData(metaData);
    }

    /**
     * Factory method that clones the given map and wraps it into a {@link MessageMetaData}.
     * 
     * @param metaData the map to clone and wrap
     * @return the generated {@link MessageMetaData}
     */
    public static MessageMetaData cloneAndWrap(Map<String, String> metaData) {
        return new MessageMetaData(new HashMap<String, String>(metaData));
    }

    /**
     * Returns the top-level type of this message. Examples of such types are RCP, health check or LSA.
     * 
     * @return the String id of the message type
     */
    public String getMessageType() {
        return getValue(KEY_MESSAGE_TYPE);
    }

    /**
     * Sets the message type.
     * 
     * @param type the message type; should be one of the values in {@link ProtocolConstants}
     * @return itself (for chaining)
     */
    public MessageMetaData setMessageType(String type) {
        setValue(KEY_MESSAGE_TYPE, type);
        return this;
    }

    /**
     * Clones and returns the internal metadata map.
     * 
     * @return an independent clone of the internal map
     */
    public Map<String, String> cloneData() {
        Map<String, String> clone = new HashMap<String, String>();
        clone.putAll(properties);
        return clone;
    }

    /**
     * @return The map.
     */
    public Map<String, String> getInnerMap() {
        return properties;
    }

    /**
     * @param key The key.
     * @return The value assigned to the key.
     */
    public String getValue(String key) {
        return properties.get(key);
    }

    /**
     * TODO krol_ph: Enter comment!
     * 
     * @param key The map key.
     * @param value The map value.
     * @return Itself.
     */
    public MessageMetaData setValue(String key, String value) {
        properties.put(key, value);
        return this;
    }

    /**
     * Sets the final recipient of a message. Can be omitted for non-routed messages.
     * 
     * @param receiver The receiver {@link NodeIdentifier}
     * @return Itself.
     */
    public MessageMetaData setFinalRecipient(NodeIdentifier receiver) {
        setValue(KEY_FINAL_RECIPIENT, receiver.getIdString());
        return this;
    }

    /**
     * Sets the {@link #KEY_SENDER} field.
     * 
     * @param sender the sender to set
     * @return self
     */
    public MessageMetaData setSender(NodeIdentifier sender) {
        setValue(KEY_SENDER, sender.getIdString());
        return this;
    }

    /**
     * Sets the {@link #KEY_MESSAGEID} field.
     * 
     * @param id the message id to set
     * @return self
     */
    public MessageMetaData setMessageId(String id) {
        setValue(KEY_MESSAGEID, id);
        return this;
    }

    /**
     * Adds a route tracing step (the node ids of either the sender or a traversed node when forwarding).
     * 
     * @param newStep the step to append
     * @return Itself.
     */
    public MessageMetaData addTraceStep(String newStep) {
        String oldValue = getValue(KEY_TRACE);
        if (oldValue == null) {
            setValue(KEY_TRACE, newStep);
        } else {
            setValue(KEY_TRACE, oldValue + "," + newStep);
        }
        return this;
    }

    /**
     * Increment hop count to check for maximum time to live.
     * 
     * @return Itself.
     */
    public MessageMetaData incHopCount() {
        if (properties.containsKey(KEY_HOPCOUNT)) {
            setValue(KEY_HOPCOUNT, Integer.toString(getHopCount() + 1));
        } else {
            setValue(KEY_HOPCOUNT, "1");
        }
        return this;
    }

    /**
     * @return The trace.
     */
    public String getTrace() {
        return getValue(KEY_TRACE);
    }

    public String getFinalRecipientString() {
        return getValue(KEY_FINAL_RECIPIENT);
    }

    /**
     * @return The receiver.
     */
    public NodeIdentifier getFinalRecipient() {
        String idString = getFinalRecipientString();
        if (idString != null) {
            return NodeIdentifierFactory.fromNodeId(idString);
        } else {
            return null;
        }
    }

    public String getSenderIdString() {
        return getValue(KEY_SENDER);
    }

    /**
     * @return The sender.
     */
    public NodeIdentifier getSender() {
        String idString = getSenderIdString();
        if (idString != null) {
            return NodeIdentifierFactory.fromNodeId(idString);
        } else {
            // TODO review handling of sender field - misc_ro
            log.debug("Returning 'null' node id for empty 'sender' field; message type=" + getMessageType());
            return null;
        }
    }

    /**
     * @return true if a sender is defined
     */
    public boolean hasSender() {
        return getSenderIdString() != null;
    }

    /**
     * @return The hash.
     */
    public String getMessageId() {
        return getValue(KEY_MESSAGEID);
    }

    /**
     * @return The hop count.
     */
    public int getHopCount() {
        try {
            return Integer.parseInt(getValue(KEY_HOPCOUNT));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
