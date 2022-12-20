/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

// TODO For RCE 10.5. probably rename this class

/**
 * Named parameter that accepts multiple values.
 * 
 * @author Sebastian Nocke
 *
 */
public class NamedMultiParameter extends NamedParameter {

    private AbstractCommandParameter[] types;
    
    private int minParameters;
    
    public NamedMultiParameter(String parameterName, String infotext, int minParameters, AbstractCommandParameter... types) {
        super(parameterName, infotext);
        this.minParameters = minParameters;
        this.types = types;
    }
    
    public NamedMultiParameter(String parameterName, String infotext, boolean parseWhenNotPresent,
            int minParameters, AbstractCommandParameter... types) {
        super(parameterName, infotext, parseWhenNotPresent);
        this.minParameters = minParameters;
        this.types = types;
    }
    
    public int getMinParameters() {
        return minParameters;
        
    }
    
    public AbstractCommandParameter[] getParameterTypes() {
        return types;
        
    }

    public ParsedMultiParameter getStandardValue() {
        
        AbstractParsedCommandParameter[] standardValues = new AbstractParsedCommandParameter[types.length];
        
        for (int i = 0; i < types.length; i++) {
            standardValues[i] = types[i].standardValue();
        }
        
        return new ParsedMultiParameter(standardValues);
        
    }
    
}
