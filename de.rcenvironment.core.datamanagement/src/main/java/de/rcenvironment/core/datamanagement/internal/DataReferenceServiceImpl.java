/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.util.Collection;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.datamanagement.DataReferenceService;
import de.rcenvironment.core.datamanagement.RemotableMetaDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link DataReferenceServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (parallelized distributed query)
 * @author Jan Flink
 */
public class DataReferenceServiceImpl implements DataReferenceService {

    private WorkflowHostService workflowHostService;

    private CommunicationService communicationService;

    private PlatformService platformService;

    protected void activate(BundleContext bundleContext) {}

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
    public DataReference getReference(String dataReferenceKey, NetworkDestination platform)
        throws CommunicationException {

        if (platform == null) {
            platform = platformService.getLocalInstanceNodeSessionId();
        }
        try {
            return getRemoteMetaDataBackendService(platform).getDataReference(dataReferenceKey);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to get data reference from remote node @%s: ",
                platform)
                + e.getMessage());
        }
    }

    @Override
    public DataReference getReference(String dataReferenceKey) throws CommunicationException {
        return getReference(dataReferenceKey, workflowHostService.getWorkflowHostNodesAndSelf());
    }

    @Override
    public DataReference getReference(String dataReferenceKey, Collection<? extends NetworkDestination> platforms)
        throws CommunicationException {
        DataReference reference = null;

        for (InstanceNodeSessionId pi : workflowHostService.getWorkflowHostNodesAndSelf()) {
            try {
                reference = getRemoteMetaDataBackendService(pi).getDataReference(dataReferenceKey);
                if (reference != null) {
                    break;
                }
            } catch (RemoteOperationException e) {
                throw new CommunicationException(StringUtils.format("Failed to get data reference from remote node @%s: ",
                    pi)
                    + e.getMessage());
            }
        }

        return reference;
    }

    private RemotableMetaDataService getRemoteMetaDataBackendService(NetworkDestination nodeId) throws RemoteOperationException {
        return (RemotableMetaDataService) communicationService.getRemotableService(RemotableMetaDataService.class, nodeId);
    }
}
