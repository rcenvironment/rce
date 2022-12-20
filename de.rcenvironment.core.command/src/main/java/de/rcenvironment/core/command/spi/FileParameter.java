/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.io.File;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Command parameter with possible values only being files.
 * It is used for parameters that enforce a file as value.
 * 
 * @author Sebastian Nocke
 *
 */
public class FileParameter extends AbstractCommandParameter {
    
    public FileParameter(String name, String description) {
        super(name, description);
        
    }
    
    @Override
    public ParsedFileParameter parseToken(String token, CommandContext context) throws CommandException {
        File file = new File(token);
        
        return new ParsedFileParameter(file);
    }

    @Override
    public ParsedFileParameter standardValue() {
        return new ParsedFileParameter(null);
    }
    
}
