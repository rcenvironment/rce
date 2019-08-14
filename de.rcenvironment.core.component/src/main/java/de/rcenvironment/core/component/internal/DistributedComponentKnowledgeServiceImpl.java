/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;

/**
 * Default {@link DistributedComponentKnowledgeService} implementation.
 * 
 * @author Robert Mischke
 */
public class DistributedComponentKnowledgeServiceImpl implements DistributedComponentKnowledgeService, AdditionalServicesProvider {

    // private static final String LIST_OF_INSTALLATIONS_PROPERTY = "componentInstallations";

    private static final String SINGLE_INSTALLATION_PROPERTY_PREFIX = "componentInstallation/";

    private NodePropertiesService nodePropertiesService;

    private final AsyncOrderedCallbackManager<DistributedComponentKnowledgeListener> componentKnowledgeCallbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);

    private volatile DistributedComponentKnowledge currentSnapshot;

    private final InternalModel internalModel;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Internal holder for the dynamic model state. Introduced to make the outer class easier to understand (especially with regards to
     * synchronization).
     * 
     * @author Robert Mischke
     */
    private static final class InternalModel {

        private List<ComponentInstallation> immutableLocalState =
            Collections.unmodifiableList(new ArrayList<ComponentInstallation>());

        private Map<InstanceNodeSessionId, Map<String, ComponentInstallation>> dynamicReceivedState =
            new HashMap<InstanceNodeSessionId, Map<String, ComponentInstallation>>();

        private Map<String, String> dynamicPublishedState =
            new HashMap<String, String>();

    }

    /**
     * Local {@link DistributedComponentKnowledge} implementation that represents a snapshot of the known component installations on
     * reachable nodes.
     * 
     * Note that this class is intended to be immutable for thread-safety, but the contained objects are not immutable yet.
     * 
     * @author Robert Mischke
     */
    private static final class DistributedComponentKnowledgeSnapshot implements DistributedComponentKnowledge {

        private final List<ComponentInstallation> localStateAsList;

        private final List<ComponentInstallation> distributedStateAsList;

        // keys are instance id strings for now; switch to logical node ids to enable publishing by logical node id
        private final Map<String, Collection<ComponentInstallation>> distributedStateAsMap;

        DistributedComponentKnowledgeSnapshot(InternalModel internalModel) {
            synchronized (internalModel) {
                // simply copy the local state object as it is immutable
                localStateAsList = internalModel.immutableLocalState;

                // convert the dynamic received state into a thread-safe, immutable copy
                List<ComponentInstallation> tempGlobalList = new ArrayList<ComponentInstallation>();
                // flatten map to list; make lists in map unmodifiable
                Map<String, Collection<ComponentInstallation>> tempMap =
                    new HashMap<String, Collection<ComponentInstallation>>();
                // extracted to fix formatter/checkstyle issue in for() line
                final Set<Entry<InstanceNodeSessionId, Map<String, ComponentInstallation>>> entrySet =
                    internalModel.dynamicReceivedState.entrySet();
                for (Map.Entry<InstanceNodeSessionId, Map<String, ComponentInstallation>> entry : entrySet) {
                    // extract
                    InstanceNodeSessionId nodeId = entry.getKey();
                    Collection<ComponentInstallation> componentsOfNode = entry.getValue().values();
                    // copy/convert
                    tempGlobalList.addAll(componentsOfNode);
                    
                    //FIXME temporary fix for the problem that published components for a node were overwritten by an empty components
                    //list if older instance session ids for the same node were contained in the internal model
                    //It should be checked if the old instance session ids should be in the model at all (bode_br)
                    ArrayList<ComponentInstallation> componentsToAdd = new ArrayList<ComponentInstallation>(componentsOfNode);
                    if (tempMap.get(nodeId.getInstanceNodeIdString()) != null) {
                        componentsToAdd.addAll(tempMap.get(nodeId.getInstanceNodeIdString()));
                    }
                    tempMap.put(nodeId.getInstanceNodeIdString(),
                        Collections.unmodifiableCollection(componentsToAdd));
                }

                distributedStateAsList = Collections.unmodifiableList(tempGlobalList);
                distributedStateAsMap = Collections.unmodifiableMap(tempMap);
            }
        }

        @Override
        public Collection<ComponentInstallation> getPublishedInstallationsOnNode(ResolvableNodeId nodeId) {
            return distributedStateAsMap.get(nodeId.getInstanceNodeIdString()); // contains immutable collection
        }

        @Override
        public Collection<ComponentInstallation> getAllPublishedInstallations() {
            return distributedStateAsList; // immutable list
        }

        @Override
        public Collection<ComponentInstallation> getLocalInstallations() {
            return localStateAsList;
        }

        @Override
        public Collection<ComponentInstallation> getAllInstallations() {
            Set<ComponentInstallation> allInstallations = new HashSet<ComponentInstallation>(
                distributedStateAsList.size() + localStateAsList.size());
            allInstallations.addAll(distributedStateAsList);
            allInstallations.addAll(localStateAsList);
            return allInstallations;
        }

        @Override
        public String toString() {
            // for debug output only
            return "Local: " + localStateAsList + ", Distributed: " + distributedStateAsMap;
        }

    }

    public DistributedComponentKnowledgeServiceImpl() {

        internalModel = new InternalModel();

        // create placeholder to use until the first update
        currentSnapshot = new DistributedComponentKnowledgeSnapshot(internalModel);

        addDistributedComponentKnowledgeListener(new DistributedComponentKnowledgeListener() {

            @Override
            public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
                if (verboseLogging) {
                    log.debug("Component knowledge updated: " + newState);
                }
            }
        });
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        Collection<AdditionalServiceDeclaration> listenerDeclarations = new ArrayList<AdditionalServiceDeclaration>();
        listenerDeclarations.add(new AdditionalServiceDeclaration(NodePropertiesChangeListener.class,
            new NodePropertiesChangeListenerAdapter() {

                @Override
                public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                    Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                    // forward to outer class
                    updateOnReachableNodePropertiesChanged(addedProperties, updatedProperties, removedProperties);
                }

            }));
        return listenerDeclarations;
    }

    @Override
    public void setLocalComponentInstallations(Collection<ComponentInstallation> allInstallations,
        Collection<ComponentInstallation> installationsToPublish) {
        Map<String, String> delta = new HashMap<String, String>();

        SortedSet<String> uniqueIds = new TreeSet<String>();

        // update the local state and notify listeners
        synchronized (internalModel) {
            internalModel.immutableLocalState = Collections.unmodifiableList(new ArrayList<ComponentInstallation>(allInstallations));

            DistributedComponentKnowledgeSnapshot newSnapshot = new DistributedComponentKnowledgeSnapshot(internalModel);
            // note: callbacks are asynchronous, so triggering them with locks held is safe
            setNewSnapshot(newSnapshot);

            // distribute the new published state if necessary
            for (ComponentInstallation installation : installationsToPublish) {

                String uniqueId = installation.getInstallationId();
                uniqueIds.add(SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId);
                // always serialize to detect changes; could be optimized if changes become frequent
                String serialized = serializeComponentInstallationData(installation);
                String propertyId = SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId;
                if (!serialized.equals(internalModel.dynamicPublishedState.get(propertyId))) {
                    // new or modified
                    log.debug("Publishing component descriptor " + uniqueId);
                    delta.put(propertyId, serialized);
                }
            }

            for (String oldId : internalModel.dynamicPublishedState.keySet()) {
                if (!uniqueIds.contains(oldId) && internalModel.dynamicPublishedState.get(oldId) != null) {
                    // already published, but not public anymore -> remove
                    log.debug("Unpublishing component id " + oldId);
                    delta.put(oldId, null);
                }
            }

            internalModel.dynamicPublishedState.putAll(delta);

            // add "ids of published installations" meta property
            // NOTE: currently unused
            // delta.put(LIST_OF_INSTALLATIONS_PROPERTY,
            // StringUtils.escapeAndConcat(uniqueIds.toArray(new String[uniqueIds.size()])));

            if (!delta.isEmpty()) {
                nodePropertiesService.addOrUpdateLocalNodeProperties(delta);
            }
        }
    }

    @Override
    public DistributedComponentKnowledge getCurrentComponentKnowledge() {
        // TODO ensure that any caller that registers a listener before calling this method
        // can never miss intermediate updates
        return currentSnapshot;
    }

    private void updateOnReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
        Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {

        DistributedComponentKnowledgeSnapshot newSnapshot = null;
        boolean modified = false;
        synchronized (internalModel) {

            for (NodeProperty property : addedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Parsing new component installation property: " + property);
                    }
                    boolean success = processAddedOrUpdatedProperty(property, false);
                    if (success) {
                        modified = true;
                    }
                }
            }
            for (NodeProperty property : updatedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Parsing updated component installation property: " + property);
                    }
                    boolean success = processAddedOrUpdatedProperty(property, true);
                    if (success) {
                        modified = true;
                    }
                }
            }

            for (NodeProperty property : removedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Removing disconnected component installation property: " + property);
                    }
                    boolean success = processRemovedProperty(property);
                    if (success) {
                        modified = true;
                    }
                }
            }

            if (modified) {
                newSnapshot = new DistributedComponentKnowledgeSnapshot(internalModel);
                // note: callbacks are asynchronous, so triggering them with locks held is safe
                setNewSnapshot(newSnapshot);
            }
        }
    }

    protected void addDistributedComponentKnowledgeListener(DistributedComponentKnowledgeListener listener) {
        final DistributedComponentKnowledge knowledgeOnRegistrationTime = currentSnapshot;
        componentKnowledgeCallbackManager.addListenerAndEnqueueCallback(listener,
            new AsyncCallback<DistributedComponentKnowledgeListener>() {

                @Override
                public void performCallback(DistributedComponentKnowledgeListener listener) {
                    listener.onDistributedComponentKnowledgeChanged(knowledgeOnRegistrationTime);
                }
            });
    }

    protected void removeDistributedComponentKnowledgeListener(DistributedComponentKnowledgeListener listener) {
        componentKnowledgeCallbackManager.removeListener(listener);
    }

    protected void bindNodePropertiesService(NodePropertiesService newInstance) {
        nodePropertiesService = newInstance;
    }

    private boolean processAddedOrUpdatedProperty(NodeProperty property, boolean isUpdate) {
        final InstanceNodeSessionId sourceNodeId = property.getInstanceNodeSessionId();
        final String propertyKey = property.getKey().substring(SINGLE_INSTALLATION_PROPERTY_PREFIX.length());
        final String value = property.getValue();
        ComponentInstallation componentInstallation = deserializeComponentInstallationData(value);
        if (componentInstallation == null) {
            log.warn("Ignoring invalid component installation entry published by " + sourceNodeId);
            return false;
        }
        // sanity check: installation property published by same node?
        final LogicalNodeId declaredNodeIdObject = componentInstallation.fetchNodeIdAsObject();
        // TODO >=8.0: improve in case of potential instance id collisions?
        if (!declaredNodeIdObject.isSameInstanceNodeAs(sourceNodeId)) {
            log.error("Ignoring invalid component installation entry: published by node " + sourceNodeId
                + ", but allegedly installed on node " + componentInstallation.getNodeId());
            return false;
        }
        if (componentInstallation.getComponentRevision() == null) {
            log.error("Ignoring invalid component installation entry: 'null' component revision");
            return false;
        }
        if (componentInstallation.getComponentRevision().getComponentInterface() == null) {
            log.error("Ignoring invalid component installation entry: 'null' component interface");
            return false;
        }
        if (componentInstallation.getComponentRevision().getComponentInterface().getIdentifier() == null) {
            log.error("Ignoring invalid component installation entry: 'null' component interface id");
            return false;
        }
        String componentDescriptionId;
        try {
            componentDescriptionId = componentInstallation.getComponentRevision()
                .getComponentInterface().getIdentifier();
        } catch (NullPointerException e) {
            log.warn("Parsed component installation data caused a NPE; ignoring", e);
            return false;
        }
        log.debug("Successfully parsed component installation published by " + sourceNodeId + ": " + componentDescriptionId);
        Map<String, ComponentInstallation> nodeState = internalModel.dynamicReceivedState.get(sourceNodeId);
        if (nodeState == null) {
            nodeState = new HashMap<String, ComponentInstallation>();
            internalModel.dynamicReceivedState.put(sourceNodeId, nodeState);
        }
        ComponentInstallation previousEntry = nodeState.put(propertyKey, componentInstallation);
        // internal consistency checks
        if (isUpdate) {
            if (previousEntry == null) {
                log.warn("Unexpected state: received a property update, but there was no previously registered component for key '"
                    + propertyKey + "'; maybe the previous property could not be parsed?");
            }
        } else {
            if (previousEntry != null) {
                log.warn("Unexpected state: received a new property, but there was a previously registered component already; key="
                    + propertyKey);
            }
        }
        return true;
    }

    private boolean processRemovedProperty(NodeProperty property) {
        final InstanceNodeSessionId nodeId = property.getInstanceNodeSessionId();
        final String propertyKey = property.getKey().substring(SINGLE_INSTALLATION_PROPERTY_PREFIX.length());

        Map<String, ComponentInstallation> nodeState = internalModel.dynamicReceivedState.get(nodeId);
        if (nodeState == null) {
            // a component was unpublished, but it was not known to this node before, so ignore it
            return false;
        }
        ComponentInstallation previousEntry = nodeState.remove(propertyKey);
        if (previousEntry == null) {
            // a component was unpublished, but it was not known to this node before, so ignore it
            return false;
        }
        log.debug("Successfully removed a component installation previously published by " + nodeId + " (key: " + propertyKey + ")");
        return true;
    }

    private boolean isComponentInstallationProperty(NodeProperty property) {
        return property.getKey().startsWith(SINGLE_INSTALLATION_PROPERTY_PREFIX);
    }

    private void setNewSnapshot(final DistributedComponentKnowledge newSnapshot) {
        currentSnapshot = newSnapshot;
        componentKnowledgeCallbackManager.enqueueCallback(new AsyncCallback<DistributedComponentKnowledgeListener>() {

            @Override
            public void performCallback(DistributedComponentKnowledgeListener listener) {
                listener.onDistributedComponentKnowledgeChanged(newSnapshot);
            }
        });
    }

    private String serializeComponentInstallationData(ComponentInstallation ci) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            return mapper.writeValueAsString(ci);
        } catch (IOException e) {
            log.error("Error serializing component descriptor", e);
            return null;
        }
    }

    private ComponentInstallation deserializeComponentInstallationData(String jsonData) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            return mapper.readValue(jsonData, ComponentInstallationImpl.class);
        } catch (IOException e) {
            log.error("Error deserializing component descriptor from JSON data: " + jsonData, e);
            return null;
        }
    }

}
