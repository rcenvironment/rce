/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.internal.ConsoleRowProcessor;

/**
 * A service to log {@link ConsoleRow} entries in the background.
 * 
 * @author Robert Mischke
 */
public interface ConsoleRowLogService extends ConsoleRowProcessor {

}
