/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Command parameter that accepts a list of values.
 * The type of the value is determined by the type field.
 * 
 * @author Sebastian Nocke
 *
 */
public class ListCommandParameter extends AbstractCommandParameter {

    private AbstractCommandParameter type;
    
    public ListCommandParameter(AbstractCommandParameter type, String name, String description) {
        super(name, description);
        this.type = type;
    }
    
    @Override
    public ParsedListParameter parseToken(String token, CommandContext context) throws CommandException {
        
        String[] tokens = splitToken(token);
        
        List<AbstractParsedCommandParameter> elements = new ArrayList<>();
        
        for (int i = 0; i < tokens.length; i++) {
            elements.add(type.parseToken(tokens[i], context));
        }
        
        return new ParsedListParameter(elements);
    }

    @Override
    public AbstractParsedCommandParameter standardValue() {
        return new ParsedListParameter(new ArrayList<>());
    }
    
    private String[] splitToken(String token) {
        
        List<String> tokens = new ArrayList<>();
        int nextComma = token.indexOf(",");
       
        while (nextComma != 0 - 1) {
            
            tokens.add(token.substring(0, nextComma));
            token = token.substring(nextComma + 1);
            
            nextComma = token.indexOf(",");
            
        }
        
        tokens.add(token);
        return tokens.toArray(new String[tokens.size()]);
    }

}
