/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.internal;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationService;

/**
 * A stub to inject a {@link CommandExecutionService} into the static field.
 * 
 * This is needed as {@link InstanceRunner} is abstract, so OSGi-DS can't inject in to it; an alternative would have been to make it
 * non-abstract, but this approach seems cleaner. - misc_ro
 * 
 * @author Robert Mischke
 */
public class InstanceRunnerStub extends InstanceRunner {

    @Override
    public void bindCommandExecutionService(CommandExecutionService newService) {
        LogFactory.getLog(getClass()).debug("Injecting shared CommandExecutionService");
        super.bindCommandExecutionService(newService);
    }
    
    @Override
    public void bindInstanceValidationService(InstanceValidationService newService) {
        LogFactory.getLog(getClass()).debug("Injecting shared InstanceStartupValidationService");
        super.bindInstanceValidationService(newService);
    }
    
    @Override
    public int performRun() throws Exception {
        throw new UnsupportedOperationException("This method should never be called");
    }

}
