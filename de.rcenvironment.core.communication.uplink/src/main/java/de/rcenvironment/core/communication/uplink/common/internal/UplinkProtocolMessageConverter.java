/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequestResponse;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationRequest;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationResponse;
import de.rcenvironment.core.communication.uplink.entities.FileHeader;
import de.rcenvironment.core.communication.uplink.entities.FileTransferSectionInfo;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationRequest;
import de.rcenvironment.core.communication.uplink.entities.ToolDocumentationResponse;
import de.rcenvironment.core.communication.uplink.network.channel.internal.ToolExecutionProviderEventTransferObject;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Converts between request/response Java objects and their {@link MessageBlock} byte array representations.
 *
 * @author Robert Mischke
 */
public class UplinkProtocolMessageConverter {

    private static final boolean ENABLE_DEBUG_OUTPUT = DebugSettings.getVerboseLoggingEnabled("uplink.conversions");

    private final ObjectMapper jsonMapper = JsonUtils.getDefaultObjectMapper();

    private final boolean debugOutputEnabled;

    private final Log log = LogFactory.getLog(getClass());

    private String logIdentity;

    private final TypeReference<HashMap<String, String>> typeReferenceStringKeyValueMap = new TypeReference<HashMap<String, String>>() {
    };

    public UplinkProtocolMessageConverter(String logIdentity) {
        this.debugOutputEnabled = ENABLE_DEBUG_OUTPUT;
        this.logIdentity = logIdentity;
    }

