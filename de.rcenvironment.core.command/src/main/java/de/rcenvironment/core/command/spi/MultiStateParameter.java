/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.Arrays;
import java.util.Optional;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Command parameter with possible values being defined by the states that are passed upon construction.
 * It is used for parameters that enforce a specific set of possible values.
 * 
 * @author Sebastian Nocke
 *
 */
public class MultiStateParameter extends AbstractCommandParameter {

    private String[] states;
    
    public MultiStateParameter(String name, String desc, String... states) {
        super(name, desc);
        this.states = states;
    }
    
    public String[] getStates() {
        return states;
    }
    
    public String getFormattedStates() {
        String format = "[" + states[0];
        
        for (int i = 1; i < states.length; i++) {
            format += " | " + states[i];
        }
        
        format += "]";
        
        return format;
    }
    
    @Override
    public ParsedStringParameter parseToken(String token, CommandContext context) throws CommandException {
        Optional<String> optional = Arrays.stream(states).filter(state -> token.equalsIgnoreCase(state)).findAny();
        
        if (optional.isPresent()) {
            return new ParsedStringParameter(optional.get());
            
        } else {
            throw CommandException.syntaxError(token + " is not one of the possible states", context);
            
        }
    }

    @Override
    public ParsedStringParameter standardValue() {
        return new ParsedStringParameter(states[0]);
    }
    
}
