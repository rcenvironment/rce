/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupListener;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.DistributedNodeComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.component.management.internal.ComponentDataConverter;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;

/**
 * Default {@link DistributedComponentKnowledgeService} implementation.
 * 
 * @author Robert Mischke
 * @author Alexander Weinert (added inaccessibleRemoteEntriesByNodeId)
 */
@Component
public class DistributedComponentKnowledgeServiceImpl
    implements DistributedComponentKnowledgeService, AdditionalServicesProvider {

    // private static final String LIST_OF_INSTALLATIONS_PROPERTY = "componentInstallations";

    private static final String SINGLE_INSTALLATION_PROPERTY_PREFIX = "componentInstallation/";

    private NodePropertiesService nodePropertiesService;

    private final AsyncOrderedCallbackManager<DistributedComponentKnowledgeListener> componentKnowledgeCallbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);

    private volatile DistributedComponentKnowledgeSnapshot currentSnapshot;

    // TODO could be made more specific by using InstanceNodeSessionId as map key; requires change of query methods first, though
    private Map<String, DistributedNodeComponentKnowledge> mutableMapOfRemoteEntriesForNextSnapshot = new HashMap<>();

    // keeps track of all component-related node properties to re-parse them when local group authorization changes
    private Map<String, NodeProperty> knownComponentNodeProperties = new HashMap<>();

    private Map<String, String> lastPublishedProperties = new HashMap<>();

    private final Object internalStateLock = new Object();

    private InstanceNodeSessionId localInstanceSessionId;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    private AuthorizationService authorizationService;

    /**
     * Local {@link DistributedComponentKnowledge} implementation that represents a snapshot of the known component installations on
     * reachable nodes.
     * 
     * Note that this class is intended to be immutable for thread-safety, but the contained objects are not immutable yet.
     * 
     * @author Robert Mischke
     * @author Alexander Weinert (keeping track of inaccessible remote entries)
     */
    private static final class DistributedComponentKnowledgeSnapshot implements DistributedComponentKnowledge {

        private final Collection<DistributedComponentEntry> allLocalEntries;

        private final Collection<DistributedComponentEntry> localAccessEntries;

        private final Collection<DistributedComponentEntry> sharedAccessEntries;

        private final Collection<DistributedComponentEntry> remoteEntries;

        // keys are instance id strings for now; switch to logical node ids to enable publishing by logical node id
        // This map only contains those remote components that are accessible to the local instance, i.e., those components that are
        // published either publicly or in a publication group that the current instance has access to
        private final Map<String, DistributedNodeComponentKnowledge> remoteEntriesByNodeId;

        private final InstanceNodeSessionId localInstanceSessionId;

        // private final Map<String, Collection<DistributedComponentEntry>> remoteEntriesByNodeId;

        private DistributedComponentKnowledgeSnapshot(InstanceNodeSessionId localInstanceSessionIdParam,
            Collection<DistributedComponentEntry> allLocalEntriesParam,
            Collection<DistributedComponentEntry> localAccessEntriesParam, Collection<DistributedComponentEntry> sharedAccessEntriesParam,
            Collection<DistributedComponentEntry> remoteEntriesParam,
            Map<String, DistributedNodeComponentKnowledge> remoteEntriesByNodeIdParam) {

            this.localInstanceSessionId = localInstanceSessionIdParam;

            // make collections immutable for publication; note that this relies on the caller not changing them after this call
            this.allLocalEntries = Collections.unmodifiableCollection(allLocalEntriesParam);
            this.localAccessEntries = Collections.unmodifiableCollection(localAccessEntriesParam);
            this.sharedAccessEntries = Collections.unmodifiableCollection(sharedAccessEntriesParam);
            this.remoteEntries = Collections.unmodifiableCollection(remoteEntriesParam);
            this.remoteEntriesByNodeId = Collections.unmodifiableMap(remoteEntriesByNodeIdParam);
        }

        /**
         * Creates an initial empty snapshot.
         * 
         * @param localInstanceSessionIdParam the local instance's id; needed to check whether a node query is about the local node
         */
        private DistributedComponentKnowledgeSnapshot(InstanceNodeSessionId localInstanceSessionIdParam) {
            this(localInstanceSessionIdParam, new ArrayList<DistributedComponentEntry>(), new ArrayList<DistributedComponentEntry>(),
                new ArrayList<DistributedComponentEntry>(), new ArrayList<DistributedComponentEntry>(),
                new HashMap<String, DistributedNodeComponentKnowledge>());
        }

        public DistributedComponentKnowledgeSnapshot updateWithNewLocalInstallations(
            Collection<DistributedComponentEntry> allLocalInstallationsParam, boolean publicationEnabled) {

            // defensive copy of the parameter list
            List<DistributedComponentEntry> allLocalInstallations = new ArrayList<>(allLocalInstallationsParam);

            // holders for more specific collections
            Collection<DistributedComponentEntry> tempLocalAccessEntries = new ArrayList<>();
            Collection<DistributedComponentEntry> tempLocalSharedAccessEntries = new ArrayList<>();

            for (DistributedComponentEntry e : allLocalInstallations) {
                switch (e.getType()) {
                case LOCAL:
                case FORCED_LOCAL:
                    tempLocalAccessEntries.add(e);
                    break;
                case SHARED:
                    if (publicationEnabled) {
                        tempLocalSharedAccessEntries.add(e);
                    } else {
                        // suppress publication by adding it to the "local only" set instead
                        tempLocalAccessEntries.add(e);
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
                }
            }

            return new DistributedComponentKnowledgeSnapshot(this.localInstanceSessionId, allLocalInstallations, tempLocalAccessEntries,
                tempLocalSharedAccessEntries, this.remoteEntries, this.remoteEntriesByNodeId);
        }

        public DistributedComponentKnowledgeSnapshot updateWithNewRemoteEntryMap(
            Map<String, DistributedNodeComponentKnowledge> newMapOfRemoteEntries) {
            Collection<DistributedComponentEntry> tempListOfRemoteEntries = new ArrayList<>();
            for (DistributedNodeComponentKnowledge nodeMap : newMapOfRemoteEntries.values()) {
                tempListOfRemoteEntries.addAll(nodeMap.getAccessibleComponents());
            }
            return new DistributedComponentKnowledgeSnapshot(this.localInstanceSessionId, this.allLocalEntries, this.localAccessEntries,
                this.sharedAccessEntries, tempListOfRemoteEntries, newMapOfRemoteEntries);
        }

        @Override
        public Collection<DistributedComponentEntry> getKnownSharedInstallationsOnNode(ResolvableNodeId nodeId,
            boolean includeInaccessible) {
            if (nodeId.isSameInstanceNodeAs(localInstanceSessionId)) {
                return sharedAccessEntries;
            }

            DistributedNodeComponentKnowledge remoteEntriesOfNode = remoteEntriesByNodeId.get(nodeId.getInstanceNodeIdString());
            if (remoteEntriesOfNode == null) {
                return new ArrayList<>();
            }

            final Collection<DistributedComponentEntry> returnValue = new ArrayList<>();
            returnValue.addAll(remoteEntriesOfNode.getAccessibleComponents());
            if (includeInaccessible) {
                returnValue.addAll(remoteEntriesOfNode.getInaccessibleComponents());
            }
            return returnValue;
        }

        @Override
        public Collection<DistributedComponentEntry> getKnownSharedInstallations() {
            Collection<DistributedComponentEntry> allInstallations = new ArrayList<>(remoteEntries.size() + sharedAccessEntries.size());
            allInstallations.addAll(remoteEntries);
            allInstallations.addAll(sharedAccessEntries);
            return allInstallations;
        }

        @Override
        public Collection<DistributedComponentEntry> getAllLocalInstallations() {
            return allLocalEntries;
        }

        @Override
        public Collection<DistributedComponentEntry> getLocalAccessInstallations() {
            return localAccessEntries;
        }

        @Override
        public Collection<DistributedComponentEntry> getSharedAccessInstallations() {
            return sharedAccessEntries;
        }

        @Override
        public Collection<DistributedComponentEntry> getAllInstallations() {
            Collection<DistributedComponentEntry> allInstallations = new ArrayList<>(remoteEntries.size() + allLocalEntries.size());
            allInstallations.addAll(remoteEntries);
            allInstallations.addAll(allLocalEntries);
            return allInstallations;
        }

        @Override
        public String toString() {
            // for debug output only
            return "Local: " + allLocalEntries + ", Remote: " + remoteEntriesByNodeId;
        }

    }

    public DistributedComponentKnowledgeServiceImpl() {

        addDistributedComponentKnowledgeListener(newState -> {
            if (verboseLogging) {
                log.debug("Component knowledge updated: " + newState);
            }
        });

    }

    /**
     * Test constructor; if this is used, #bindNodeConfigurationService does not have to be bound.
     * 
     * @param nodeSessionId the simulated id of the local node
     */
    protected DistributedComponentKnowledgeServiceImpl(InstanceNodeSessionId nodeSessionId) {
        this(); // inherit default constructor
        this.localInstanceSessionId = nodeSessionId;
    }

    /**
     * OSGi-DS activation method.
     */
    @Activate
    public void activate() {
        synchronized (internalStateLock) {
            // create placeholder to use until the first update
            setNewSnapshot(new DistributedComponentKnowledgeSnapshot(localInstanceSessionId));
        }
    }

    @Reference(unbind = "unbindObjectBindingsService")
    protected void bindObjectBindingsService(ObjectBindingsService objectBindingsService) {
        objectBindingsService.addBinding(AuthorizationAccessGroupListener.class, accessGroups -> {
            synchronized (internalStateLock) {
                // re-parse all remote node properties by triggering "property updated" code on each of them; "false" = internal update
                updateOnReachableNodePropertiesChanged(new ArrayList<NodeProperty>(),
                    new ArrayList<>(knownComponentNodeProperties.values()), new ArrayList<NodeProperty>(), false);
            }
        }, this); // this = owner
    }

    protected void unbindObjectBindingsService(ObjectBindingsService objectBindingsService) {
        objectBindingsService.removeAllBindingsOfOwner(this);
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        Collection<AdditionalServiceDeclaration> listenerDeclarations = new ArrayList<>();
        listenerDeclarations.add(new AdditionalServiceDeclaration(NodePropertiesChangeListener.class,
            new NodePropertiesChangeListenerAdapter() {

                @Override
                public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                    Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                    // forward to outer class; "true" = actual remote update
                    updateOnReachableNodePropertiesChanged(addedProperties, updatedProperties, removedProperties, true);
                }

            }));
        return listenerDeclarations;
    }

    @Override
    public void updateLocalComponentInstallations(Collection<DistributedComponentEntry> allLocalInstallations, boolean publicationEnabled) {

        Map<String, String> propertiesDelta = new HashMap<>();

        synchronized (internalStateLock) {

            // note: this simple approach is only valid to suppress partial updates on startup, ie switching once from false to true;
            // TODO also support switching from true to false, which should unpublish all existing properties
            if (!publicationEnabled && !allLocalInstallations.isEmpty()) {
                log.debug("Not publishing local component information yet (usually as part of the startup process)");
                return;
            }

            DistributedComponentKnowledgeSnapshot newSnapshot =
                currentSnapshot.updateWithNewLocalInstallations(allLocalInstallations, publicationEnabled);

            // note: callbacks are asynchronous, so triggering them with locks held is safe
            setNewSnapshot(newSnapshot);

            // update the publication data if necessary
            final Collection<String> uniqueIds = new TreeSet<>();
            for (DistributedComponentEntry entry : newSnapshot.getSharedAccessInstallations()) {
                final ComponentInstallation installation = entry.getComponentInstallation();
                // ignore/skip non-shared component entries
                if (entry.getType() != DistributedComponentEntryType.SHARED) {
                    // TODO decide whether these should have been filtered out already, or if filtering is supposed to happen here
                    continue;
                }
                final String uniqueId = installation.getInstallationId();
                uniqueIds.add(SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId);
                // TODO add installation data hash for more efficient change detection
                final String serializedEntryData = entry.getPublicationData();
                if (serializedEntryData == null) {
                    log.error("Skipping component publishing of " + uniqueId + " as it was not properly serialized");
                    continue;
                }
                final String propertyId = SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId;
                if (!serializedEntryData.equals(lastPublishedProperties.get(propertyId))) {
                    // new or modified
                    log.debug("Publishing component descriptor " + uniqueId);
                    propertiesDelta.put(propertyId, serializedEntryData);
                }
            }

            for (Map.Entry<String, String> entry : lastPublishedProperties.entrySet()) {
                if (!uniqueIds.contains(entry.getKey()) && entry.getValue() != null) {
                    // already published, but not public anymore -> remove
                    log.debug("Unpublishing component id " + entry.getKey());
                    propertiesDelta.put(entry.getKey(), null);
                }
            }

        }
        if (!propertiesDelta.isEmpty()) {
            // add "ids of published installations" meta property
            // NOTE: currently unused
            // delta.put(LIST_OF_INSTALLATIONS_PROPERTY,
            // StringUtils.escapeAndConcat(uniqueIds.toArray(new String[uniqueIds.size()])));
            lastPublishedProperties.putAll(propertiesDelta);
            nodePropertiesService.addOrUpdateLocalNodeProperties(propertiesDelta);
        }
    }

    @Override
    public DistributedComponentKnowledge getCurrentSnapshot() {
        // TODO ensure that any caller that registers a listener before calling this method
        // can never miss intermediate updates
        synchronized (internalStateLock) {
            return currentSnapshot;
        }
    }

    /**
     * @param isActualRemoteUpdate false if {@link #knownComponentNodeProperties} are being re-parsed on a local authorization change; in
     *        this case, there is no need to update {@link #knownComponentNodeProperties} with the "new" properties
     */
    private void updateOnReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
        Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties,
        boolean isActualRemoteUpdate) {

        boolean modified = false;
        synchronized (internalStateLock) {

            for (NodeProperty property : addedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Parsing new component installation property: " + property);
                    }
                    if (isActualRemoteUpdate) {
                        knownComponentNodeProperties.put(property.getDistributedUniqueKey(), property);
                    } else {
                        throw new IllegalStateException();
                    }
                    // We intentionally do not use the |= operator here, as doing so would short-circuit the evaluation of
                    // processAddedOrUpdatedProperty. That method, however, updates internal state of this object and thus has to be called
                    // for each update.
                    modified = processAddedOrUpdatedProperty(property, false) || modified;
                }
            }
            for (NodeProperty property : updatedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Parsing updated component installation property: " + property);
                    }
                    if (isActualRemoteUpdate) {
                        knownComponentNodeProperties.put(property.getDistributedUniqueKey(), property);
                    }
                    // We intentionally do not use the |= operator here, as doing so would short-circuit the evaluation of
                    // processAddedOrUpdatedProperty. That method, however, updates internal state of this object and thus has to be called
                    // for each update.
                    modified = processAddedOrUpdatedProperty(property, true) || modified;
                }
            }

            for (NodeProperty property : removedProperties) {
                if (isComponentInstallationProperty(property)) {
                    if (verboseLogging) {
                        log.debug("Removing disconnected component installation property: " + property);
                    }
                    if (isActualRemoteUpdate) {
                        knownComponentNodeProperties.remove(property.getDistributedUniqueKey());
                    } else {
                        throw new IllegalStateException();
                    }
                    // We intentionally do not use the |= operator here, as doing so would short-circuit the evaluation of
                    // processAddedOrUpdatedProperty. That method, however, updates internal state of this object and thus has to be called
                    // for each update.
                    modified = processRemovedProperty(property) || modified;
                }
            }

            if (modified) {
                DistributedComponentKnowledgeSnapshot newSnapshot =
                    currentSnapshot.updateWithNewRemoteEntryMap(mutableMapOfRemoteEntriesForNextSnapshot);
                // note: callbacks are asynchronous, so triggering them with locks held is safe
                setNewSnapshot(newSnapshot);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, // force line break
        unbind = "removeDistributedComponentKnowledgeListener")
    protected void addDistributedComponentKnowledgeListener(DistributedComponentKnowledgeListener listener) {
        final DistributedComponentKnowledge knowledgeAtRegistrationTime = currentSnapshot;
        componentKnowledgeCallbackManager.addListenerAndEnqueueCallback(listener,
            knowledgeListener -> knowledgeListener.onDistributedComponentKnowledgeChanged(knowledgeAtRegistrationTime));
    }

    protected void removeDistributedComponentKnowledgeListener(DistributedComponentKnowledgeListener listener) {
        componentKnowledgeCallbackManager.removeListener(listener);
    }

    @Reference
    protected void bindNodePropertiesService(NodePropertiesService newInstance) {
        nodePropertiesService = newInstance;
    }

    @Reference
    protected void bindNodeConfigurationService(NodeConfigurationService newInstance) {
        localInstanceSessionId = newInstance.getInstanceNodeSessionId();
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
    }

    // TODO this method has grown too big; should be refactored -- misc_ro, Dec 2018
    private boolean processAddedOrUpdatedProperty(NodeProperty property, boolean isUpdate) {
        final InstanceNodeSessionId sourceNodeId = property.getInstanceNodeSessionId();

        if (sourceNodeId.equals(localInstanceSessionId)) {
            // log.debug("Ignoring component property update published by the local node");
            return false;
        }
        final String propertyKey = property.getKey().substring(SINGLE_INSTALLATION_PROPERTY_PREFIX.length());
        final String jsonData = property.getValue();
        final DistributedComponentEntry newEntry;
        try {
            newEntry = ComponentDataConverter.deserializeRemoteDistributedComponentEntry(jsonData, authorizationService);
        } catch (OperationFailureException e) {
            log.warn(
                "Ignoring invalid component installation entry published by " + sourceNodeId + "(" + e.getMessage() + "): " + jsonData);
            return false;
        }

        final boolean newEntryIsAccessible = newEntry.isAccessible();
        if (newEntryIsAccessible) {
            // We only validate the component installation is the entry is accessible, since otherwise no information on the published
            // component is transmitted, i.e., in particular not the component installation
            final boolean componentInstallationValid = validateDistributedComponentEntry(newEntry, sourceNodeId);
            if (!componentInstallationValid) {
                return false;
            }
        }

        final String remoteNodeKey = sourceNodeId.getInstanceNodeIdString();
        // Map#getOrDefault does not enter the given default into the map if the key is not yet associated. Since we, however, enter a
        // new value for the given key into the map in the next step, we do not care about this.
        final DistributedNodeComponentKnowledge nodeState =
            mutableMapOfRemoteEntriesForNextSnapshot.getOrDefault(remoteNodeKey, DistributedNodeComponentKnowledgeImpl.createEmpty());

        final DistributedComponentEntry previousEntry = nodeState.getComponent(propertyKey);
        if (newEntryIsAccessible) {
            mutableMapOfRemoteEntriesForNextSnapshot.put(remoteNodeKey, nodeState.putAccessibleComponent(propertyKey, newEntry));

            // Logging. Not refactored to own method due to overly specific set of parameters.
            final boolean previousEntryExists = previousEntry != null;
            if (isUpdate && !previousEntryExists) {
                log.debug("Added a new local entry for remote component id " + propertyKey
                    + " after a remote node property update; typically, this is because local or remote "
                    + "authorization settings have changed, and now allow access to this component");
            } else if (!isUpdate && previousEntryExists) {
                log.warn("Unexpected state: received a new property, but there was a previously registered component already; key="
                    + propertyKey);
            }
        } else {
            mutableMapOfRemoteEntriesForNextSnapshot.put(remoteNodeKey, nodeState.putInaccessibleComponent(propertyKey, newEntry));

            // Logging. Not refactored to own method due to overly specific set of parameters.
            final boolean wasPreviouslyAccessible = nodeState.componentExists(propertyKey) && nodeState.isComponentAccessible(propertyKey);
            if (wasPreviouslyAccessible) {
                log.debug("Removing remote component entry " + propertyKey + " from " + sourceNodeId
                    + " as there is no matching local access group anymore; authorized remote access groups are: "
                    + newEntry.getDeclaredPermissionSet());
            } else {
                log.debug("Ignoring remote component entry " + propertyKey + " from " + sourceNodeId
                    + " as there is no local authorized group matching its access groups " + newEntry.getDeclaredPermissionSet());
            }
        }

        return true;
    }

    /**
     * A distributed component entry is valid if - its contained component installation is valid according to
     * {@link #validateComponentInstallation(ComponentInstallation, InstanceNodeSessionId)}, and - the ids contained in its component
     * interface are valid according to
     * {@link ComponentIdRules#validateComponentInterfaceIds(de.rcenvironment.core.component.model.api.ComponentInterface)}. As a side
     * effect, this method logs a debugging- or error message denoting either the reason for validation errors or the successful parse.
     * 
     * @param newEntry The DistributedComponentEntry to be checked
     * @param sourceNodeId
     * @return True if the given distributed component entry is valid, false otherwise.
     */
    private boolean validateDistributedComponentEntry(final DistributedComponentEntry newEntry,
        final InstanceNodeSessionId sourceNodeId) {
        final ComponentInstallation componentInstallation = newEntry.getComponentInstallation();
        // TODO >=8.0: improve in case of potential instance id collisions?
        final Optional<String> componentInstallationError = validateComponentInstallation(componentInstallation, sourceNodeId);
        if (componentInstallationError.isPresent()) {
            log.error("Ignoring invalid component installation entry: " + componentInstallationError.get());
            return false;
        }

        final Optional<String> idValidationError =
            ComponentIdRules.validateComponentInterfaceIds(componentInstallation.getComponentInterface());
        if (idValidationError.isPresent()) {
            log.error(StringUtils.format("Ignoring invalid component installation %s published by node %s as it contains an invalid id: %s",
                componentInstallation.getInstallationId(), sourceNodeId, idValidationError.get()));
            return false;
        }

        final String componentDescriptionId = componentInstallation.getComponentInterface().getIdentifierAndVersion();
        log.debug("Successfully parsed component installation published by " + sourceNodeId + ": " + componentDescriptionId);
        return true;
    }

    /**
     * A component installation is valid if - the node it claims to be installed on is equal to the node actually publishing the component -
     * its component revision is not null - its component interface is not null - its identifier and version are not null.
     * 
     * @param componentInstallation The component installation to check
     * @param sourceNodeId The id of the node that published this component installation
     * @return An empty optional if the given component installation is valid, an optional containing a human-readable error message
     *         otherwise.
     */
    private Optional<String> validateComponentInstallation(ComponentInstallation componentInstallation,
        InstanceNodeSessionId sourceNodeId) {
        final LogicalNodeId declaredNodeIdObject = componentInstallation.getNodeIdObject();
        if (!declaredNodeIdObject.isSameInstanceNodeAs(sourceNodeId)) {
            return Optional
                .of("published by node " + sourceNodeId + ", but allegedly installed on node " + componentInstallation.getNodeId());
        }
        if (componentInstallation.getComponentRevision() == null) {
            return Optional.of("'null' component revision");
        }
        if (componentInstallation.getComponentInterface() == null) {
            return Optional.of("'null' component interface");
        }
        if (componentInstallation.getComponentInterface().getIdentifierAndVersion() == null) {
            return Optional.of("'null' component interface id");
        }
        return Optional.empty();
    }

    private boolean processRemovedProperty(NodeProperty property) {
        final InstanceNodeSessionId sourceNodeId = property.getInstanceNodeSessionId();
        final String remoteNodeKey = sourceNodeId.getInstanceNodeIdString();

        DistributedNodeComponentKnowledge nodeState = mutableMapOfRemoteEntriesForNextSnapshot.get(remoteNodeKey);
        if (nodeState == null) {
            // a component was unpublished, but the node was not known to this node before, so ignore it
            return false;
        }

        final String propertyKey = property.getKey().substring(SINGLE_INSTALLATION_PROPERTY_PREFIX.length());
        if (!nodeState.componentExists(propertyKey)) {
            // a component was unpublished, but it was not known to this node before, so ignore it
            return false;
        }

        mutableMapOfRemoteEntriesForNextSnapshot.put(remoteNodeKey, nodeState.removeComponent(propertyKey));
        log.debug("Successfully removed a component installation previously published by " + sourceNodeId + " (key: " + propertyKey + ")");
        return true; // map modified
    }

    private boolean isComponentInstallationProperty(NodeProperty property) {
        return property.getKey().startsWith(SINGLE_INSTALLATION_PROPERTY_PREFIX);
    }

    private void setNewSnapshot(final DistributedComponentKnowledgeSnapshot newSnapshot) {
        currentSnapshot = newSnapshot;
        mutableMapOfRemoteEntriesForNextSnapshot = new HashMap<>(newSnapshot.remoteEntriesByNodeId);
        componentKnowledgeCallbackManager.enqueueCallback(listener -> listener.onDistributedComponentKnowledgeChanged(newSnapshot));
    }

}
