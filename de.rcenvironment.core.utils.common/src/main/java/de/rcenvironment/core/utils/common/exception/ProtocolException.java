/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.exception;

import java.io.IOException;

/**
 * An exception for network protocol errors, including protocol version mismatches. Made a subclass of IOException as most places will need
 * to throw/catch both low-level and protocol exceptions anyway, so this allows them to simply catch {@link IOException}, and inspect the
 * exception type only where it is relevant.
 *
 * @author Robert Mischke
 */
public class ProtocolException extends IOException {

    private static final long serialVersionUID = 8408311689963407342L;

    public ProtocolException(String message) {
        super(message);
    }

}
