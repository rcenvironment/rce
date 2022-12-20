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
 * Command parameter with possible values only being true or false.
 * It is used for parameters that enforce a boolean value.
 * 
 * @author Sebastian Nocke
 *
 */
public class BooleanParameter extends AbstractCommandParameter {

    private Boolean standarValue;
    
    public BooleanParameter(Boolean standardValue, String name, String description) {
        super(name, description);
        standarValue = standardValue;
    }
    
    @Override
    public ParsedBooleanParameter parseToken(String token, CommandContext context) throws CommandException {
        
        if (token.equalsIgnoreCase("true")) {
            return new ParsedBooleanParameter(true);
            
        } else if (token.equalsIgnoreCase("false")) {
            return new ParsedBooleanParameter(false);
            
        } else {
            throw CommandException.syntaxError(token + " must be true or false", context);
            
        }
        
    }

    @Override
    public AbstractParsedCommandParameter standardValue() {
        return new ParsedBooleanParameter(standarValue);
        
    }

}
