/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.internal.BCCryptographyOperationsProviderImpl;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Test for {@link AuthorizationServiceImpl}.
 *
 * @author Robert Mischke
 */
public class AuthorizationServiceImplTest {

    private AuthorizationServiceImpl service;

    /**
     * Common test setup.
     */
    @Before
    public void setup() {
        service = new AuthorizationServiceImpl();
        service.bindCryptographyOperationsProvider(new BCCryptographyOperationsProviderImpl());
    }

    /**
     * Tests default objects like the "public" access group and the "public" permission set (which should only contain the "public" access
     * group).
     */
    @Test
    public void defaultPermissionSets() {
        final DefaultAuthorizationObjects defaultObjects = service.getDefaultAuthorizationObjects();

        final AuthorizationAccessGroup publicAccessGroup = defaultObjects.accessGroupPublicInLocalNetwork();
        assertThat(publicAccessGroup.getFullId(), is("public"));
        assertThat(publicAccessGroup.getName(), is("public"));
        assertThat(publicAccessGroup.getIdPart(), nullValue());

        // note: the list of groups is current hard-coded to the "public" group
        assertThat(service.listAccessibleGroups(false).size(), is(0));
        assertThat(service.listAccessibleGroups(true).size(), is(1));
        assertThat(service.listAccessibleGroups(true), hasItems(publicAccessGroup));

        assertThat(defaultObjects.permissionSetLocalOnly().getAccessGroups().size(), is(0));
        assertThat(defaultObjects.permissionSetPublicInLocalNetwork().getAccessGroups().size(), is(1));

        assertThat(defaultObjects.permissionSetLocalOnly().includesAccessGroup(publicAccessGroup), is(false));
        assertThat(defaultObjects.permissionSetPublicInLocalNetwork().includesAccessGroup(publicAccessGroup), is(true));
    }

    /**
     * Tests group operations.
     * 
     * @throws OperationFailureException on failure
     */
    @Test
    public void groupHandling() throws OperationFailureException {
        final String test1Name = "Test1";
        final AuthorizationAccessGroup test1Group = service.createLocalGroup(test1Name);
        assertThat(test1Group.getName(), is(test1Name));
        assertThat(test1Group.getIdPart().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH));
        assertThat(test1Group.getFullId().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH + test1Name.length() + 1));

        assertThat(service.isGroupAccessible(test1Group), is(true));
        final AuthorizationAccessGroupKeyData originalKeyData = service.getKeyDataForGroup(test1Group);
        assertThat(originalKeyData, notNullValue());
        final int expectedEncodedKeyLength = CryptographyOperationsProvider.SYMMETRIC_KEY_EXPECTED_ENCODED_LENGTH;
        assertThat(originalKeyData.getEncodedStringForm().length(), is(expectedEncodedKeyLength));

        String exported = service.exportToString(test1Group);
        assertThat(exported, notNullValue());

        LogFactory.getLog(getClass()).debug(exported);

        service.deleteLocalGroupData(test1Group);
        assertThat(service.isGroupAccessible(test1Group), is(false));
        assertThat(service.getKeyDataForGroup(test1Group), nullValue());

        final AuthorizationAccessGroup imported = service.importFromString(exported);

        assertThat(service.isGroupAccessible(test1Group), is(true));

        assertThat(imported.getName(), is(test1Name));
        assertThat(imported.getIdPart().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH));
        assertThat(imported.getFullId().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH + test1Name.length() + 1));

        final AuthorizationAccessGroupKeyData importedKeyData = service.getKeyDataForGroup(imported);
        assertThat("old and new key data holders are not the same object", originalKeyData != importedKeyData);
        assertThat(importedKeyData.getEncodedStringForm().length(), is(expectedEncodedKeyLength));
        assertThat(importedKeyData.getEncodedStringForm(),
            equalTo(originalKeyData.getEncodedStringForm()));
    }

    /**
     * Tests group operations.
     * 
     * @throws OperationFailureException on failure
     */
    @Test
    public void externalGroupRepresentation() throws OperationFailureException {
        final String testGroupName = "Ext";
        final String testIdPart = "0123456789abcdef";
        final String extFullId = testGroupName + AuthorizationService.ID_SEPARATOR + testIdPart;
        final AuthorizationAccessGroup represented = service.representRemoteGroupId(extFullId);
        assertThat(represented.getName(), is(testGroupName));
        assertThat(represented.getIdPart().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH));
        assertThat(represented.getFullId().length(), is(AuthorizationService.GROUP_ID_SUFFIX_LENGTH + testGroupName.length() + 1));
        // TODO test displayName, too?

        assertThat(service.isGroupAccessible(represented), is(false));
        assertThat(service.getKeyDataForGroup(represented), nullValue());

        // service.exportToString(represented); // TODO test; should throw an exception
    }

    // TODO add test for various findLocalGroupById() cases
}
