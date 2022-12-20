/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Parsed command parameter with a string value.
 * This class is also used to retrieve the value of a {@link MultiStateParameter}.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedStringParameter extends AbstractParsedCommandParameter {

    private final String result;
    
    public ParsedStringParameter(String result) {
        this.result = result;
    }
    
    @Override
    public String getResult() {
        return result;
    }
    
}
