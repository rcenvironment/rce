/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.testutils.AuthorizationTestUtils;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Unit test for {@link ComponentDataConverter}, which especially tests the external serialized form of component descriptions/installations
 * to ensure network compatibility.
 *
 * @author Robert Mischke
 */
public class ComponentDataConverterTest {

    private static final String DOUBLE_QUOTE = "\"";

    private static final String DEFAULT_TEST_COMPONENT_ID = "testId";

    private static final String DEFAULT_TEST_COMPONENT_VERSION = "v1";

    private static final String DEFAULT_TEST_COMPONENT_ID_AND_VERSION = DEFAULT_TEST_COMPONENT_ID + "/" + DEFAULT_TEST_COMPONENT_VERSION;

    private static final String DEFAULT_TEST_NODE_ID_STRING = "04f0e7b6446a5f2a4d7d9137574b47f2:0";

    private final AuthorizationService authorizationService = AuthorizationTestUtils.createAuthorizationServiceStub();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Tests the serialized form of component descriptions/installations.
     * 
     * @throws OperationFailureException on unexpected failure
     * @throws IdentifierException on unexpected failure
     */
    @Test
    public void propertiesOfSerializedForm() throws OperationFailureException, IdentifierException {
        final ComponentInstallation installation = createTestComponentInstallation();
        String serialized = ComponentDataConverter.serializeComponentInstallationData(installation);

        assertThat(serialized, is(notNullValue()));
        // print the serialized form for inspection
        LogFactory.getLog(getClass()).debug(serialized);

        // actual content checks: presence/absence of fields and/or data
        assertThat(serialized,
            containsString("\"installationId\":\"" + DEFAULT_TEST_COMPONENT_ID_AND_VERSION + DOUBLE_QUOTE));
        assertThat(serialized, containsString("\"identifier\":\"" + DEFAULT_TEST_COMPONENT_ID + DOUBLE_QUOTE));
        assertThat(serialized, containsString("\"nodeId\":\"" + DEFAULT_TEST_NODE_ID_STRING + DOUBLE_QUOTE));
        assertThat(serialized, not(containsString("ublished")));

        // detect/prevent leaks of id object fields
        assertThat(serialized, not(containsString("\"Object")));

        // a simple stability check of the serialized form; obviously, the length value is specific to the created test object,
        // and must be adapted every time the test object is changed
        final int expectedSerializedLength = 1031;
        assertThat(serialized.length(), is(expectedSerializedLength));
    }

    /**
     * Tests deserialization of the external serialized form.
     * 
     * Note: This test overlaps with the (much more detailed) test
     * de.rcenvironment.core.component.model.internal.ComponentInstallationImplTest.testRoundTripSerialization(). Currently, this test does
     * not provide any additional benefit, but it may become more useful once {@link ComponentInstallation} is separated from the external
     * representation of component interfaces.
     * 
     * @throws OperationFailureException on unexpected failure
     * @throws IdentifierException on unexpected failure
     */
    @Test
    public void componentSerializationRoundTrip() throws OperationFailureException, IdentifierException {
        final ComponentInstallation originalInstallation = createTestComponentInstallation();
        assertPropertiesOfTestComponentInstallation(originalInstallation); // sanity check

        String serialized = ComponentDataConverter.serializeComponentInstallationData(originalInstallation);

        final ComponentInstallation restoredObject = ComponentDataConverter.deserializeComponentInstallationData(serialized);
        assertPropertiesOfTestComponentInstallation(restoredObject);
    }

    /**
     * Tests serialization and deserialization of a complete {@link DistributedComponentEntry} with public access.
     * 
     * @throws OperationFailureException on unexpected failure
     * @throws IdentifierException on unexpected failure
     */
    @Test
    public void componentEntrySerializationRoundTripWithPublicAuthorization() throws OperationFailureException, IdentifierException {

        final ComponentInstallation originalInstallation = createTestComponentInstallation();
        final AuthorizationPermissionSet permissionSet =
            authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
        final DistributedComponentEntry originalEntry = ComponentDataConverter.createLocalDistributedComponentEntry(originalInstallation,
            permissionSet, authorizationService);

        assertPropertiesOfTestComponentInstallation(originalInstallation); // sanity check

        final String serialized = originalEntry.getPublicationData();
        log.debug(serialized);

        final DistributedComponentEntry restoredEntry =
            ComponentDataConverter.deserializeRemoteDistributedComponentEntry(serialized, authorizationService);
        ComponentInstallation restoredInstallation = restoredEntry.getComponentInstallation();

        // test component data
        assertPropertiesOfTestComponentInstallation(restoredInstallation);

        // test authorization info
        assertThat(restoredEntry.getDeclaredPermissionSet(), CoreMatchers.equalTo(originalEntry.getDeclaredPermissionSet()));
    }

