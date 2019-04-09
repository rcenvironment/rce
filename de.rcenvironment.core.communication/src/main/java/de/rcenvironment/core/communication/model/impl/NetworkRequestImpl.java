/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import java.io.Serializable;
import java.util.Map;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.internal.AbstractNetworkMessage;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Implementation of a transport-independent network request.
 * 
 * @author Robert Mischke
 */
public class NetworkRequestImpl extends AbstractNetworkMessage implements NetworkRequest, Serializable {

    // TODO made this class Serializable for quick prototyping; rework so this is not used anymore
    private static final long serialVersionUID = 1608492229624555125L;

    public NetworkRequestImpl(byte[] contentBytes, Map<String, String> metaData) {
        this(contentBytes, metaData, generateMessageId());
    }

    // TODO comment: parameters are wrapped, not cloned
    public NetworkRequestImpl(Serializable body, Map<String, String> metaData) throws SerializationException {
        this(body, metaData, generateMessageId());
    }

    // TODO comment: parameters are wrapped, not cloned
    public NetworkRequestImpl(byte[] contentBytes, Map<String, String> metaData, String requestId) {
        super(metaData);
        setContentBytes(contentBytes);
        setRequestId(requestId);
    }

    // TODO comment: parameters are wrapped, not cloned
    public NetworkRequestImpl(Serializable body, Map<String, String> metaData, String requestId) throws SerializationException {
        super(metaData);
        setContent(body);
        setRequestId(requestId);
    }

    private static String generateMessageId() {
        return IdGenerator.fastRandomHexString(REQUEST_ID_LENGTH);
    }

}
