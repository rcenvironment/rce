/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;
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

    private NodeIdentifier platformId;

    public LogListener(NodeIdentifier aPlatformId) {
        platformId = aPlatformId;
    }

    @CallbackMethod
    @Override
    @AllowRemoteAccess
    public void logged(SerializableLogEntry logEntry) {
        logEntry.setPlatformIdentifer(platformId);
        LogModel.getInstance().addLogEntry(logEntry);
    }
    
    @Override
    public Class<? extends Serializable> getInterface() {
        return SerializableLogListener.class;
    }

}
