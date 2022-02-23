/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import java.util.Map;

import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.internal.AbstractNetworkMessage;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;

/**
 * Implementation of a transport-independent network response. Currently, such responses are exclusively sent in response to received
 * {@link NetworkRequest}s.
 * 
 * Payload content rules:
 * <ul>
 * <li>If the result code is SUCCESS, the payload is specific for the message type, and should not be interpreted by code that does not
 * understand the message type.
 * <li>If the result code is not SUCCESS, then the payload is either a serialized Java string containing additional error information, or
 * null. This text is typically included in an error message on the calling side.
 * </ul>
 * 
 * @author Robert Mischke
 */
public class NetworkResponseImpl extends AbstractNetworkMessage implements NetworkResponse {

    /**
     * Response-specific metadata key for the result code.
     */
    public static final String METADATA_KEY_RESULT_CODE = "response.resultCode";

    /**
     * Cached string representation of the numeric {@link ResultCode#SUCCESS} value.
     */
    private static final String SUCCESS_CODE_STRING = Integer.toString(ResultCode.SUCCESS.getCode());

    /**
     * Creates an instance with empty metadata, except for the given request id. This is the constructor for most use cases, and is
     * typically called via {@link NetworkResponseFactory}.
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
     * Creates an instance with the given body and meta data; only used by test code (including the "virtual" network transport, which
     * clones messages to detached them).
     * 
     * @param body the response body to use
     * @param metaData the meta data to use
     */
    public NetworkResponseImpl(byte[] body, Map<String, String> metaData) {
        super(metaData);
        setContentBytes(body);
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
