/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.update.internal;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link DistributedPersistentComponentDescriptionUpdateService}.
 * 
 * @author Tobias Brieden
 * @author Doreen Seider
 * 
 *         Note: See note in {@link RemotablePersistentComponentDescriptionUpdateService}. --seid_do
 */
public class DistributedPersistentComponentDescriptionUpdateServiceImpl implements DistributedPersistentComponentDescriptionUpdateService {

    private static final Log LOGGER = LogFactory.getLog(DistributedPersistentComponentDescriptionUpdateServiceImpl.class);

    private CommunicationService communicationService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private LogicalNodeId localLogicalNodeId;

    @Override
    public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, final boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        CallablesGroup<Integer> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Integer.class);

        final Map<LogicalNodeId, List<PersistentComponentDescription>> sortedDescriptionsMap =
            Collections.unmodifiableMap(sortPersistentComponentDescriptions(descriptions));

        for (final LogicalNodeId node : sortedDescriptionsMap.keySet()) {
            callablesGroup.add(new Callable<Integer>() {

                @Override
                @TaskDescription("Distributed persistent component update check: getFormatVersionsAffectedByUpdate()")
                public Integer call() throws Exception {
                    try {
                        RemotablePersistentComponentDescriptionUpdateService udpateService = communicationService
                            .getRemotableService(RemotablePersistentComponentDescriptionUpdateService.class, node);
                        return udpateService.getFormatVersionsAffectedByUpdate(sortedDescriptionsMap.get(node), silent);
                    } catch (RemoteOperationException | RuntimeException e) {
                        LOGGER.warn(StringUtils.format("Failed to check for persistent component updates for node: %s; cause: %s",
                            node, e.toString()));
                        return null;
                    }

                }
            });

            List<Integer> results = callablesGroup.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    LOGGER.warn("Exception during asynchrous execution", e);
                }
            });
            // merge results
            for (Integer singleResult : results) {
                if (singleResult != null) {
                    versionsToUpdate = versionsToUpdate | singleResult;
                }
            }
        }
        return versionsToUpdate;
    }

    @Override
    public List<PersistentComponentDescription> performComponentDescriptionUpdates(final int formatVersion,
        List<PersistentComponentDescription> descriptions, final boolean silent) throws IOException {

        List<PersistentComponentDescription> allUpdatedDescriptions = new ArrayList<PersistentComponentDescription>();

        final Map<LogicalNodeId, List<PersistentComponentDescription>> sortedDescriptionsMap =
            Collections.unmodifiableMap(sortPersistentComponentDescriptions(descriptions));

        final List<PersistentComponentDescription> unModdescriptions = Collections.unmodifiableList(descriptions);

        CallablesGroup<List> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(List.class);

        for (final LogicalNodeId node : sortedDescriptionsMap.keySet()) {
            callablesGroup.add(new Callable<List>() {

                @Override
                @TaskDescription("Distributed persistent component update: performComponentDescriptionUpdates()")
                public List call() throws Exception {
                    RemotablePersistentComponentDescriptionUpdateService updateService = communicationService
                        .getRemotableService(RemotablePersistentComponentDescriptionUpdateService.class, node);
                    if ((updateService.getFormatVersionsAffectedByUpdate(unModdescriptions, silent) & formatVersion) == formatVersion) {
                        try {
                            return updateService
                                .performComponentDescriptionUpdates(formatVersion, sortedDescriptionsMap.get(node), silent);
                        } catch (UndeclaredThrowableException e) {
                            LOGGER.warn("Failed to perform persistent component updates for node: " + node, e);
                            return null;
                        }
                    } else {
                        return sortedDescriptionsMap.get(node);
                    }
                }
            });
        }

        List<List> results = callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                LOGGER.warn("Exception during asynchrous execution", e);
            }
        });

        // merge results
        for (List singleResult : results) {
            if (singleResult != null) {
                allUpdatedDescriptions.addAll(singleResult);
            }
        }
        return allUpdatedDescriptions;
    }

    /**
     * Groups the given {@link PersistentComponentDescription}s by the node on which the update should be performed.
     * 
     */
    private Map<LogicalNodeId, List<PersistentComponentDescription>> sortPersistentComponentDescriptions(
        List<PersistentComponentDescription> descriptions) {

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();

        Map<LogicalNodeId, List<PersistentComponentDescription>> sortedDescriptions =
            new HashMap<LogicalNodeId, List<PersistentComponentDescription>>();

        for (PersistentComponentDescription description : descriptions) {

            LogicalNodeId targetNodeForUpdate = getTargetNodeForUpdate(description, compKnowledge.getAllInstallations());

            if (!sortedDescriptions.containsKey(targetNodeForUpdate)) {
                sortedDescriptions.put(targetNodeForUpdate, new ArrayList<PersistentComponentDescription>());
            }
            sortedDescriptions.get(targetNodeForUpdate).add(description);
        }
        return sortedDescriptions;
    }

    /**
     * Determines for each component on which node its update should be performed.
     * 
     * TODO the logic within this method is highly flawed! The node is often selected at random and there is no clear decision made whether
     * the update should always be made to the newest available version in the network.
     * 
     */
    protected LogicalNodeId getTargetNodeForUpdate(PersistentComponentDescription compDesc,
        Collection<DistributedComponentEntry> compInsts) {

        ComponentInstallation exactlyMatchingComponent = null;

        List<ComponentInstallation> matchingComponents = new ArrayList<ComponentInstallation>();

        // for all registered components which match the persistent one (identifiers are equal and version of persistent one is greater or
        // equal of registered one) decide:
        // if the platform is equal as well, the component [compDesc] is registered on the node where it was when workflow was created, the
        // update
        // check can be directly done on the given node, the description can be returned as it is and this method is done otherwise add the
        // basically matching component to the list of matching components which will be considered later on
        for (DistributedComponentEntry entry : compInsts) {
            ComponentInstallation compInst = entry.getComponentInstallation();
            ComponentInterface compInterface = compInst.getComponentInterface();
            String compId = compInterface.getIdentifierAndVersion();
            if (compId.contains(ComponentConstants.ID_SEPARATOR)) {
                compId = compInterface.getIdentifierAndVersion().split(ComponentConstants.ID_SEPARATOR)[0];
            }
            if (compId.equals(compDesc.getComponentIdentifier())
                && (compDesc.getComponentVersion().equals("")
                    || compInterface.getVersion().compareTo(compDesc.getComponentVersion()) >= 0)) {

                if (compInst.getNodeId() == null
                    || compInst.getNodeId().equals(localLogicalNodeId.getLogicalNodeIdString())) {
                    return localLogicalNodeId;
                } else if (compInst.getNodeId() != null && compDesc.getComponentNodeIdentifier() != null
                    && compInst.getNodeId().equals(compDesc.getComponentNodeIdentifier().getLogicalNodeIdString())) {
                    exactlyMatchingComponent = compInst;
                } else {
                    matchingComponents.add(compInst);
                }
            }
        }

        // if there is not local component, take the exactly matching remote component if there is one
        if (exactlyMatchingComponent != null) {
            return exactlyMatchingComponent.getNodeIdObject();
        }

        // a matching component on the originally registered node was not found. thus set the node
        // identifier of any matching component if there is at least one found
        if (matchingComponents.size() > 0) {
            return matchingComponents.get(0).getNodeIdObject();
        }

        // if there is no matching component found in the RCE network, the local node should be used, thus the local update service will be
        // requested and will return that it has no updater registered for the component as it is not registered at all
        return localLogicalNodeId;

    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        this.communicationService = newCommunicationService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService service) {
        this.componentKnowledgeService = service;
    }

    protected void bindPlatformService(PlatformService platformService) {
        localLogicalNodeId = platformService.getLocalDefaultLogicalNodeId();
    }

}
