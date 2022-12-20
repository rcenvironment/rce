/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Base class for {@link NamedSingleParameter} and {@link NamedMultiParameter}.
 * These classes define named parameters of commands.
 * 
 * @author Sebastian Nocke
 *
 */
public abstract class NamedParameter {

    private String parameterName;
    private String infotext;
    private boolean parseWhenNotPresent;
    
    public NamedParameter(String parameterName, String infotext) {
        this.parameterName = parameterName;
        this.infotext = infotext;
        this.parseWhenNotPresent = true;
    }
    
    public NamedParameter(String parameterName, String infotext, boolean parseWhenNotPresent) {
        this.parameterName = parameterName;
        this.infotext = infotext;
        this.parseWhenNotPresent = parseWhenNotPresent;
    }
    
    public String getName() {
        return parameterName;
    }
    
    public String getInfotext() {
        return infotext;
    }
    
    public boolean getParseWhenNotPresent() {
        return parseWhenNotPresent;
    }
    
    abstract AbstractParsedCommandParameter getStandardValue();
    
}