    /**
     * Encodes handshake data for sending in either direction.
     * 
     * @param dataMap the data to encode
     * @return the {@link MessageBlock} to send
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeHandshakeData(Map<String, String> dataMap) throws ProtocolException {
        return encodeJSONMessage(MessageType.HANDSHAKE, dataMap);
    }

    /**
     * Decodes handshake data.
     * 
     * @param handshakeData the message block to decode
     * @return the decoded data
     * @throws ProtocolException on failure
     */
    public Map<String, String> decodeHandshakeData(MessageBlock handshakeData) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.HANDSHAKE, typeReferenceStringKeyValueMap, handshakeData);
    }

    /**
     * Encodes a {@link MessageType#GOODBYE} message with an error string.
     * <p>
     * Note that unlike all other methods, this does not throw a {@link ProtocolException} to facilitate error handling code paths. If
     * encoding this message fails, something has gone really, really wrong anyway. -- misc_ro
     * 
     * @param message the error messages; should not be null or empty, although this is handled as a fallback
     * @return the {@link MessageBlock}
     */
    public MessageBlock encodeErrorGoodbyeMessage(String message) {
        if (StringUtils.isNullorEmpty(message)) {
            log.warn("Was requested to encode a null or empty error message; replacing with a placeholder message");
            message = "<no message available>";
        }
        try {
            return new MessageBlock(MessageType.GOODBYE.getCode(), message.getBytes(Charsets.UTF_8));
        } catch (ProtocolException e) {
            throw new IllegalStateException(
                "Unexpected internal error: Failed to encode a 'goodbye' error message; message parameter: " + message);
        }
    }

    /**
     * Converts a {@link ToolDescriptorListUpdate} into a {@link MessageBlock}.
     * 
     * @param data the {@link ToolDescriptorListUpdate}
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeToolDescriptorListUpdate(ToolDescriptorListUpdate data) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_DESCRIPTOR_LIST_UPDATE, data);
    }

    /**
     * Converts a {@link MessageBlock} into a {@link ToolDescriptorListUpdate}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolDescriptorListUpdate}
     * @throws ProtocolException on failure
     */
    public ToolDescriptorListUpdate decodeToolDescriptorListUpdate(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_DESCRIPTOR_LIST_UPDATE, ToolDescriptorListUpdate.class, messageBlock);
    }

    /**
     * Encodes a request for channel creation.
     * 
     * @param request the ChannelCreationRequest
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeChannelCreationRequest(ChannelCreationRequest request) throws ProtocolException {
        return encodeJSONMessage(MessageType.CHANNEL_INIT, request);
    }

    /**
     * Converts a {@link MessageBlock} into a {@link ChannelCreationRequest}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ChannelCreationRequest}
     * @throws ProtocolException on failure
     */
    public ChannelCreationRequest decodeChannelCreationRequest(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.CHANNEL_INIT, ChannelCreationRequest.class, messageBlock);
    }

    /**
     * Encodes a response for a channel creation request.
     * 
     * @param response the ChannelCreationResponse
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeChannelCreationResponse(ChannelCreationResponse response) throws ProtocolException {
        return encodeJSONMessage(MessageType.CHANNEL_INIT_RESPONSE, response);
    }

    /**
     * Decodes the final response for a channel creation request.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ChannelCreationResponse}
     * @throws ProtocolException on failure
     */
    public ChannelCreationResponse decodeChannelCreationResponse(MessageBlock messageBlock)
        throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.CHANNEL_INIT_RESPONSE, ChannelCreationResponse.class, messageBlock);
    }

    /**
     * Encodes a {@link ToolExecutionRequest}.
     * 
     * @param request the {@link ToolExecutionRequest}
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeToolExecutionRequest(ToolExecutionRequest request) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_EXECUTION_REQUEST, request);
    }

    /**
     * Decodes a {@link ToolExecutionRequest}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolExecutionRequest}
     * @throws ProtocolException on failure
     */
    public ToolExecutionRequest decodeToolExecutionRequest(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_EXECUTION_REQUEST, ToolExecutionRequest.class, messageBlock);
    }

    /**
     * Encodes a {@link ToolExecutionRequestResponse}.
     * 
     * @param request the {@link ToolExecutionRequestResponse}
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeToolExecutionRequestResponse(ToolExecutionRequestResponse request) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_EXECUTION_REQUEST_RESPONSE, request);
    }

    /**
     * Decodes a {@link ToolExecutionRequestResponse}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolExecutionRequestResponse}
     * @throws ProtocolException on failure
     */
    public ToolExecutionRequestResponse decodeToolExecutionRequestResponse(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_EXECUTION_REQUEST_RESPONSE, ToolExecutionRequestResponse.class, messageBlock);
    }

    /**
     * Encodes a {@link FileTransferSectionInfo} object into a {@link MessageType#FILE_TRANSFER_SECTION_START} message.
     * 
     * @param data the {@link FileTransferSectionInfo} object
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeFileTransferSectionStart(FileTransferSectionInfo data) throws ProtocolException {
        return encodeJSONMessage(MessageType.FILE_TRANSFER_SECTION_START, data);
    }

    /**
     * Extracts the {@link FileTransferSectionInfo} object of a {@link MessageType#FILE_TRANSFER_SECTION_START} message.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link FileTransferSectionInfo} object
     * @throws ProtocolException on failure
     */
    public FileTransferSectionInfo decodeFileTransferSectionStart(MessageBlock messageBlock) throws ProtocolException {
        final FileTransferSectionInfo decodedWrapper =
            validateAndDecodeJSONMessage(MessageType.FILE_TRANSFER_SECTION_START, FileTransferSectionInfo.class, messageBlock);
        if (decodedWrapper != null) {
            return decodedWrapper;
        } else {
            return new FileTransferSectionInfo(null); // return surrogate
        }
    }

    /**
     * Encodes a batch of {@link ToolExecutionProviderEventTransferObject}s.
     * 
     * @param batch the {@link ToolExecutionProviderEventTransferObject}s
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeToolExecutionEvents(List<ToolExecutionProviderEventTransferObject> batch) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_EXECUTION_EVENTS, batch);
    }

    /**
     * Decodes a batch of {@link ToolExecutionProviderEventTransferObject}s.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolExecutionProviderEventTransferObject}s
     * @throws ProtocolException on failure
     */
    public List<ToolExecutionProviderEventTransferObject> decodeToolExecutionEvents(MessageBlock messageBlock) throws ProtocolException {
        TypeReference<List<ToolExecutionProviderEventTransferObject>> typeRef =
            new TypeReference<List<ToolExecutionProviderEventTransferObject>>() {
            };
        return validateAndDecodeJSONMessage(MessageType.TOOL_EXECUTION_EVENTS, typeRef, messageBlock);
    }

    /**
     * Creates a signal to request the cancellation of a tool execution.
     * 
     * @return the generated {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock createToolCancellationRequest() throws ProtocolException {
        return new MessageBlock(MessageType.TOOL_CANCELLATION_REQUEST);
    }

    /**
     * Encodes a {@link ToolExecutionRequest} as a {@link MessageType#TOOL_EXECUTION_FINISHED} message.
     * 
     * @param executionResult the result of the tool execution
     * @return the generated {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeToolExecutionResult(ToolExecutionResult executionResult) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_EXECUTION_FINISHED, executionResult);
    }

    /**
     * Decodes a {@link MessageType#TOOL_EXECUTION_FINISHED} and extracts the embedded {@link ToolExecutionResult}.
     * 
     * @param messageBlock the extracted {@link ToolExecutionResult}
     * @return the {@link ToolExecutionRequestResponse}
     * @throws ProtocolException on failure
     */
    public ToolExecutionResult decodeToolExecutionResult(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_EXECUTION_FINISHED, ToolExecutionResult.class, messageBlock);
    }

    /**
     * Encodes a {@link FileHeader}.
     * 
     * @param header the {@link FileHeader}
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeFileHeader(FileHeader header) throws ProtocolException {
        return encodeJSONMessage(MessageType.FILE_HEADER, header);
    }

    /**
     * Decodes a {@link FileHeader}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link FileHeader}
     * @throws ProtocolException on failure
     */
    public FileHeader decodeFileHeader(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.FILE_HEADER, FileHeader.class, messageBlock);
    }

    /**
     * Encodes a {@link ToolDocumentationRequest}.
     * 
     * @param request the {@link ToolDocumentationRequest}
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeDocumentationRequest(ToolDocumentationRequest request) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_DOCUMENTATION_REQUEST, request);
    }

    /**
     * Decodes a {@link ToolDocumentationRequest}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolDocumentationRequest}
     * @throws ProtocolException on failure
     */
    public ToolDocumentationRequest decodeDocumentationRequest(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_DOCUMENTATION_REQUEST, ToolDocumentationRequest.class, messageBlock);
    }

    /**
     * Encodes a {@link ToolDocumentationResponse}.
     * 
     * @param response the ToolDocumentationResponse
     * @return the {@link MessageBlock}
     * @throws ProtocolException on failure
     */
    public MessageBlock encodeDocumentationResponse(ToolDocumentationResponse response) throws ProtocolException {
        return encodeJSONMessage(MessageType.TOOL_DOCUMENTATION_RESPONSE, response);
    }

    /**
     * Decodes a {@link ToolDocumentationResponse}.
     * 
     * @param messageBlock the {@link MessageBlock}
     * @return the {@link ToolDocumentationRequest}
     * @throws ProtocolException on failure
     */
    public ToolDocumentationResponse decodeDocumentationResponse(MessageBlock messageBlock) throws ProtocolException {
        return validateAndDecodeJSONMessage(MessageType.TOOL_DOCUMENTATION_RESPONSE, ToolDocumentationResponse.class, messageBlock);
    }

    private MessageBlock encodeJSONMessage(MessageType messageType, Object jsonData) throws ProtocolException {
        try {
            final byte[] jsonBytes = jsonMapper.writeValueAsBytes(jsonData);
            if (debugOutputEnabled) {
                log.debug(StringUtils.format("[%s] Encoded JSON message of type %s; output: '%s'", logIdentity, messageType,
                    jsonMapper.writeValueAsString(jsonData)));
            }
            return new MessageBlock(messageType.getCode(), jsonBytes);
        } catch (JsonProcessingException | ProtocolException e) {
            // wrap implementation detail exceptions
            throw new ProtocolException(
                StringUtils.format("Failed to encode JSON message of type %s: %s", messageType, e.toString()));
        }
    }

    // TODO see if anything can be done about this ugly code duplication

    private <T> T validateAndDecodeJSONMessage(MessageType expectedMessageType, Class<T> resultClass, MessageBlock messageBlock)
        throws ProtocolException {
        validateMessageType(expectedMessageType, messageBlock);
        try {
            if (debugOutputEnabled) {
                log.debug(
                    StringUtils.format("[%s] Decoding JSON message of type %s; input: '%s'", logIdentity, messageBlock.getType(),
                        new String(messageBlock.getData(), Charsets.UTF_8)));
            }
            return jsonMapper.readValue(messageBlock.getData(), resultClass);
        } catch (IOException e) {
            // wrap implementation detail exceptions
            throw new ProtocolException(
                StringUtils.format("Failed to decode JSON message of expected type %s: %s", expectedMessageType, e.toString()));
        }
    }

    private void validateMessageType(MessageType expectedMessageType, MessageBlock messageBlock) throws ProtocolException {
        if (messageBlock.getType() != expectedMessageType) {
            throw new ProtocolException("Internal error: Actual message code does not match the expected value");
        }
    }

    private <T> T validateAndDecodeJSONMessage(MessageType expectedMessageType, TypeReference<T> resultClass,
        MessageBlock messageBlock)
        throws ProtocolException {
        validateMessageType(expectedMessageType, messageBlock);
        try {
            if (debugOutputEnabled) {
                log.debug(
                    StringUtils.format("[%s] Decoding JSON message of type %s; input: '%s'", logIdentity, messageBlock.getType(),
                        new String(messageBlock.getData(), Charsets.UTF_8)));
            }
            return jsonMapper.readValue(messageBlock.getData(), resultClass);
        } catch (IOException e) {
            // wrap implementation detail exceptions
            throw new ProtocolException(
                "Failed to decode JSON message of expected type " + expectedMessageType + ": " + e.toString());
        }
    }

}
