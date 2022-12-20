/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.List;

/**
 * Parsed command parameter for a list of values.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedListParameter extends AbstractParsedCommandParameter {

    private List<AbstractParsedCommandParameter> elements;
    
    public ParsedListParameter(List<AbstractParsedCommandParameter> elements) {
        this.elements = elements;
    }
    
    @Override
    public List<AbstractParsedCommandParameter> getResult() {
        return elements;
    }

}
