/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.testutils.AuthorizationTestUtils;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.internal.ComponentDataConverter;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;

/**
 * Test for {@link DistributedComponentKnowledgeService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Alexander Weinert
 */
public class DistributedComponentKnowledgeServiceTest {

    private LogicalNodeId localNodeId;

    private InstanceNodeSessionId localInstanceNodeSessionId;

    private final AuthorizationService authorizationServiceStub = AuthorizationTestUtils.createAuthorizationServiceStub();

    private final AuthorizationPermissionSet publicPermissionSet =
        authorizationServiceStub.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();

    // An access group that the local instance is not a member of
    private AuthorizationAccessGroup groupA;

    // A permission set containing only groupA
    private AuthorizationPermissionSet permissionSetA;

    // An access group that the local instance is a member of
    private AuthorizationAccessGroup groupB;

    // A permission set containing only groupB
    private AuthorizationPermissionSet permissionSetB;

    /**
     * Since {@link DistributedComponentKnowledgeServiceImpl} is versatile and concurrent, testing this class requires both the construction
     * of a number of helper objects as well as careful orchestration of calls and waiting for callbacks. This class serves to simplify
     * testing of {@link DistributedComponentKnowledgeServiceImpl} by providing methods that streamline the actual testing code, enabling
     * the tester to focus on the behavior being tested instead of the minutiae of, e.g., waiting for callbacks.
     * 
     * @author Alexander Weinert
     */
    private class DistributedComponentKnowledgeServiceTestWrapper {

        private final DistributedComponentKnowledgeServiceImpl service;

        private final NodePropertiesChangeListener nodePropertiesChangeListener;

        DistributedComponentKnowledgeServiceTestWrapper(DistributedComponentKnowledgeServiceImpl serviceParam) {
            this.service = serviceParam;

            final Collection<AdditionalServiceDeclaration> additionalServices = service.defineAdditionalServices();

            assertTrue("DistributedComponentKnowledgeServiceImpl must ask for registration of a NodePropertiesChangeListener",
                additionalServices.size() >= 1);
            final Optional<AdditionalServiceDeclaration> optionalNodePropertiesChangeListener = additionalServices.stream()
                .filter(serviceDeclaration -> serviceDeclaration.getServiceClass().equals(NodePropertiesChangeListener.class))
                .findAny();
            assertTrue("DistributedComponentKnowledgeServiceImpl must ask for registration of a NodePropertiesChangeListener",
                optionalNodePropertiesChangeListener.isPresent());

            nodePropertiesChangeListener = (NodePropertiesChangeListener) optionalNodePropertiesChangeListener.get().getImplementation();
        }

        /**
         * Notifies the wrapped {@link DistributedComponentKnowledgeServiceImpl} about added properties and waits for the update of the
         * {@link DistributedComponentKnowledge}.
         * 
         * @param nodeProperties The added node properties.
         * @return The updated snapshot of {@link DistributedComponentKnowledge}.
         */
        public DistributedComponentKnowledge addProperties(NodeProperty... nodeProperties) {
            final Collection<NodeProperty> addedProperties = Arrays.asList(nodeProperties);
            final Collection<NodeProperty> changedProperties = new HashSet<>();
            final Collection<NodeProperty> removedProperties = new HashSet<>();

            return notifyProperties(addedProperties, changedProperties, removedProperties);
        }

        /**
         * Notifies the wrapped {@link DistributedComponentKnowledgeServiceImpl} about updated properties and waits for the update of the
         * {@link DistributedComponentKnowledge}.
         * 
         * @param nodeProperties The updated node properties.
         * @return The updated snapshot of {@link DistributedComponentKnowledge}.
         */
        public DistributedComponentKnowledge updateProperties(NodeProperty... nodeProperties) {
            final Collection<NodeProperty> addedProperties = new HashSet<>();
            final Collection<NodeProperty> changedProperties = Arrays.asList(nodeProperties);
            final Collection<NodeProperty> removedProperties = new HashSet<>();

            return notifyProperties(addedProperties, changedProperties, removedProperties);
        }

        /**
         * Notifies the wrapped {@link DistributedComponentKnowledgeServiceImpl} about removed properties and waits for the update of the
         * {@link DistributedComponentKnowledge}.
         * 
         * @param nodeProperties The removed node properties.
         * @return The updated snapshot of {@link DistributedComponentKnowledge}.
         */
        public DistributedComponentKnowledge removeProperties(NodeProperty... nodeProperties) {
            final Collection<NodeProperty> addedProperties = new HashSet<>();
            final Collection<NodeProperty> changedProperties = new HashSet<>();
            final Collection<NodeProperty> removedProperties = Arrays.asList(nodeProperties);

            return notifyProperties(addedProperties, changedProperties, removedProperties);
        }

