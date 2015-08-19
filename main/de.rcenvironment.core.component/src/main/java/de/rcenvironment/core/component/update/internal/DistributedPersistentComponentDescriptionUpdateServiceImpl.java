/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.update.internal;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of {@link DistributedPersistentComponentDescriptionUpdateService}.
 * 
 * @author Doreen Seider
 */
public class DistributedPersistentComponentDescriptionUpdateServiceImpl implements DistributedPersistentComponentDescriptionUpdateService {

    private static final Log LOGGER = LogFactory.getLog(DistributedPersistentComponentDescriptionUpdateServiceImpl.class);
    
    private CommunicationService communicationService;

    private BundleContext context;
    
    @Override
    public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, final boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        
        CallablesGroup<Integer> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Integer.class);
        
        final Map<NodeIdentifier, List<PersistentComponentDescription>> sortedDescriptionsMap =
            Collections.unmodifiableMap(sortPersistentComponentDescriptions(descriptions));
        
        for (NodeIdentifier node : sortedDescriptionsMap.keySet()) {
            final NodeIdentifier node2 = node;
            callablesGroup.add(new Callable<Integer>() {

                @Override
                @TaskDescription("Distributed persistent component update check: getFormatVersionsAffectedByUpdate()")
                public Integer call() throws Exception {
                    PersistentComponentDescriptionUpdateService udpateService = (PersistentComponentDescriptionUpdateService)
                        communicationService.getService(PersistentComponentDescriptionUpdateService.class,
                            node2, context);
                    try {
                        return udpateService
                            .getFormatVersionsAffectedByUpdate(sortedDescriptionsMap.get(node2), silent);
                    } catch (UndeclaredThrowableException e) {
                        LOGGER.warn("Failed to check for persistent component updates for node: " + node2, e);
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

        final Map<NodeIdentifier, List<PersistentComponentDescription>> sortedDescriptionsMap =
            Collections.unmodifiableMap(sortPersistentComponentDescriptions(descriptions));
        
        final List<PersistentComponentDescription> unModdescriptions = Collections.unmodifiableList(descriptions);

        CallablesGroup<List> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(List.class);
        
        for (NodeIdentifier node : sortedDescriptionsMap.keySet()) {
            final NodeIdentifier node2 = node;
            callablesGroup.add(new Callable<List>() {
    
                @Override
                @TaskDescription("Distributed persistent component update: performComponentDescriptionUpdates()")
                public List call() throws Exception {
                    PersistentComponentDescriptionUpdateService updateService = (PersistentComponentDescriptionUpdateService)
                        communicationService.getService(PersistentComponentDescriptionUpdateService.class,
                            node2, context);
                    if ((updateService.getFormatVersionsAffectedByUpdate(unModdescriptions, silent) & formatVersion) == formatVersion) {
                        try {
                            return updateService
                                .performComponentDescriptionUpdates(formatVersion, sortedDescriptionsMap.get(node2), silent);
                        } catch (UndeclaredThrowableException e) {
                            LOGGER.warn("Failed to perform persistent component updates for node: " + node2, e);
                            return null;
                        }
                    } else {
                        return sortedDescriptionsMap.get(node2);
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

    protected void activate(BundleContext bundleContext) {
        this.context = bundleContext;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        this.communicationService = newCommunicationService;
    }

    private Map<NodeIdentifier, List<PersistentComponentDescription>> sortPersistentComponentDescriptions(
        List<PersistentComponentDescription> descriptions) {

        Map<NodeIdentifier, List<PersistentComponentDescription>> sortedDescriptions =
            new HashMap<NodeIdentifier, List<PersistentComponentDescription>>();

        for (PersistentComponentDescription description : descriptions) {
            if (!sortedDescriptions.containsKey(description.getComponentNodeIdentifier())) {
                sortedDescriptions.put(description.getComponentNodeIdentifier(), new ArrayList<PersistentComponentDescription>());
            }
            sortedDescriptions.get(description.getComponentNodeIdentifier()).add(description);
        }
        return sortedDescriptions;
    }

}
