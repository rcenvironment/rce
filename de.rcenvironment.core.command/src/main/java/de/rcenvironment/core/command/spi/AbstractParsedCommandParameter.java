/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Base class for {@link ParsedStringParameter}, {@link ParsedIntegerParameter}, {@link ParsedBooleanParameter},
 * {@link ParsedFileParameter}, {@link ParsedMultiStateParameter}, and {@link ParsedListCommandParameter}.
 * Used in command implementation methods in order to retrieve parameter values.
 * 
 * @author Sebastian Nocke
 *
 */
public abstract class AbstractParsedCommandParameter {

    public abstract Object getResult();
    
}
