/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
