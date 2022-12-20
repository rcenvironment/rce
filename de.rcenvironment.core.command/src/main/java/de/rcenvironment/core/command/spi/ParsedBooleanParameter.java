/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Parsed command parameter with a boolean value.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedBooleanParameter extends AbstractParsedCommandParameter {

    private Boolean result;
    
    public ParsedBooleanParameter(Boolean result) {
        this.result = result;
    }
    
    @Override
    public Boolean getResult() {
        return result;
    }

}
