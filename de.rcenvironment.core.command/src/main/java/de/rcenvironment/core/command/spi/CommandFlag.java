/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Defines flags for commands with support for long and short versions for a flag.
 * 
 * @author Sebastian Nocke
 *
 */
public class CommandFlag {

    private String shortFlag;
    private String longFlag;
    private String infotext;
    
    public CommandFlag(String flag) {
        this.shortFlag = flag;
        this.longFlag = flag;
        this.infotext = "This is a command flag";
    }
    
    public CommandFlag(String shortFlag, String longFlag) {
        this.shortFlag = shortFlag;
        this.longFlag = longFlag;
        this.infotext = "This is a command flag";
    }
    
    public CommandFlag(String shortFlag, String longFlag, String description) {
        this.shortFlag = shortFlag;
        this.longFlag = longFlag;
        this.infotext = description;
    }
    
    public String getShortFlag() {
        return shortFlag;
    }
    
    public String getLongFlag() {
        return longFlag;
    }
    
    public String getInfotext() {
        return infotext;
    }
    
    public boolean isFitting(String token) {
        return (shortFlag != null && shortFlag.equals(token)) || (longFlag != null && longFlag.equals(token));
    }
    
}
