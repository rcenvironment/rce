/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import java.io.Serializable;
import java.util.Map;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.internal.AbstractNetworkMessage;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;

/**
 * Implementation of a transport-independent network response. Currently, such responses are exclusively sent in response to received
 * {@link NetworkRequest}s.
 * 
 * @author Robert Mischke
 */
public class NetworkResponseImpl extends AbstractNetworkMessage implements NetworkResponse, Serializable {

    /**
     * Response-specific metadata key for the result code.
     */
    public static final String METADATA_KEY_RESULT_CODE = "response.resultCode";

    /**
     * Cached string representation of the numeric {@link ResultCode#SUCCESS} value.
     */
    private static final String SUCCESS_CODE_STRING = Integer.toString(ResultCode.SUCCESS.getCode());

    // TODO made this class Serializable for quick prototyping; rework so this is not used anymore
    private static final long serialVersionUID = 5984970957378933267L;

    /**
     * Creates an instance with the given body and meta data.
     * 
     * @param body the response body to use
     * @param metaData the meta data to use
     */
    public NetworkResponseImpl(byte[] body, Map<String, String> metaData) {
        super(metaData);
        setContentBytes(body);
    }

    /**
     * Creates an instance with the given body and meta data.
     * 
     * @param body the response body to use
     * @param metaData the meta data to use
     * @throws SerializationException on serialization failure
     */
    @Deprecated
    public NetworkResponseImpl(Serializable body, Map<String, String> metaData) throws SerializationException {
        super(metaData);
        setContent(body);
    }

    /**
     * Creates an instance with empty metadata, except for the given request id.
     * 
     * @param body the response body
     * @param requestId the request id to set
     * @param resultCode the result code to set (as its numeric value)
     */
    public NetworkResponseImpl(byte[] body, String requestId, ResultCode resultCode) {
        super();
        setContentBytes(body);
        setRequestId(requestId);
        setResultCode(resultCode.getCode());
    }

    /**
     * Creates an instance with empty metadata, except for the given request id.
     * 
     * @param body the response body
     * @param requestId the request id to set
     * @param resultCode the result code to set
     * @throws SerializationException on serialization failure
     */
    @Deprecated
    public NetworkResponseImpl(Serializable body, String requestId, int resultCode) throws SerializationException {
        super();
        setContent(body);
        setRequestId(requestId);
        setResultCode(resultCode);
    }

    @Override
    public boolean isSuccess() {
        // optimized with a string constant as it is called frequently
        return SUCCESS_CODE_STRING.equals(metaDataWrapper.getValue(METADATA_KEY_RESULT_CODE));
    }

    @Override
    public ResultCode getResultCode() {
        try {
            return ResultCode.fromCode(Integer.parseInt(metaDataWrapper.getValue(METADATA_KEY_RESULT_CODE)));
        } catch (NumberFormatException e) {
            return ResultCode.UNDEFINED;
        } catch (IllegalArgumentException e) {
            return ResultCode.UNDEFINED;
        }
    }

    private void setResultCode(int code) {
        metaDataWrapper.setValue(METADATA_KEY_RESULT_CODE, Integer.toString(code));
    }

}
