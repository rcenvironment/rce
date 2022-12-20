/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Command parameter with possible values being strings.
 * It is used for parameters that a enforce string value.
 * 
 * @author Sebastian Nocke
 *
 */
public class StringParameter extends AbstractCommandParameter {

    private final String standardValue;
    
    public StringParameter(String standardValue, String name, String description) {
        super(name, description);
        this.standardValue = standardValue;
    }
    
    @Override
    public ParsedStringParameter parseToken(String token, CommandContext context) {
        return new ParsedStringParameter(token);
    }

    @Override
    public ParsedStringParameter standardValue() {
        return new ParsedStringParameter(standardValue);
    }
    
}
