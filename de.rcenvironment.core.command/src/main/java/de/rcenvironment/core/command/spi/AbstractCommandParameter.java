/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Base class for {@link StringParameter}, {@link IntegerParameter}, {@link BooleanParameter},
 * {@link FileParameter}, {@link MultiStateParameter}, and {@link ListCommandParameter}.
 * These classes define parameters of commands.
 * 
 * @author Sebastian Nocke
 *
 */
public abstract class AbstractCommandParameter {

    protected String description;
    private String name;
    
    public AbstractCommandParameter(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public abstract AbstractParsedCommandParameter parseToken(String token, CommandContext context) throws CommandException;
    
    public abstract AbstractParsedCommandParameter standardValue();
    
}
