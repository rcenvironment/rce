/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ToolIntegrationContextRegistry}.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationContextRegistryImpl implements ToolIntegrationContextRegistry {

    private static ToolIntegrationService integrationService;

    private static List<ToolIntegrationContext> integrationInformationToProcess = new LinkedList<ToolIntegrationContext>();

    private final Map<String, ToolIntegrationContext> informationMap = new HashMap<String, ToolIntegrationContext>();

    @Override
    public synchronized void addToolIntegrationContext(ToolIntegrationContext context) {
        if (!informationMap.containsKey(context.getContextId())) {
            informationMap.put(context.getContextId(), context);
            if (integrationService == null) {
                integrationInformationToProcess.add(context);
            } else {
                createIntegrationThread(context);
            }
        }
    }

    private synchronized void createIntegrationThread(final ToolIntegrationContext context) {

        if (!CommandLineArguments.isDoNotStartComponentsRequested()) {
            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                @Override
                @TaskDescription("Read integrated tool folder at startup")
                public void run() {
                    integrationService.readAndIntegratePersistentTools(context);
                }
            });
        }
    }

    @Override
    public void removeToolIntegrationContext(String contextID) {
        if (informationMap.containsKey(contextID)) {
            informationMap.remove(contextID);
        }
    }

    @Override
    public void removeToolIntegrationContext(ToolIntegrationContext context) {
        informationMap.remove(context);
    }

    @Override
    public Collection<ToolIntegrationContext> getAllIntegrationContexts() {
        return informationMap.values();
    }

    @Override
    public ToolIntegrationContext getToolIntegrationContext(String informationID) {
        return informationMap.get(informationID);
    }

    @Override
    public boolean hasId(String informationID) {
        boolean result = false;
        for (ToolIntegrationContext tic : informationMap.values()) {
            if (informationID.contains(tic.getPrefixForComponentId())) {
                result = true;
            }
        }
        return result;
    }

    protected void bindToolIntegrationService(ToolIntegrationService intService) {
        integrationService = intService;
        for (ToolIntegrationContext i : integrationInformationToProcess) {
            createIntegrationThread(i);
        }
    }
}
