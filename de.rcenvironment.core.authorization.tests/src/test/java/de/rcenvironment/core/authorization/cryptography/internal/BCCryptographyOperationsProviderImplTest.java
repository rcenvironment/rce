/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.cryptography.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link BCCryptographyOperationsProviderImpl} unit tests.
 *
 * @author Robert Mischke
 */
public class BCCryptographyOperationsProviderImplTest {

    private CryptographyOperationsProvider provider;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        provider = new BCCryptographyOperationsProviderImpl();
    }

    /**
     * Test symmetric key generation, encryption, and decryption. As part of the overall test, byte array wrapping is tested as well,
     * including wrapped transport of the encryption key.
     * 
     * @throws OperationFailureException on unexpected errors
     */
    @Test
    public void testSymmetricEncryptionRoundTrip() throws OperationFailureException {
        SymmetricKey originalKey = provider.generateSymmetricKey();
        String plainText = "test123";
        String wrappedCipherBytes = provider.encryptAndEncodeString(originalKey, plainText);
        String wrappedKey = provider.encodeSymmetricKey(originalKey);

        log.debug(wrappedCipherBytes);
        assertThat("no plaintext leak", !wrappedCipherBytes.contains(plainText));
        assertThat("no '/' character", !wrappedCipherBytes.contains("/"));
        log.debug(wrappedKey);

        final SymmetricKey restoredKey = provider.decodeSymmetricKey(wrappedKey);
        final String restoredText = provider.decodeAndDecryptString(restoredKey, wrappedCipherBytes);
        assertThat(restoredText, equalTo(plainText));
    }

}