        private DistributedComponentKnowledge notifyProperties(Collection<NodeProperty> addedProperties,
            Collection<NodeProperty> updatedProperties, Collection<NodeProperty> removedProperties) {

            final Semaphore callbackFired = new Semaphore(0);
            final DistributedComponentKnowledgeListener listenerStub = newState -> callbackFired.release();
            service.addDistributedComponentKnowledgeListener(listenerStub);

            nodePropertiesChangeListener.onReachableNodePropertiesChanged(addedProperties, updatedProperties, removedProperties);

            try {
                callbackFired.tryAcquire(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail("DistributedComponentKnowledgeServiceImpl did not notify callbacks within two seconds.");
            }

            service.removeDistributedComponentKnowledgeListener(listenerStub);

            return service.getCurrentSnapshot();
        }

    }

    /**
     * The class {@link DistributedComponentKnowledge} offers quite a wide-ranged overview over the known components of all known nodes. By
     * wrapping the common assertions about that object in this class we increase readability of the test code.
     *
     * @author Alexander Weinert
     */
    private class DistributedComponentKnowledgeTestWrapper {

        private final DistributedComponentKnowledge knowledge;

        DistributedComponentKnowledgeTestWrapper(DistributedComponentKnowledge knowledge) {
            this.knowledge = knowledge;
        }

        /**
         * @param expected The expected number of all known installations.
         * @return This object for daisy-chaining.
         */
        public DistributedComponentKnowledgeTestWrapper assertNumberOfAllInstallations(int expected) {
            assertEquals(expected, knowledge.getAllInstallations().size());
            return this;
        }

        /**
         * @param expected The expected number of all local installations.
         * @return This object for daisy-chaining.
         */
        public DistributedComponentKnowledgeTestWrapper assertNumberOfLocalInstallations(int expected) {
            assertEquals(expected, knowledge.getAllLocalInstallations().size());
            return this;
        }

        /**
         * @param expected The expected number of all known shared installations.
         * @return This object for daisy-chaining.
         */
        public DistributedComponentKnowledgeTestWrapper assertNumberOfKnownSharedInstallations(int expected) {
            assertEquals(expected, knowledge.getKnownSharedInstallations().size());
            return this;
        }

        /**
         * @param nodeId   The id of the remote node.
         * @param expected The expected number of all known shared installations on the given node.
         * @return This object for daisy-chaining.
         */
        public DistributedComponentKnowledgeTestWrapper assertNumberOfSharedInstallationsOnNode(ResolvableNodeId nodeId, int expected) {
            assertEquals(expected, knowledge.getKnownSharedInstallationsOnNode(nodeId, true).size());
            return this;
        }

        /**
         * @param nodeId   The id of the remote node.
         * @param expected The expected number of all known installations on the given node that are accessible by this node.
         * @return This object for daisy-chaining.
         */
        public DistributedComponentKnowledgeTestWrapper assertNumberOfAccessibleSharedInstallationsOnNode(ResolvableNodeId nodeId,
            int expected) {
            assertEquals(expected, knowledge.getKnownSharedInstallationsOnNode(nodeId, false).size());
            return this;
        }

        /**
         * @param nodeId        The id of the remote node.
         * @param installations The complete list of installations on the given remote node that are accessible by this node.
         * @return This object for daisy-chaining.
         */
        public void assertAccessibleSharedInstallationsOnNode(LogicalNodeId nodeId,
            ComponentInstallation... installations) {
            final Collection<DistributedComponentEntry> sharedInstallationsOnNode =
                knowledge.getKnownSharedInstallationsOnNode(nodeId, false);

            assertEquals(installations.length, sharedInstallationsOnNode.size());

            for (ComponentInstallation componentInstallation : installations) {
                final Predicate<DistributedComponentEntry> hasGivenInstallation =
                    componentEntry -> componentEntry.getComponentInstallation().equals(componentInstallation);
                final Optional<DistributedComponentEntry> remoteEntry =
                    sharedInstallationsOnNode.stream().filter(hasGivenInstallation).findAny();
                assertTrue(StringUtils.format("ComponentInstallation %s not present on node", componentInstallation),
                    remoteEntry.isPresent());
            }
        }
    }

    /**
     * Set up.
     * 
     * @throws OperationFailureException Thrown if the creation of the access groups groupA or groupB fails. Not expected.
     */
    @Before
    public void setUp() throws OperationFailureException {
        LogicalNodeSessionId localNodeSessionId = NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true);
        localNodeId = localNodeSessionId.convertToLogicalNodeId();
        localInstanceNodeSessionId = localNodeSessionId.convertToInstanceNodeSessionId();

