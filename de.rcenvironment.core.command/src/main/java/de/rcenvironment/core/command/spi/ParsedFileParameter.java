/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.io.File;

/**
 * Parsed command parameter with a file value.
 * 
 * @author Sebastian Nocke
 *
 */
public class ParsedFileParameter extends AbstractParsedCommandParameter {

    private final File file;
    
    public ParsedFileParameter(File file) {
        this.file = file;
    }
    
    @Override
    public File getResult() {
        return file;
    }
    
}
