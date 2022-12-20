/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Parsed command parameter value with an integer value.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedIntegerParameter extends AbstractParsedCommandParameter {

    private final Integer number;
    
    public ParsedIntegerParameter(Integer number) {
        this.number = number;
    }
    
    @Override
    public Integer getResult() {
        return number;
    }

}
