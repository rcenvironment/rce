/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.log.DistributedLogReaderService;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.log.SerializableLogReaderService;


/**
 * Implementation of the {@link DistributedLogReaderServiceImpl}.
 *
 * @author Doreen Seider
 */
public class DistributedLogReaderServiceImpl implements DistributedLogReaderService {

    private static final Log LOGGER = LogFactory.getLog(DistributedLogReaderServiceImpl.class);

    private CommunicationService communicationService;
    
    private BundleContext context;

    private List<SerializableLogListener> logListeners = new ArrayList<SerializableLogListener>();

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }
    
    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }
    
    @Override
    public void addLogListener(SerializableLogListener logListener, NodeIdentifier nodeId) {
        
        try {
            SerializableLogReaderService service = (SerializableLogReaderService) communicationService
                .getService(SerializableLogReaderService.class, nodeId, context);
            
            service.addLogListener(logListener);
            logListeners.add(logListener);
        } catch (RuntimeException e) {
            LOGGER.warn("Can not add to remote log listener of platform: " + nodeId, e);
        }
    }

    @Override
    public List<SerializableLogEntry> getLog(NodeIdentifier nodeId) {
        try {
            SerializableLogReaderService service = (SerializableLogReaderService) communicationService
                .getService(SerializableLogReaderService.class, nodeId, context);
            return service.getLog();
        } catch (RuntimeException e) {
            LOGGER.warn("Can not get log from remote platform: " + nodeId, e);
            return new LinkedList<SerializableLogEntry>();
        }
    }

    @Override
    public void removeLogListener(SerializableLogListener logListener, NodeIdentifier nodeId) {

        try {
            SerializableLogReaderService service = (SerializableLogReaderService) communicationService
                .getService(SerializableLogReaderService.class, nodeId, context);
            service.removeLogListener(logListener);
            logListeners.remove(logListener);
        } catch (RuntimeException e) {
            LOGGER.warn("Can not remove from remote log listener: " + nodeId, e);            
        }
        
    }

}
