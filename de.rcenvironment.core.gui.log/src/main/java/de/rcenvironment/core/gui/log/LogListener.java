/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.log;

import java.io.Serializable;

import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.gui.log.internal.LogModel;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link SerializableLogListener} in order to register for log events.
 * 
 * @author Doreen Seider
 */
public class LogListener implements SerializableLogListener {

    private static final long serialVersionUID = 1L;

    private InstanceNodeSessionId platformId;

    public LogListener(InstanceNodeSessionId aPlatformId) {
        platformId = aPlatformId;
    }

    @CallbackMethod
    @Override
    @AllowRemoteAccess
    public void logged(SerializableLogEntry logEntry) {
        if (logEntry.getLevel() != LogService.LOG_DEBUG) {
            logEntry.setPlatformIdentifer(platformId);
            LogModel.getInstance().addLogEntry(logEntry);
        }
    }
    
    @Override
    public Class<? extends Serializable> getInterface() {
        return SerializableLogListener.class;
    }

}
