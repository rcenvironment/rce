/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.datamanagement.history;

import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;


/**
 * Constants regarding {@link ComponentHistoryDataItem}s.
 * 
 * @author Doreen Seider
 */
public final class ComponentHistoryDataItemConstants {

    /** File name for stdout as it must be used within history data items. */
    public static final String STDOUT_LOGFILE_NAME = "stdout.log";
    
    /** File name for stderr as it must be used within history data items. */
    public static final String STDERR_LOGFILE_NAME = "stderr.log";
    
    private ComponentHistoryDataItemConstants() {}
    
}
