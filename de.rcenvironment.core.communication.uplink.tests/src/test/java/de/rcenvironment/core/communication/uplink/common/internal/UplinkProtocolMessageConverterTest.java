/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * {@link UplinkProtocolMessageConverter} tests.
 *
 * @author Robert Mischke
 * @author Brigitte Boden (added tests for tool execution requests)
 */
public class UplinkProtocolMessageConverterTest {

    private final UplinkProtocolMessageConverter converter = new UplinkProtocolMessageConverter("unit test");

    private final ObjectMapper jsonMapper = JsonUtils.getDefaultObjectMapper();

    /**
     * Encode/decode test.
     * 
     * @throws ProtocolException on failure
     */
    @Test
    public void emptyToolDescriptorListUpdate() throws ProtocolException {
        final ToolDescriptorListUpdate original = new ToolDescriptorListUpdate("dummySource", "dummyName", new ArrayList<>());
        final MessageBlock encoded = converter.encodeToolDescriptorListUpdate(original);
        final ToolDescriptorListUpdate restoredCopy = converter.decodeToolDescriptorListUpdate(encoded);
        assertEquals(original.getDestinationId(), restoredCopy.getDestinationId());
        // the restored list should not be the exact same object
        assertFalse(original.getToolDescriptors() == restoredCopy.getToolDescriptors());
        // compare list content (empty)
        assertArrayEquals(original.getToolDescriptors().toArray(), restoredCopy.getToolDescriptors().toArray());
    }

    /**
     * Encode/decode test.
     * 
     * @throws ProtocolException on failure
     */
    @Test
    public void filledToolDescriptorListUpdate() throws ProtocolException {
        final ArrayList<ToolDescriptor> originalToolDescriptors = new ArrayList<>();
        originalToolDescriptors.add(new ToolDescriptor("tId", "tVer", new HashSet<String>(Arrays.asList("groupId")), "hash",
            new JsonDataWithOptionalEncryption("serialized", new HashMap<String, String>())));
        final ToolDescriptorListUpdate original = new ToolDescriptorListUpdate("dummySource", "dummyName", originalToolDescriptors);
        final MessageBlock encoded = converter.encodeToolDescriptorListUpdate(original);
        final ToolDescriptorListUpdate restoredCopy = converter.decodeToolDescriptorListUpdate(encoded);
        assertEquals(original.getDestinationId(), restoredCopy.getDestinationId());
        // the restored list should not be the exact same object
        assertFalse(original.getToolDescriptors() == restoredCopy.getToolDescriptors());
        // compare list content
        assertArrayEquals(original.getToolDescriptors().toArray(), restoredCopy.getToolDescriptors().toArray());
    }

    /**
     * Tests if basic encoding and decoding of a tool execution request works without exceptions.
     * 
     * @throws ProtocolException
     */
    @Test
    public void encodeAndDecodeToolExecutionRequest() throws ProtocolException {
        ToolExecutionRequest request =
            new ToolExecutionRequest("testName", "testVersion", "testId", "testDestination", new HashSet<String>(),
                new HashSet<Map<String, Object>>(), new HashSet<Map<String, Object>>(), new HashMap<String, String>());
        MessageBlock encoded = converter.encodeToolExecutionRequest(request);
        converter.decodeToolExecutionRequest(encoded);
    }

    /**
     * Ensures that the encoded message for a tool execution request does not contain the element "isMockMode".
     * (cf. Mantis ID 17273)
     * 
     * @throws ProtocolException
     * @throws JsonProcessingException
     */
    @Test
    public void encodeToolExecutionRequest() throws ProtocolException, JsonProcessingException {
        ToolExecutionRequest request =
            new ToolExecutionRequest("testName", "testVersion", "testId", "testDestination", new HashSet<String>(),
                new HashSet<Map<String, Object>>(), new HashSet<Map<String, Object>>(), new HashMap<String, String>());
        String jsonString = jsonMapper.writeValueAsString(request);
        assertFalse(jsonString.contains("isMockMode"));
    }

    /**
     * Checks if an encoded message from an older RCE version where the "isMockMode" element is contained can be decoded without causing an
     * exception.
     * (cf. Mantis ID 17273)
     * 
     * @throws ProtocolException
     * @throws JsonProcessingException
     */
    @Test
    public void decodeToolExecutionRequest() throws ProtocolException, JsonProcessingException {
        String jsonStringWithMockmode =
            "{\"toolId\":\"textName\",\"toolVersion\":\"1.0\",\"authGroupId\":\"public\",\"destinationId\":\"testDestination\",\"nonRequiredInputs\":[],\"dynamicInputs\":[],\"dynamicOutputs\":[],\"properties\":{\"storeComponentHistoryData\":\"true\",\"keepOnFailure\":\"false\",\"chosenDeleteTempDirBehavior\":\"deleteWorkingDirectoriesNever\"},\"isMockMode\":null}\"";

        jsonMapper.readValue(jsonStringWithMockmode, ToolExecutionRequest.class);

    }

}
