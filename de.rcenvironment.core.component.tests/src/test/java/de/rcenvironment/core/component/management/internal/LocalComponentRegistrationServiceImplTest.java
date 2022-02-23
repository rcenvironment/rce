/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.authorization.testutils.AuthorizationTestUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.impl.ComponentAuthorizationSelectorImpl;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Unit test for {@link LocalComponentRegistrationServiceImpl}.
 *
 * @author Robert Mischke
 */
public class LocalComponentRegistrationServiceImplTest {

    private LocalComponentRegistrationServiceImpl service;

    private AuthorizationService authService;

    private AuthorizationPermissionSet permissionSetPublic;

    private DefaultAuthorizationObjects defaultAuthorizationObjects;

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        authService = AuthorizationTestUtils.createAuthorizationServiceStub();

        final ComponentPermissionStorage storageMock = EasyMock.createNiceMock(ComponentPermissionStorage.class);
        EasyMock.expect(storageMock.restorePersistedAssignments())
            .andReturn(new HashMap<ComponentAuthorizationSelector, AuthorizationPermissionSet>());
        EasyMock.replay(storageMock);

        service = new LocalComponentRegistrationServiceImpl();
        service.bindAuthorizationService(authService);
        service.bindPermissionStorage(storageMock);
        service.bindDistributedComponentKnowledgeService(EasyMock.createNiceMock(DistributedComponentKnowledgeService.class));
        service.activate();

        defaultAuthorizationObjects = authService.getDefaultAuthorizationObjects();
        permissionSetPublic = defaultAuthorizationObjects.permissionSetPublicInLocalNetwork();
    }

    /**
     * Tests modification and reading of component-permission associations.
     * 
     * @throws OperationFailureException if setting a permission failed
     */
    @Test
    public void componentPermissionSettingAndQuerying() throws OperationFailureException {
        final ComponentAuthorizationSelector selector1 = new ComponentAuthorizationSelectorImpl("test1");

        // initial state
        assertThat(service.getComponentPermissionSet(selector1, false), nullValue());
        assertThat(service.getComponentPermissionSet(selector1, true), is(notNullValue()));
        assertThat(service.getComponentPermissionSet(selector1, true).getAccessGroups().size(), is(0));
        assertThat(service.getComponentPermissionSet(selector1, true).isLocalOnly(), is(true));
        assertThat(service.listAuthorizationSelectorsForRemotableComponents().size(), is(0));

        // set a permission
        service.setComponentPermissions(selector1, permissionSetPublic);
        assertThat(service.listAssignedComponentPermissions().size(), is(1));
        assertThat(service.listAssignedComponentPermissions().keySet(), hasItems(selector1));
        assertThat(service.getComponentPermissionSet(selector1, false), is(permissionSetPublic));

        // verify equality of equivalent selector
        final ComponentAuthorizationSelector selector1b = new ComponentAuthorizationSelectorImpl("test1");
        assertThat(service.listAssignedComponentPermissions().keySet(), hasItems(selector1b));
        assertThat(service.getComponentPermissionSet(selector1b, false), is(permissionSetPublic));

        // verify a different selector still doesn't match
        final ComponentAuthorizationSelectorImpl selector2 = new ComponentAuthorizationSelectorImpl("test2");
        assertThat(service.getComponentPermissionSet(selector2, true).getAccessGroups().size(), is(0));

        // final sanity check; should not be affected at all
        assertThat(service.listAuthorizationSelectorsForRemotableComponents().size(), is(0));
    }

    /**
     * Tests {@link LocalComponentRegistrationServiceImpl#setComponentPermissionState()}(), including the special cases of setting or
     * resetting public access.
     * 
     * @throws OperationFailureException if setting a permission failed
     */
    @Test
    public void individualPermissionSetting() throws OperationFailureException {
        final ComponentAuthorizationSelector selector = new ComponentAuthorizationSelectorImpl("test3");

        final AuthorizationAccessGroup group1 = authService.createLocalGroup("g1");
        service.setComponentPermissionState(selector, group1, false);

        // resetting an absent permission should cause no change; still a "null" permission set
        assertThat(service.getComponentPermissionSet(selector, false), is(nullValue()));

        service.setComponentPermissionState(selector, group1, true);
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().size(), is(1));
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().contains(group1), is(true));

        final AuthorizationAccessGroup publicGroup = defaultAuthorizationObjects.accessGroupPublicInLocalNetwork();

        // set "public" group access to "true" -> should rewrite the whole permission set to "public", not just add the "public" group
        service.setComponentPermissionState(selector, publicGroup, true);
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().size(), is(1)); // not 2
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().contains(publicGroup), is(true));

        // set "public" group access to "false" -> should rewrite the whole permission set to "local" (no real special case yet)
        service.setComponentPermissionState(selector, publicGroup, false);
        assertThat(service.getComponentPermissionSet(selector, false), is(nullValue()));

        // restore Group1 access and check consistency
        service.setComponentPermissionState(selector, group1, true);
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().size(), is(1));
        assertThat(service.getComponentPermissionSet(selector, false).getAccessGroups().contains(group1), is(true));

        // set "public" group access to "false" -> this is the actual special case: should still reset to "local", ie remove Group1
        service.setComponentPermissionState(selector, publicGroup, false);
        assertThat(service.getComponentPermissionSet(selector, false), is(nullValue()));
    }

    /**
     * Registers the standard change listener at the authorization service, and tests that permissions using local groups are properly
     * deleted when those groups are deleted.
     * 
     * @throws OperationFailureException on unexpected errors
     */
    @Test
    public void behaviorOnLocalAuthGroupDeletion() throws OperationFailureException {

        // not done in setup by default
        authService.addAuthorizationAccessGroupListener(service);

        final AuthorizationAccessGroup g1 = authService.createLocalGroup("g1");
        final AuthorizationAccessGroup g2 = authService.createLocalGroup("g2");
        final ComponentAuthorizationSelector selector1 = new ComponentAuthorizationSelectorImpl("common/c1");
        final ComponentAuthorizationSelector selector2 = new ComponentAuthorizationSelectorImpl("common/c2");

        assertThat(service.listAssignedComponentPermissions().size(), is(0));
        service.setComponentPermissionState(selector1, g1, true);
        service.setComponentPermissionState(selector1, g2, true);
        service.setComponentPermissionState(selector2, g1, true);
        service.setComponentPermissionState(selector2, g2, true);
        assertThat(service.listAssignedComponentPermissions().size(), is(2));
        assertThat("c1 authorized for g1", service.getComponentPermissionSet(selector1, true).includesAccessGroup(g1));
        assertThat("c1 authorized for g2", service.getComponentPermissionSet(selector1, true).includesAccessGroup(g2));
        assertThat("c2 authorized for g1", service.getComponentPermissionSet(selector2, true).includesAccessGroup(g1));

        authService.deleteLocalGroupData(g1);

        assertThat("c1 not authorized for g1 anymore", !service.getComponentPermissionSet(selector1, true).includesAccessGroup(g1));
        assertThat("c1 still authorized for g2", service.getComponentPermissionSet(selector1, true).includesAccessGroup(g2));
        assertThat("c2 not authorized for g1 anymore", !service.getComponentPermissionSet(selector2, true).includesAccessGroup(g1));
    }

}
