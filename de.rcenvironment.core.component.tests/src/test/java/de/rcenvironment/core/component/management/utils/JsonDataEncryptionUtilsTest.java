/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.authorization.testutils.AuthorizationTestUtils;

/**
 * Unit test for {@link JsonDataEncryptionUtils}.
 *
 * @author Robert Mischke
 */
public class JsonDataEncryptionUtilsTest {

    private static final String KEY_ID_1 = "key1";

    private static final String DEFAULT_TEST_DATA = "{ \"someField\":\"someData\"}";

    private CryptographyOperationsProvider cryptographyOperationsProvider;

    /**
     * Common setup.
     * 
     * @throws Exception on failure
     */
    @Before
    public void setUp() throws Exception {
        cryptographyOperationsProvider = AuthorizationTestUtils.createAuthorizationServiceStub().getCryptographyOperationsProvider();
    }

    /**
     * Tests a basic encryption-decryption loop.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void basicRoundTrip() throws Exception {
        Map<String, SymmetricKey> keys = new HashMap<>();
        keys.put(KEY_ID_1, cryptographyOperationsProvider.generateSymmetricKey());
        final JsonDataWithOptionalEncryption wrapper =
            JsonDataEncryptionUtils.encryptForKeys(DEFAULT_TEST_DATA, keys, cryptographyOperationsProvider);
        String decrypted = JsonDataEncryptionUtils.attemptDecryption(wrapper, KEY_ID_1, keys.get(KEY_ID_1), cryptographyOperationsProvider);
        assertEquals(DEFAULT_TEST_DATA, decrypted);
    }

}
