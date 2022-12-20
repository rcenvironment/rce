/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Named parameter that accepts a single value.
 * 
 * @author Sebastian Nocke
 *
 */
public class NamedSingleParameter extends NamedParameter {

    private AbstractCommandParameter type;

    public NamedSingleParameter(String parameterName, String infotext, AbstractCommandParameter type) {
        super(parameterName, infotext);
        this.type = type;
    }
    
    public NamedSingleParameter(String parameterName, String infotext, boolean parseWhenNotPresent, AbstractCommandParameter type) {
        super(parameterName, infotext, parseWhenNotPresent);
        this.type = type;
    }
    
    public AbstractCommandParameter getParameterType() {
        return type;
        
    }
    
    public AbstractParsedCommandParameter getStandardValue() {
        return type.standardValue();
        
    }
    
}
