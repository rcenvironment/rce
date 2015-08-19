/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.datamanagement.DistributedDataReferenceService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Implementation of {@link DistributedDataReferenceServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (parallelized distributed query)
 * @author Jan Flink
 */
public class DistributedDataReferenceServiceImpl implements DistributedDataReferenceService {

    private static final Log LOGGER = LogFactory.getLog(DistributedDataReferenceServiceImpl.class);

    private WorkflowHostService workflowHostService;

    private CommunicationService communicationService;

    private PlatformService platformService;

    private BundleContext context;

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindWorkflowHostService(WorkflowHostService newWorkflowHostService) {
        workflowHostService = newWorkflowHostService;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    @Override
    public DataReference getReference(String dataReferenceKey, NodeIdentifier platform)
        throws AuthorizationException {

        if (platform == null) {
            platform = platformService.getLocalNodeId();
        }
        MetaDataBackendService metaDataBackendService =
            (MetaDataBackendService) communicationService.getService(MetaDataBackendService.class, platform, context);
        try {
            return metaDataBackendService.getDataReference(dataReferenceKey);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to get reference on platform: " + platform, e);
            return null;
        }
    }

    @Override
    public DataReference getReference(String dataReferenceKey) throws AuthorizationException {
        return getReference(dataReferenceKey, workflowHostService.getWorkflowHostNodesAndSelf());
    }

    @Override
    public DataReference getReference(String dataReferenceKey, Collection<NodeIdentifier> platforms)
        throws AuthorizationException {
        DataReference reference = null;

        for (NodeIdentifier pi : workflowHostService.getWorkflowHostNodesAndSelf()) {
            MetaDataBackendService metaDataBackendService =
                (MetaDataBackendService) communicationService.getService(MetaDataBackendService.class, pi, context);
            try {
                reference = metaDataBackendService.getDataReference(dataReferenceKey);
                if (reference != null) {
                    break;
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to get reference on platform: " + pi, e);
            }
        }

        return reference;
    }
}