    /**
     * Tests serialization and deserialization of a complete {@link DistributedComponentEntry} with group-based authorization settings.
     * 
     * @throws OperationFailureException on unexpected failure
     * @throws IdentifierException on unexpected failure
     */
    @Test
    public void componentEntrySerializationRoundTripWithGroupBasedAuthorization() throws OperationFailureException, IdentifierException {

        final ComponentInstallation originalInstallation = createTestComponentInstallation();
        final AuthorizationAccessGroup group1 = authorizationService.createLocalGroup("Group1");
        final AuthorizationAccessGroup group2 = authorizationService.createLocalGroup("Group2");
        final AuthorizationAccessGroup group3 = authorizationService.createLocalGroup("Group3");
        final AuthorizationPermissionSet permissionSet = authorizationService.buildPermissionSet(group1, group2, group3);
        final DistributedComponentEntry originalEntry = ComponentDataConverter.createLocalDistributedComponentEntry(originalInstallation,
            permissionSet, authorizationService);

        assertThat(originalEntry.getDeclaredPermissionSet(), equalTo(permissionSet));
        assertThat(originalEntry.getMatchingPermissionSet(), equalTo(permissionSet));

        assertPropertiesOfTestComponentInstallation(originalInstallation); // sanity check

        final String serialized = originalEntry.getPublicationData();
        log.debug(serialized);

        authorizationService.deleteLocalGroupData(group2); // simulate unknown remote group; should still be restored
        assertThat("group2 not locally accessible anymore", !authorizationService.isGroupAccessible(group2));

        final DistributedComponentEntry restoredEntry =
            ComponentDataConverter.deserializeRemoteDistributedComponentEntry(serialized, authorizationService);
        ComponentInstallation restoredInstallation = restoredEntry.getComponentInstallation();

        // test component data
        assertPropertiesOfTestComponentInstallation(restoredInstallation);

        // test authorization info
        assertThat(restoredEntry.getDeclaredPermissionSet(), CoreMatchers.equalTo(permissionSet));
        // should be redundant, but hey, this is a test
        assertThat(restoredEntry.getDeclaredPermissionSet(), CoreMatchers.equalTo(originalEntry.getDeclaredPermissionSet()));

        final Collection<AuthorizationAccessGroup> restoredGroups = restoredEntry.getDeclaredPermissionSet().getAccessGroups();
        assertThat(restoredGroups.size(), is(3));
        assertThat("group1 present", restoredGroups.contains(group1));
        assertThat("group2 present", restoredGroups.contains(group2));
        assertThat("group3 present", restoredGroups.contains(group3));

        final Collection<AuthorizationAccessGroup> matchingGroups = restoredEntry.getMatchingPermissionSet().getAccessGroups();
        assertThat(matchingGroups.size(), is(2));
        assertThat("group1 present", matchingGroups.contains(group1));
        assertThat("group2 absent", !matchingGroups.contains(group2));
        assertThat("group3 present", matchingGroups.contains(group3));

        assertThat("overall entry is accessible", restoredEntry.isAccessible());
    }

    private ComponentInstallation createTestComponentInstallation() throws IdentifierException {
        final LogicalNodeId logicalNodeId = NodeIdentifierUtils.parseLogicalNodeIdString(DEFAULT_TEST_NODE_ID_STRING);
        final ComponentInstallation installation =
            ComponentTestUtils.createTestComponentInstallation(DEFAULT_TEST_COMPONENT_ID, DEFAULT_TEST_COMPONENT_VERSION, logicalNodeId);
        return installation;
    }

    private void assertPropertiesOfTestComponentInstallation(ComponentInstallation restoredInstallation) {
        assertThat(restoredInstallation.getInstallationId(), is(DEFAULT_TEST_COMPONENT_ID_AND_VERSION));
        assertThat(restoredInstallation.getComponentInterface().getIdentifier(), is(DEFAULT_TEST_COMPONENT_ID));
        assertThat(restoredInstallation.getComponentInterface().getVersion(), is(DEFAULT_TEST_COMPONENT_VERSION));
        assertThat(restoredInstallation.getComponentInterface().getIdentifierAndVersion(), is(DEFAULT_TEST_COMPONENT_ID_AND_VERSION));
        assertThat(restoredInstallation.getNodeId(), is(DEFAULT_TEST_NODE_ID_STRING));
        assertThat(restoredInstallation.getNodeIdObject().getLogicalNodeIdString(), is(DEFAULT_TEST_NODE_ID_STRING));
    }

}
