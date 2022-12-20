/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Command parameter with possible values only being integers.
 * It is used for parameters that enforce an integer value.
 * 
 * @author Sebastian Nocke
 *
 */
public class IntegerParameter extends AbstractCommandParameter {

    private final Integer standardValue;
    
    public IntegerParameter(Integer standarValue, String name, String description) {
        super(name, description);
        this.standardValue = standarValue;
    }
    
    @Override
    public ParsedIntegerParameter parseToken(String token, CommandContext context) throws CommandException {
        
        try {
            int number = Integer.parseInt(token);
            return new ParsedIntegerParameter(number);
        } catch (NumberFormatException e) {
            throw CommandException.syntaxError("integer parameter could not be parsed", context);
        }
        
    }

    @Override
    public ParsedIntegerParameter standardValue() {
        return new ParsedIntegerParameter(standardValue);
    }
    
}
