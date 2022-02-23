/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.internal;

import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationService;

/**
 * A stub to inject a {@link CommandExecutionService} into the static field.
 * 
 * This is needed as {@link InstanceRunner} is abstract, so OSGi-DS can't inject in to it; an alternative would have been to make it
 * non-abstract, but this approach seems cleaner. - misc_ro
 * 
 * @author Robert Mischke
 */
@Component
public class InstanceRunnerStub extends InstanceRunner {

    private static final String THIS_METHOD_SHOULD_NEVER_BE_CALLED = "This method should never be called";

    @Override
    @Reference(cardinality = ReferenceCardinality.MANDATORY, name = "Command Execution Service", policy = ReferencePolicy.STATIC)
    public void bindCommandExecutionService(CommandExecutionService newService) {
        LogFactory.getLog(getClass()).debug("Injecting shared CommandExecutionService");
        super.bindCommandExecutionService(newService);
    }
    
    @Override
    @Reference(cardinality = ReferenceCardinality.MANDATORY, name = "Instance Validation Service", policy = ReferencePolicy.STATIC)
    public void bindInstanceValidationService(InstanceValidationService newService) {
        LogFactory.getLog(getClass()).debug("Injecting shared InstanceStartupValidationService");
        super.bindInstanceValidationService(newService);
    }
    
    @Override
    public int performRun() throws Exception {
        throw new UnsupportedOperationException(THIS_METHOD_SHOULD_NEVER_BE_CALLED);
    }

    @Override
    public void onShutdownRequired(List<InstanceValidationResult> validationResults) {
        throw new UnsupportedOperationException(THIS_METHOD_SHOULD_NEVER_BE_CALLED);
    }

    @Override
    public boolean onRecoveryRequired(List<InstanceValidationResult> validationResults) {
        throw new UnsupportedOperationException(THIS_METHOD_SHOULD_NEVER_BE_CALLED);
    }

    @Override
    public boolean onConfirmationRequired(List<InstanceValidationResult> validationResults) {
        throw new UnsupportedOperationException(THIS_METHOD_SHOULD_NEVER_BE_CALLED);
    }
    
    

}
