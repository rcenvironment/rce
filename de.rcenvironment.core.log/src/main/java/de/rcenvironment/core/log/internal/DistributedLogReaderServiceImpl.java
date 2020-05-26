/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.log.DistributedLogReaderService;
import de.rcenvironment.core.log.RemotableLogReaderService;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of the {@link DistributedLogReaderServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (7.0.0 adaptations)
 */
public class DistributedLogReaderServiceImpl implements DistributedLogReaderService {

    private static final Log LOGGER = LogFactory.getLog(DistributedLogReaderServiceImpl.class);

    private CommunicationService communicationService;

    private List<SerializableLogListener> logListeners = new ArrayList<SerializableLogListener>();

    protected void activate(BundleContext bundleContext) {}

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    @Override
    public void addLogListener(SerializableLogListener logListener, InstanceNodeSessionId nodeId) {

        try {
            RemotableLogReaderService service = (RemotableLogReaderService) communicationService
                .getRemotableService(RemotableLogReaderService.class, nodeId);

            service.addLogListener(logListener);
            logListeners.add(logListener);
        } catch (RemoteOperationException e) {
            LOGGER.warn(StringUtils.format("Failed to add remote log listener on %s: %s", nodeId, e.getMessage()));
        }
    }

    @Override
    public List<SerializableLogEntry> getLog(InstanceNodeSessionId nodeId) {
        try {
            RemotableLogReaderService service = (RemotableLogReaderService) communicationService
                .getRemotableService(RemotableLogReaderService.class, nodeId);
            return service.getLog();
        } catch (RemoteOperationException e) {
            LOGGER.warn(StringUtils.format("Failed to get log data from %s: %s", nodeId, e.getMessage()));
            return new LinkedList<SerializableLogEntry>();
        }
    }

    @Override
    public void removeLogListener(SerializableLogListener logListener, InstanceNodeSessionId nodeId) {

        try {
            RemotableLogReaderService service = (RemotableLogReaderService) communicationService
                .getRemotableService(RemotableLogReaderService.class, nodeId);
            service.removeLogListener(logListener);
            logListeners.remove(logListener);
        } catch (RemoteOperationException e) {
            LOGGER.warn(StringUtils.format("Failed to remove remote log listener from %s: %s", nodeId, e.getMessage()));
        }

    }

}
