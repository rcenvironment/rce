/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Parsed command parameter values of a {@link NamedMultiParameter}.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedMultiParameter extends AbstractParsedCommandParameter {

    private AbstractParsedCommandParameter[] parsedParameters;
    
    public ParsedMultiParameter(AbstractParsedCommandParameter[] parsedParameters) {
        this.parsedParameters = parsedParameters;
    }
    
    @Override
    public AbstractParsedCommandParameter[] getResult() {
        return parsedParameters;
        
    }

}
