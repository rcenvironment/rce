/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Utilities to encrypt and decrypt JSON data with one or more independently usable keys.
 * <p>
 * This utility class is NOT concerned with the question how component data is mapped to oro from its string representation. In fact, the
 * provided string data could be anything, although in practice, it is checked whether it "looks like" JSON data to verify proper
 * decryption.
 *
 * @author Robert Mischke
 */
public final class JsonDataEncryptionUtils {

    private JsonDataEncryptionUtils() {}

    /**
     * Wraps the provided data as-is, without any encryption.
     * 
     * @param inputData the data to wrap
     * @return the generated transfer object
     */
    public static JsonDataWithOptionalEncryption asPublicData(String inputData) {
        return new JsonDataWithOptionalEncryption(inputData, null);
    }

    /**
     * Generates a random key, encrypts the provided data with it, and encrypts that common key with each provided individual key. The
     * encrypted input data and the encrypted forms of the common key are returned as a {@link #JsonDataEncryptionUtils()}.
     * 
     * @param inputData the data to wrap
     * @param providedEncryptionKeys the individual keys that should have access to the input data
     * @param cryptographyOperationsProvider the cryptography implementation to use
     * @return the generated transfer object
     * 
     * @throws OperationFailureException on failure to generate a random encryption key
     */
    public static JsonDataWithOptionalEncryption encryptForKeys(String inputData,
        Map<String, SymmetricKey> providedEncryptionKeys,
        CryptographyOperationsProvider cryptographyOperationsProvider) throws OperationFailureException {

        // generate the common symmetric encryption key to encrypt the input data with
        SymmetricKey commonEncryptionKey = cryptographyOperationsProvider.generateSymmetricKey();
        // TODO (p2) improve: The common key was encoded already, and is now encrypted as that
        // string form's byte array representation - ideally, the key's byte array form would be encrypted directly.
        // While not dramatic for performance, this may make interoperation with other languages more difficult.
        final String encodedCommonEncryptionKey = commonEncryptionKey.getEncodedForm();

        // encrypt the data
        String encryptedData = cryptographyOperationsProvider.encryptAndEncodeString(commonEncryptionKey, inputData);

        // prepare the map of individual key ciphertexts
        Map<String, String> individualCiphertextsOfCommonKey = new HashMap<>();

        // encrypt the common key with each group's individual key
        for (Entry<String, SymmetricKey> e : providedEncryptionKeys.entrySet()) {
            String id = e.getKey();
            SymmetricKey individualKey = e.getValue();

            individualCiphertextsOfCommonKey.put(id,
                cryptographyOperationsProvider.encryptAndEncodeString(individualKey, encodedCommonEncryptionKey));
        }

        return new JsonDataWithOptionalEncryption(encryptedData, individualCiphertextsOfCommonKey);
    }

    /**
     * Encapsulates the check whether a {@link JsonDataWithOptionalEncryption} object represents public data.
     * 
     * @param wrapper the {@link JsonDataWithOptionalEncryption} object
     * @return true if the object represents public data
     */
    public static boolean isPublic(JsonDataWithOptionalEncryption wrapper) {
        return wrapper.getAuthData() == null;
    }

    /**
     * Encapsulates the extraction of the public (i.e. unencrypted) data from a {@link JsonDataWithOptionalEncryption} object.
     * 
     * @param wrapper the {@link JsonDataWithOptionalEncryption} object
     * @return the original data
     */
    public static String getPublicData(JsonDataWithOptionalEncryption wrapper) {
        return wrapper.getData();
    }

    /**
     * Encapsulates the extraction of the set of key ids from a {@link JsonDataWithOptionalEncryption} object.
     * 
     * @param wrapper the {@link JsonDataWithOptionalEncryption} object
     * @return the set of key ids, or an empty set if the data is public
     */
    public static Set<String> getKeyIds(JsonDataWithOptionalEncryption wrapper) {
        final Map<String, String> authData = wrapper.getAuthData();
        if (authData == null) {
            return new HashSet<>();
        } else {
            return authData.keySet(); // TODO (p3) clone or make unmodifiable? (low priority)
        }
    }

    /**
     * Generates a random key, encrypts the provided data with it, and encrypts that common key with each provided individual key. The
     * encrypted input data and the encrypted forms of the common key are returned as a {@link #JsonDataEncryptionUtils()}.
     * 
     * @param wrapper the {@link JsonDataWithOptionalEncryption} object
     * @param keyId the authorization id within the {@link JsonDataWithOptionalEncryption} object that the provided decryption key
     *        corresponds to
     * @param decryptionKey the symmetric key to attempt decryption with
     * @param cryptographyOperationsProvider the cryptography implementation to use
     * @return the generated transfer object
     * 
     * @throws OperationFailureException on decryption failure or error, or if the decrypted string does not "look like" a JSON object
     */
    public static String attemptDecryption(JsonDataWithOptionalEncryption wrapper,
        String keyId, SymmetricKey decryptionKey, CryptographyOperationsProvider cryptographyOperationsProvider)
        throws OperationFailureException {

        final String encryptedCommonKey = wrapper.getAuthData().get(keyId);
        // decrypt the common component data key
        SymmetricKey commonKey = cryptographyOperationsProvider.decodeSymmetricKey(
            cryptographyOperationsProvider.decodeAndDecryptString(decryptionKey, encryptedCommonKey));
        // use the common component data key to decrypt the component data
        final String decryptedComponentData =
            cryptographyOperationsProvider.decodeAndDecryptString(commonKey, wrapper.getData());
        if (isPlausibleJsonObjectString(decryptedComponentData)) {
            return decryptedComponentData;
        } else {
            LogFactory.getLog(JsonDataEncryptionUtils.class)
                .warn("Data decrypted using key " + keyId + " does not match the expected format: " + decryptedComponentData);
            throw new OperationFailureException("Decryption using key " + keyId
                + " was successful, but the resulting data has an unexpected format; see the log file for details");
        }
    }

    /**
     * A simple check whether a decrypted string "looks like" it could be valid data for subsequent deserialization. This is used to detect
     * whether an incorrect key was used to decrypt data that was encrypted with another one.
     * <p>
     * As the current serialization format is a JSON object, the provided string is simply checked for the presence of enclosing "{...}"
     * characters. If future serialization code adds whitespace around this, it is valid to adapt this method to match that, too.
     * 
     * @param data the string to check for plausiblity
     * @return true if the string "looks like" it could be valid deserializable data
     */
    public static boolean isPlausibleJsonObjectString(String data) {
        return (data != null) && data.startsWith("{") && data.endsWith("}");
    }

}
