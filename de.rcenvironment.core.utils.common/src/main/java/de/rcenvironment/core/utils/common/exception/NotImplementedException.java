/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.exception;

/**
 * A simple placeholder {@link RuntimeException} to throw if an unimplemented method has been called unexpectedly. Obviously, such
 * exceptions should only be temporary, and not be present in release code.
 *
 * @author Robert Mischke
 */
public class NotImplementedException extends RuntimeException {

    private static final long serialVersionUID = -4901239269773742673L;

}
