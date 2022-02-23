/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * A service encapsulating running a {@link Runnable} as required by ToolIntegration.
 * 
 * @author Alexander Weinert
 */
@Component(service = RunnerService.class)
public class RunnerService {

    /**
     * @param categoryName The name for the category of runnable submitted.
     * @param runnable Some runnable that shall be executed.
     * @param taskId A string explanation of the task performed by the given runnable.
     */
    // TODO this needs an API solution for providing the task description / category name in the new concept -- misc_ro
    public void execute(String categoryName, Runnable runnable, String taskId) {
        ConcurrencyUtils.getAsyncTaskService().execute(categoryName, taskId, runnable);
    }
}