        groupA = authorizationServiceStub.createLocalGroup("GroupA");
        permissionSetA = authorizationServiceStub.buildPermissionSet(groupA);
        authorizationServiceStub.deleteLocalGroupData(groupA);

        groupB = authorizationServiceStub.createLocalGroup("GroupB");
        permissionSetB = authorizationServiceStub.buildPermissionSet(groupB);
    }

    /**
     * Tests that DistributedComponentKnowledgeServiceImpl correctly reports changes of the local installations of components to the
     * NodePropertiesService. In particular, it asserts - that the DistributedComponentKnowledgeService only reports those local
     * installations that are, in fact, published, - that it only reports the changes in local installations instead of the complete set of
     * local installations, and - that it correctly reports removed installations
     */
    @Test
    public void testDeltaOfLocalInstallations() {

        final DistributedComponentKnowledgeServiceImpl service = new DistributedComponentKnowledgeServiceImpl(localInstanceNodeSessionId);
        service.activate();

        final Collection<DistributedComponentEntry> localInstallations = new ArrayList<>();

        final DistributedComponentEntry entry1 = ComponentTestUtils.createTestDistributedComponentEntry("id-1", "1", localNodeId,
            publicPermissionSet, authorizationServiceStub);

        final DistributedComponentEntry entry2 = ComponentTestUtils.createTestDistributedComponentEntry("id-2", "2", localNodeId,
            null, authorizationServiceStub);

        NodePropertiesService nodePropertiesService = EasyMock.createStrictMock(NodePropertiesService.class);
        service.bindNodePropertiesService(nodePropertiesService);

        Capture<Map<String, String>> firstDelta = Capture.newInstance();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(firstDelta));
        EasyMock.replay(nodePropertiesService);

        localInstallations.add(entry1);
        localInstallations.add(entry2);
        service.updateLocalComponentInstallations(localInstallations, true);
        EasyMock.verify(nodePropertiesService);

        Map<String, String> delta = firstDelta.getValue();
        assertEquals(1, delta.size());
        assertTrue(delta.containsKey(createDeltaKey(entry1)));
        assertNotNull(delta.get(createDeltaKey(entry1)));

        final DistributedComponentEntry entry3 =
            ComponentTestUtils.createTestDistributedComponentEntry("id-3", "3", localNodeId,
                publicPermissionSet, authorizationServiceStub);

        final DistributedComponentEntry entry4 =
            ComponentTestUtils.createTestDistributedComponentEntry("id-4", "4", localNodeId,
                publicPermissionSet, authorizationServiceStub);

