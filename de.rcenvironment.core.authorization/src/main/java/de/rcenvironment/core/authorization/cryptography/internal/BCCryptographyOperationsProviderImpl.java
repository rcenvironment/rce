/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.cryptography.internal;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Bouncy Castle implementation of {@link CryptographyOperationsProvider}.
 * 
 * @author Robert Mischke
 */
@Component
public class BCCryptographyOperationsProviderImpl extends AbstractCryptographyOperationsProvider implements CryptographyOperationsProvider {

    private static final String ERROR_MESSAGE_INITIALIZING_SYMMETRIC_CIPHER = "Error initializing symmetric cipher";

    private static final String ERROR_MESSAGE_GENERIC_CRYPTO_ERROR = "Error during cryptographic operation";

    private static final String BC_PROVIDER_ID = "BC";

    private static final String SYMMETRIC_CIPHER_ID = "AES";

    private final Log log = LogFactory.getLog(getClass());

    private SecureRandom sharedSecureRandom;

    public BCCryptographyOperationsProviderImpl() {
        if (Security.getProvider(BC_PROVIDER_ID) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.debug("Installed BouncyCastle provider");
        }
        sharedSecureRandom = new SecureRandom();
    }

    @Override
    public SymmetricKey generateSymmetricKey() throws OperationFailureException {
        // note: the actual key length is verified in encodeRawKey() below
        KeyGenerator kg;
        try {
            kg = KeyGenerator.getInstance(SYMMETRIC_CIPHER_ID, BC_PROVIDER_ID);
            // InvalidParameterException is a RTE, but would be thrown here on invalid key size
            kg.init(SYMMETRIC_KEY_NATIVE_BIT_LENGTH);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidParameterException e) {
            // unlikely error -> stacktrace
            throw new OperationFailureException(ERROR_MESSAGE_INITIALIZING_SYMMETRIC_CIPHER, e);
        }
        SecretKey rawKey = kg.generateKey();

        final String encodedKey = encodeRawKey(rawKey, SYMMETRIC_KEY_NATIVE_BYTE_LENGTH);
        // note: currently no version separator as there should be no ambiguity
        final String completeEncoded = SYMMETRIC_KEY_CURRENT_VERSION_PREFIX + encodedKey;
        if (completeEncoded.length() != SYMMETRIC_KEY_EXPECTED_ENCODED_LENGTH) {
            throw new OperationFailureException("Internal error: unexpected length of serialized key");
        }
        return new SymmetricKeyImpl(rawKey, completeEncoded);
    }

    @Override
    public byte[] encrypt(SymmetricKey key, byte[] input) throws OperationFailureException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_CIPHER_ID, BC_PROVIDER_ID);
            cipher.init(Cipher.ENCRYPT_MODE, getRawKeyFromWrapper(key));
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            // unlikely error -> stacktrace
            throw new OperationFailureException(ERROR_MESSAGE_INITIALIZING_SYMMETRIC_CIPHER, e);
        } catch (InvalidKeyException e) {
            // data error -> no stacktrace
            throw new OperationFailureException("Invalid encryption key: " + e.toString());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            // unlikely error -> stacktrace
            throw new OperationFailureException(ERROR_MESSAGE_GENERIC_CRYPTO_ERROR, e);
        }
    }

    @Override
    public byte[] decrypt(SymmetricKey key, byte[] input) throws OperationFailureException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SYMMETRIC_CIPHER_ID, BC_PROVIDER_ID);
            cipher.init(Cipher.DECRYPT_MODE, getRawKeyFromWrapper(key));
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            // unlikely error -> stacktrace
            throw new OperationFailureException(ERROR_MESSAGE_INITIALIZING_SYMMETRIC_CIPHER, e);
        } catch (InvalidKeyException e) {
            // data error -> no stacktrace
            throw new OperationFailureException("Invalid encryption key: " + e.toString());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            // unlikely error -> stacktrace
            throw new OperationFailureException(ERROR_MESSAGE_GENERIC_CRYPTO_ERROR, e);
        }
    }

    @Override
    public String encodeSymmetricKey(SymmetricKey key) {
        return key.getEncodedForm(); // delegate
    }

    @Override
    public SymmetricKey decodeSymmetricKey(String input) throws OperationFailureException {
        if (input.length() != SYMMETRIC_KEY_EXPECTED_ENCODED_LENGTH) {
            throw new OperationFailureException("Unexpected length of received key data: " + input);
        }
        // note: currently no version separator as there should be no ambiguity
        final String expectedPrefix = SYMMETRIC_KEY_CURRENT_VERSION_PREFIX;
        if (!input.startsWith(expectedPrefix)) {
            throw new OperationFailureException("Unexpected key format (missing version identifier): " + input);
        }
        final SecretKeySpec rawKey = decodeRawKey(input.substring(expectedPrefix.length()), SYMMETRIC_KEY_NATIVE_BYTE_LENGTH);
        // reuse the original encoded form, assuming that it is well-formed at this point
        return new SymmetricKeyImpl(rawKey, input);
    }

    private String encodeRawKey(final SecretKey rawKey, int expectedByteLength) throws OperationFailureException {
        final byte[] encoded = rawKey.getEncoded();
        if (encoded.length != expectedByteLength) {
            throw new OperationFailureException("Unexpected native key representation: " + Hex.encodeHexString(encoded));
        }
        return encodeByteArray(encoded);
    }

    private SecretKeySpec decodeRawKey(String input, int expectedByteLength) throws OperationFailureException {
        if (input == null) {
            throw new OperationFailureException("Key data cannot be null");
        }
        final byte[] decodeByteArray = decodeByteArray(input);
        if (decodeByteArray.length != expectedByteLength) {
            throw new IllegalStateException("Unexpected key material (invalid length): " + Hex.encodeHexString(decodeByteArray));
        }
        final SecretKeySpec rawKey = new SecretKeySpec(decodeByteArray, SYMMETRIC_CIPHER_ID);
        return rawKey;
    }

    private SecretKey getRawKeyFromWrapper(SymmetricKey wrapper) {
        return ((SymmetricKeyImpl) wrapper).getSecretKey();
    }

}
