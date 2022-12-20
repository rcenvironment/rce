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
 * Container for a command executing method and the corresponding context.
 * 
 * @author Sebastian Nocke
 *
 */
public class ExecutableCommand {

    private final SingleCommandHandler handler;
    
    private final CommandContext context;
    
    public ExecutableCommand(SingleCommandHandler handler, CommandContext context) {
        this.handler = handler;
        this.context = context;
    }

    public void execute() throws CommandException {
        handler.execute(context);
        context.getOutputReceiver().onFinished();
    }
    
    public SingleCommandHandler getHandler() {
        return handler;
        
    }
    
    public CommandContext getContext() {
        return context;
        
    }
    
}
