/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierServiceImpl;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * {@link NodePropertiesRegistry} tests.
 *
 * @author Robert Mischke
 */
public class NodePropertiesRegistryTest {

    private NodePropertiesRegistry registry = new NodePropertiesRegistry();

    private NodeIdentifierService nodeIdentifierService = new NodeIdentifierServiceImpl(IdGeneratorType.FAST);

    /**
     * Tests a basic merge operation and the returned merged and parsed value.
     * 
     * @throws Exception on test setup error
     */
    @Test
    public void basicMerging() throws Exception {
        List<NodePropertyImpl> mergeResult =
            mergeUpdateData("3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10000:displayName:1500000000000:Instance 1");

        assertEquals(1, mergeResult.size());
        NodePropertyImpl firstResultEntry = mergeResult.get(0);
        final long expectedSeqNo = 1500000000000L;
        assertEquals(expectedSeqNo, firstResultEntry.getSequenceNo());
        assertEquals("3115a32f64d1f72156fa81afc44ff9cd", firstResultEntry.getInstanceNodeSessionId().getInstanceNodeIdString());
        assertEquals("59dda10000", firstResultEntry.getInstanceNodeSessionId().getSessionIdPart());
        assertEquals("displayName", firstResultEntry.getKey());
        assertEquals("Instance 1", firstResultEntry.getValue());

        assertEquals(1, registry.getDetachedCopyOfEntries().size());
    }

    /**
     * Verifies the removal of outdated node properties once a later session id is merged. For testing the merged state, only the "effective
     * set" and the total number of registered properties is checked, which should be sufficient for now. Note that distributed property
     * propagation is verified by higher-level (VirtualInstance) tests, too.
     * 
     * @throws Exception on test setup error
     */
    @Test
    public void testOldNodeSessionRemoval() throws Exception {
        List<NodePropertyImpl> effectiveSet;

        effectiveSet = mergeUpdateData(
            "3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10000:displayName:1500000000000:Instance 1",
            "3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10000:dummyProperty:1500000000000:dummyValue 1",
            "66dc36b999217894b2b2141cbbe0a8bf\\:\\:59dda10000:displayName:1500000000001:Instance 2",
            "66dc36b999217894b2b2141cbbe0a8bf\\:\\:59dda10000:dummyProperty:1500000000001:dummyValue 2");
        assertEquals(4, effectiveSet.size());
        assertEquals(4, registry.getDetachedCopyOfEntries().size());

        // test a standard update by timestamps; two are more recent (actual updates), one is not
        effectiveSet = mergeUpdateData(
            "3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10000:displayName:1500000000002:Instance 1",
            "66dc36b999217894b2b2141cbbe0a8bf\\:\\:59dda10000:displayName:1500000000002:Instance 2",
            "66dc36b999217894b2b2141cbbe0a8bf\\:\\:59dda10000:dummyProperty:1500000000001:dummyValue 2");
        assertEquals(2, effectiveSet.size());
        assertEquals(4, registry.getDetachedCopyOfEntries().size());

        // now the main test case: registering properties from a newer instance session should remove all properties of any old session;
        // the timestamp should not matter, so it is artificially left the same for a stricter test condition, although this constellation
        // would not happen in practice
        effectiveSet = mergeUpdateData(
            "3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10005:dummyProperty:1500000000000:dummyValue 1");
        assertEquals(1, effectiveSet.size());
        assertEquals(3, registry.getDetachedCopyOfEntries().size());

        // verify that an older session does not take place, even if its timestamp is more recent
        effectiveSet = mergeUpdateData(
            "3115a32f64d1f72156fa81afc44ff9cd\\:\\:59dda10000:dummyProperty:1500000000003:dummyValue 1b");
        assertEquals(0, effectiveSet.size());
        assertEquals(3, registry.getDetachedCopyOfEntries().size());
    }

    private List<NodePropertyImpl> mergeUpdateData(String... entries) throws IdentifierException {
        List<NodePropertyImpl> input = new ArrayList<>();
        for (String entry : entries) {
            input.add(new NodePropertyImpl(entry, nodeIdentifierService));
        }
        List<NodePropertyImpl> mergeResult = new ArrayList<>(registry.mergeAndGetEffectiveSubset(input));
        return mergeResult;
    }

}
