/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.binary.Hex;

/**
 * Utility class for generating common types of (usually random) identifiers.
 * 
 * @author Robert Mischke
 */
public final class IdGenerator {

    /**
     * The (arbitrary) upper length limit of generated id strings.
     */
    public static final int MAX_RANDOM_STRING_LENGTH = 64;

    private static final SecureRandom sharedSecureRandom = new SecureRandom(); // thread safe

    private IdGenerator() {
        // prevent instantiation
    }

    /**
     * Generates a random String of hex characters. The requested String length must be even and must not exceed
     * {@link #MAX_RANDOM_STRING_LENGTH}. The {@link IdGeneratorType} parameter select between computationally cheap or cryptographically
     * secure random generation.
     * 
     * @param length the requested string length
     * @param generatorType whether to prioritize speed {@link IdGeneratorType#FAST} or security ( {@link IdGeneratorType#SECURE})
     * @return the generated string
     */
    public static String createRandomHexString(int length, IdGeneratorType generatorType) {
        validateRequestedLength(length);
        byte[] bytes = new byte[length / 2];
        switch (generatorType) {
        case FAST:
            ThreadLocalRandom.current().nextBytes(bytes);
            break;
        case SECURE:
            sharedSecureRandom.nextBytes(bytes);
            break;
        default:
            throw new IllegalArgumentException();
        }
        String string = endodeBytesAsHexAndValidateLength(bytes, length);
        return string;
    }

    /**
     * Generates a computationally cheap random String of hex characters. The requested String length must be even and must not exceed
     * {@link #MAX_RANDOM_STRING_LENGTH}.
     * 
     * @param length the requested string length
     * @return the generated string
     */
    public static String fastRandomHexString(int length) {
        return createRandomHexString(length, IdGeneratorType.FAST);
    }

    /**
     * Generates a cryptographically strong random String of hex characters. The requested String length must be even and must not exceed
     * {@link #MAX_RANDOM_STRING_LENGTH}.
     * 
     * @param length the requested string length
     * @return the generated string
     */
    public static String secureRandomHexString(int length) {
        return createRandomHexString(length, IdGeneratorType.SECURE);
    }

    private static void validateRequestedLength(int length) {
        if (length < 2 || length > MAX_RANDOM_STRING_LENGTH || length % 2 != 0) {
            throw new IllegalArgumentException("Length must be an even number between 2 and " + MAX_RANDOM_STRING_LENGTH);
        }
    }

    private static String endodeBytesAsHexAndValidateLength(byte[] bytes, int length) {
        String string = Hex.encodeHexString(bytes);
        if (string.length() != length) {
            ConsistencyChecks.reportFailure("Unexpected string length: " + string);
        }
        return string;
    }
}
