/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants class.
 * 
 * @author Sascha Zur
 */
public final class EncrypterComponentConstants {

    /** Component ID. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "encrypter";

    /** Constant for algorithm name. */
    public static final String ALGORITHM_AES = "AES";

    /** Constant for algorithm name. */
    public static final String ALGORITHM_BLOWFISH = "Blowfish";

    /** Array with all encryption algorithms. */
    public static final String[] ALGORITHMS = { ALGORITHM_AES, ALGORITHM_BLOWFISH };

    /** Constant for configuration key. */
    public static final String CONFIG_KEY_ALGORITHM = "encryptionAlgorithm";

    /** Constant for configuration key. */
    public static final String CONFIG_KEY_USE_DEFAULT_PASSWORD = "useDefaultPassword";

    /** Constant for configuration key. */
    public static final String CONFIG_KEY_ENCRYPTION_PASSPHRASE = "encryptionPassphrase";

    /** Constant for the default password. */
    public static final String DEFAULT_PASSWORD = "GoodNewsEveryone";

    /** Name of the static output. */
    public static final String INPUT_NAME_TEXT = "Text";
    
    /** Name of the static output. */
    public static final String OUTPUT_NAME_RESULT = "Encrypted result";

    /** Constant for reading a meta datum. */
    public static final String METADATUM_USAGE_OF_FILE = "usageOfFile";

    /** Name of the static output. */
    public static final String METADATUM_VALUE_USAGE_NAME = "Filename";

    private EncrypterComponentConstants() {

    }
}
