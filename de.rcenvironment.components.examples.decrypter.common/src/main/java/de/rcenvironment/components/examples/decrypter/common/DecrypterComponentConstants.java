/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants class.
 * 
 * @author Sascha Zur
 */
public final class DecrypterComponentConstants {

    /** Component ID. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "decrypter";

    /** Constant for algorithm name. */
    public static final String ALGORITHM_AES = "AES";

    /** Constant for algorithm name. */
    public static final String ALGORITHM_BLOWFISH = "Blowfish";

    /** Array with all encryption algorithms. */
    public static final String[] ALGORITHMS = { ALGORITHM_AES, ALGORITHM_BLOWFISH };

    /** Constant for configuration key. */
    public static final String CONFIG_KEY_ALGORITHM = "decryptionAlgorithm";

    /** Constant for configuration key. */
    public static final String CONFIG_KEY_USEDEFAULTPASSWORD = "useDefaultPassword";

    /** Constant for configuration key. */
    public static final String CONIG_KEY_DECRYPTING_PASSPHRASE = "decryptingPassphrase";

    /** Constant for static input name. */
    public static final String INPUT_NAME_FILE = "Encrypted File";

    /** Constant for the default password. */
    public static final String DEFAULT_PASSWORD = "GoodNewsEveryone";

    /** Constant for static output name. */
    public static final String OUTPUT_NAME_RESULT = "Decrypted result";

    private DecrypterComponentConstants() {

    }
}