        EasyMock.reset(nodePropertiesService);
        Capture<Map<String, String>> secondDelta = Capture.newInstance();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(secondDelta));
        EasyMock.replay(nodePropertiesService);

        localInstallations.add(entry3);
        localInstallations.add(entry4);
        service.updateLocalComponentInstallations(localInstallations, true);
        EasyMock.verify(nodePropertiesService);

        delta = secondDelta.getValue();
        assertEquals(2, delta.size());
        assertTrue(delta.containsKey(createDeltaKey(entry3)));
        assertNotNull(delta.get(createDeltaKey(entry3)));
        assertTrue(delta.containsKey(createDeltaKey(entry4)));
        assertNotNull(delta.get(createDeltaKey(entry4)));

        EasyMock.reset(nodePropertiesService);
        Capture<Map<String, String>> thirdDelta = Capture.newInstance();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(thirdDelta));
        EasyMock.replay(nodePropertiesService);

        localInstallations.remove(entry3);
        service.updateLocalComponentInstallations(localInstallations, true);
        EasyMock.verify(nodePropertiesService);

        delta = thirdDelta.getValue();
        assertEquals(1, delta.size());
        assertTrue(delta.containsKey(createDeltaKey(entry3)));
        assertNull(delta.get(createDeltaKey(entry3)));
    }

    private String createDeltaKey(DistributedComponentEntry entry) {
        return StringUtils.format("componentInstallation/%s", entry.getComponentInstallation().getInstallationId());
    }

    /**
     * Asserts that the {@link DistributedComponentKnowledgeService} correctly interprets property changes of remote instances.
     * 
     * @throws OperationFailureException Thrown if the deserialization of a component installation fails. Not expected.
     * @throws InterruptedException      Thrown if the internal callbacks of {@link DistributedComponentKnowledgeServiceImpl} are not fired
     *                                   within two seconds. Not expected.
     */
    @Test
    public void testRemoteInstallationUpdate() throws OperationFailureException, InterruptedException {
        final DistributedComponentKnowledgeServiceImpl service = new DistributedComponentKnowledgeServiceImpl(localInstanceNodeSessionId);
        service.activate();
        service.bindAuthorizationService(authorizationServiceStub);

        final DistributedComponentKnowledgeServiceTestWrapper serviceWrapper = new DistributedComponentKnowledgeServiceTestWrapper(service);

        final InstanceNodeSessionId remoteInstanceSessionId = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
        final LogicalNodeId remoteNodeId = remoteInstanceSessionId.convertToDefaultLogicalNodeId();

        final ComponentInstallation componentInstallation = ComponentTestUtils.createTestComponentInstallation("myComponent", "2.0",
            remoteNodeId);

        final String componentInstallationPropertyKey = "componentInstallation/myComponent:2.0";

        // Make a component public on a remote instance
        final String serializedComponentInstallation = ComponentDataConverter
            .createLocalDistributedComponentEntry(componentInstallation, this.publicPermissionSet, authorizationServiceStub)
            .getPublicationData();

        final NodeProperty publicComponentProperty =
            mockNodeProperty(remoteInstanceSessionId, componentInstallationPropertyKey, serializedComponentInstallation, 1);

        final DistributedComponentKnowledge afterAdding = serviceWrapper.addProperties(publicComponentProperty);

        new DistributedComponentKnowledgeTestWrapper(afterAdding)
            .assertNumberOfAllInstallations(1)
            .assertNumberOfLocalInstallations(0)
            .assertNumberOfKnownSharedInstallations(1)
            .assertNumberOfSharedInstallationsOnNode(remoteNodeId, 1)
            .assertNumberOfAccessibleSharedInstallationsOnNode(remoteNodeId, 1)
            .assertAccessibleSharedInstallationsOnNode(remoteNodeId, componentInstallation);

        // Make the component inaccessible by adding it to an inaccessible group
        final String serializedInaccessibleComponentInstallation = ComponentDataConverter
            .createLocalDistributedComponentEntry(componentInstallation, permissionSetA, authorizationServiceStub)
            .getPublicationData();
        final NodeProperty inaccessibleComponentProperty = mockNodeProperty(remoteInstanceSessionId, componentInstallationPropertyKey,
            serializedInaccessibleComponentInstallation, 2);

        final DistributedComponentKnowledge afterInaccessible = serviceWrapper.updateProperties(inaccessibleComponentProperty);

        new DistributedComponentKnowledgeTestWrapper(afterInaccessible)
            .assertNumberOfAllInstallations(0)
            .assertNumberOfLocalInstallations(0)
            .assertNumberOfKnownSharedInstallations(0)
            .assertNumberOfSharedInstallationsOnNode(remoteNodeId, 1)
            .assertNumberOfAccessibleSharedInstallationsOnNode(remoteNodeId, 0);

        // Make the component accessible again by adding it to an available group
        final String serializedAccessibleComponentInstallation = ComponentDataConverter
            .createLocalDistributedComponentEntry(componentInstallation, permissionSetB, authorizationServiceStub)
            .getPublicationData();

        final NodeProperty groupBComponentProperty =
            mockNodeProperty(remoteInstanceSessionId, componentInstallationPropertyKey, serializedAccessibleComponentInstallation, 1);

        final DistributedComponentKnowledge afterReaccessible = serviceWrapper.updateProperties(groupBComponentProperty);
        new DistributedComponentKnowledgeTestWrapper(afterReaccessible)
            .assertNumberOfAllInstallations(1)
            .assertNumberOfLocalInstallations(0)
            .assertNumberOfKnownSharedInstallations(1)
            .assertNumberOfSharedInstallationsOnNode(remoteNodeId, 1)
            .assertNumberOfAccessibleSharedInstallationsOnNode(remoteNodeId, 1)
            .assertAccessibleSharedInstallationsOnNode(remoteNodeId, componentInstallation);

        // Remove the component from the remote instance
        final DistributedComponentKnowledge afterRemoval = serviceWrapper.removeProperties(groupBComponentProperty);
        new DistributedComponentKnowledgeTestWrapper(afterRemoval)
            .assertNumberOfAllInstallations(0)
            .assertNumberOfLocalInstallations(0)
            .assertNumberOfKnownSharedInstallations(0)
            .assertNumberOfSharedInstallationsOnNode(remoteNodeId, 0)
            .assertNumberOfAccessibleSharedInstallationsOnNode(remoteNodeId, 0);
    }

    private NodeProperty mockNodeProperty(InstanceNodeSessionId instanceNodeSessionId, String dataKey, String value, long sequenceNo) {
        return new NodePropertyMockBuilder()
            .instanceNodeSessionId(instanceNodeSessionId)
            .instanceNodeSessionIdString(instanceNodeSessionId.getInstanceNodeIdString())
            .distributedUniqueKey(StringUtils.format("%s:%s", instanceNodeSessionId, dataKey))
            .key(dataKey).value(value)
            .sequenceNo(sequenceNo)
            .build();
    }
}
