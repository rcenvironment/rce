/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
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
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
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
    // Note: this map currently only contains accessible remote components; it is not tracking those without local authorization!
    private Map<String, Map<String, DistributedComponentEntry>> mutableMapOfRemoteEntriesForNextSnapshot = new HashMap<>();

    private Map<String, Map<String, DistributedComponentEntry>> mutableMapOfInaccessibleRemoteEntriesForNextSnapshot = new HashMap<>();

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
     */
    private static final class DistributedComponentKnowledgeSnapshot implements DistributedComponentKnowledge {

        private final Collection<DistributedComponentEntry> allLocalEntries;

        private final Collection<DistributedComponentEntry> localAccessEntries;

        private final Collection<DistributedComponentEntry> sharedAccessEntries;

        private final Collection<DistributedComponentEntry> remoteEntries;

        // keys are instance id strings for now; switch to logical node ids to enable publishing by logical node id
        // This map only contains those remote components that are accessible to the local instance, i.e., those components that are
        // published either publicly or in a publication group that the current instance has access to
        private final Map<String, Map<String, DistributedComponentEntry>> remoteEntriesByNodeId;

        // keys are instance id strings for now; switch to logical node ids to enable publishing by logical node id
        private final Map<String, Map<String, DistributedComponentEntry>> inaccessibleRemoteEntriesByNodeId;

        private final InstanceNodeSessionId localInstanceSessionId;

        // private final Map<String, Collection<DistributedComponentEntry>> remoteEntriesByNodeId;

        private DistributedComponentKnowledgeSnapshot(InstanceNodeSessionId localInstanceSessionIdParam,
            Collection<DistributedComponentEntry> allLocalEntriesParam,
            Collection<DistributedComponentEntry> localAccessEntriesParam, Collection<DistributedComponentEntry> sharedAccessEntriesParam,
            Collection<DistributedComponentEntry> remoteEntriesParam,
            Map<String, Map<String, DistributedComponentEntry>> remoteEntriesByNodeIdParam,
            Map<String, Map<String, DistributedComponentEntry>> inaccessibleRemoteEntriesByNodeIdParam) {

            this.localInstanceSessionId = localInstanceSessionIdParam;

            // make collections immutable for publication; note that this relies on the caller not changing them after this call
            this.allLocalEntries = Collections.unmodifiableCollection(allLocalEntriesParam);
            this.localAccessEntries = Collections.unmodifiableCollection(localAccessEntriesParam);
            this.sharedAccessEntries = Collections.unmodifiableCollection(sharedAccessEntriesParam);
            this.remoteEntries = Collections.unmodifiableCollection(remoteEntriesParam);
            this.remoteEntriesByNodeId = Collections.unmodifiableMap(remoteEntriesByNodeIdParam);
            this.inaccessibleRemoteEntriesByNodeId = Collections.unmodifiableMap(inaccessibleRemoteEntriesByNodeIdParam);
        }

        /**
         * Creates an initial empty snapshot.
         * 
         * @param localInstanceSessionIdParam the local instance's id; needed to check whether a node query is about the local node
         */
        private DistributedComponentKnowledgeSnapshot(InstanceNodeSessionId localInstanceSessionIdParam) {
            this(localInstanceSessionIdParam, new ArrayList<DistributedComponentEntry>(), new ArrayList<DistributedComponentEntry>(),
                new ArrayList<DistributedComponentEntry>(), new ArrayList<DistributedComponentEntry>(),
                new HashMap<String, Map<String, DistributedComponentEntry>>(),
                new HashMap<String, Map<String, DistributedComponentEntry>>());
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
                tempLocalSharedAccessEntries, this.remoteEntries, this.remoteEntriesByNodeId, this.inaccessibleRemoteEntriesByNodeId);
        }

        public DistributedComponentKnowledgeSnapshot updateWithNewRemoteEntryMap(
            Map<String, Map<String, DistributedComponentEntry>> newMapOfRemoteEntries,
            Map<String, Map<String, DistributedComponentEntry>> newMapOfInaccessibleRemoteEntries) {
            Collection<DistributedComponentEntry> tempListOfRemoteEntries = new ArrayList<>();
            for (Map<String, DistributedComponentEntry> nodeMap : newMapOfRemoteEntries.values()) {
                for (DistributedComponentEntry e : nodeMap.values()) {
                    tempListOfRemoteEntries.add(e);
                }
            }
            return new DistributedComponentKnowledgeSnapshot(this.localInstanceSessionId, this.allLocalEntries, this.localAccessEntries,
                this.sharedAccessEntries, tempListOfRemoteEntries, newMapOfRemoteEntries, newMapOfInaccessibleRemoteEntries);
        }

        @Override
        public Collection<DistributedComponentEntry> getKnownSharedInstallationsOnNode(ResolvableNodeId nodeId,
            boolean includeInaccessible) {
            if (nodeId.isSameInstanceNodeAs(localInstanceSessionId)) {
                return sharedAccessEntries;
            } else {
                Map<String, DistributedComponentEntry> remoteEntriesOfNode =
                    remoteEntriesByNodeId.get(nodeId.getInstanceNodeIdString());
                if (remoteEntriesOfNode != null) {
                    if (includeInaccessible) {
                        final Map<String, DistributedComponentEntry> inaccessibleRemoteEntries =
                            inaccessibleRemoteEntriesByNodeId.get(nodeId.getInstanceNodeIdString());
                        remoteEntriesOfNode = new HashMap<>(remoteEntriesOfNode);
                        if (inaccessibleRemoteEntries != null) {
                            remoteEntriesOfNode.putAll(inaccessibleRemoteEntries);
                        }
                    }
                    return remoteEntriesOfNode.values(); // value set of an immutable map
                } else {
                    return new ArrayList<>();
                }
            }
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

        addDistributedComponentKnowledgeListener(new DistributedComponentKnowledgeListener() {

            @Override
            public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
                if (verboseLogging) {
                    log.debug("Component knowledge updated: " + newState);
                }
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
        objectBindingsService.addBinding(AuthorizationAccessGroupListener.class, new AuthorizationAccessGroupListener() {

            @Override
            public void onAvailableAuthorizationAccessGroupsChanged(List<AuthorizationAccessGroup> accessGroups) {
                synchronized (internalStateLock) {
                    // re-parse all remote node properties by triggering "property updated" code on each of them; "false" = internal update
                    updateOnReachableNodePropertiesChanged(new ArrayList<NodeProperty>(),
                        new ArrayList<>(knownComponentNodeProperties.values()), new ArrayList<NodeProperty>(), false);
                }
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

            // new DistributedComponentKnowledgeSnapshot(internalModel, allLocalInstallations, localInstanceSessionId);

            // note: callbacks are asynchronous, so triggering them with locks held is safe
            setNewSnapshot(newSnapshot);

            // update the publication data if necessary
            SortedSet<String> uniqueIds = new TreeSet<>();
            for (DistributedComponentEntry entry : newSnapshot.getSharedAccessInstallations()) {
                ComponentInstallation installation = entry.getComponentInstallation();
                // ignore/skip non-shared component entries
                if (entry.getType() != DistributedComponentEntryType.SHARED) {
                    // TODO decide whether these should have been filtered out already, or if filtering is supposed to happen here
                    continue;
                }
                String uniqueId = installation.getInstallationId();
                uniqueIds.add(SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId);
                // TODO add installation data hash for more efficient change detection
                String serializedEntryData = entry.getPublicationData();
                if (serializedEntryData == null) {
                    log.error("Skipping component publishing of " + uniqueId + " as it was not properly serialized");
                    continue;
                }
                String propertyId = SINGLE_INSTALLATION_PROPERTY_PREFIX + uniqueId;
                if (!serializedEntryData.equals(lastPublishedProperties.get(propertyId))) {
                    // new or modified
                    log.debug("Publishing component descriptor " + uniqueId);
                    propertiesDelta.put(propertyId, serializedEntryData);
                }
            }

            for (String oldId : lastPublishedProperties.keySet()) {
                if (!uniqueIds.contains(oldId) && lastPublishedProperties.get(oldId) != null) {
                    // already published, but not public anymore -> remove
                    log.debug("Unpublishing component id " + oldId);
                    propertiesDelta.put(oldId, null);
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
                    currentSnapshot.updateWithNewRemoteEntryMap(mutableMapOfRemoteEntriesForNextSnapshot,
                        mutableMapOfInaccessibleRemoteEntriesForNextSnapshot);
                // note: callbacks are asynchronous, so triggering them with locks held is safe
                setNewSnapshot(newSnapshot);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, // force line break
        unbind = "removeDistributedComponentKnowledgeListener")
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
        final String remoteNodeKey = sourceNodeId.getInstanceNodeIdString();

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
            log.warn("Ignoring invalid component installation entry published by " + sourceNodeId + ": " + jsonData);
            return false;
        }

        if (!newEntry.isAccessible()) {
            Map<String, DistributedComponentEntry> nodeState = mutableMapOfRemoteEntriesForNextSnapshot.get(remoteNodeKey);
            final boolean wasPreviouslyAccessible;
            if (nodeState != null) {
                wasPreviouslyAccessible = (nodeState.remove(propertyKey) != null);
                // TODO remove empty sub-maps?
            } else {
                wasPreviouslyAccessible = false;
            }

            if (!mutableMapOfInaccessibleRemoteEntriesForNextSnapshot.containsKey(remoteNodeKey)) {
                mutableMapOfInaccessibleRemoteEntriesForNextSnapshot.put(remoteNodeKey, new HashMap<>());
            }
            final Map<String, DistributedComponentEntry> nodeStateForInaccessibleRemoteEntries =
                mutableMapOfInaccessibleRemoteEntriesForNextSnapshot.get(remoteNodeKey);
            nodeStateForInaccessibleRemoteEntries.put(propertyKey, newEntry);

            if (wasPreviouslyAccessible) {
                log.debug("Removing remote component entry " + propertyKey + " from " + sourceNodeId
                    + " as there is no matching local access group anymore; authorized remote access groups are: "
                    + newEntry.getDeclaredPermissionSet());
            } else {
                log.debug("Ignoring remote component entry " + propertyKey + " from " + sourceNodeId
                    + " as there is no local authorized group matching its access groups " + newEntry.getDeclaredPermissionSet());
            }

            return true;
        }
        final ComponentInstallation componentInstallation = newEntry.getComponentInstallation();
        // sanity check: installation property published by same node?
        final LogicalNodeId declaredNodeIdObject = componentInstallation.getNodeIdObject();
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
        if (componentInstallation.getComponentInterface() == null) {
            log.error("Ignoring invalid component installation entry: 'null' component interface");
            return false;
        }
        if (componentInstallation.getComponentInterface().getIdentifierAndVersion() == null) {
            log.error("Ignoring invalid component installation entry: 'null' component interface id");
            return false;
        }
        final String componentDescriptionId;
        try {
            componentDescriptionId = componentInstallation.getComponentInterface().getIdentifierAndVersion();
        } catch (NullPointerException e) {
            log.warn("Parsed component installation data caused a NPE; ignoring", e);
            return false;
        }

        final Optional<String> idValidationError =
            ComponentIdRules.validateComponentInterfaceIds(componentInstallation.getComponentInterface());
        if (idValidationError.isPresent()) {
            log.error(StringUtils.format("Ignoring invalid component installation %s published by node %s as it contains an invalid id: %s",
                componentInstallation.getInstallationId(), sourceNodeId, idValidationError.get()));
            return false;
        }

        log.debug("Successfully parsed component installation published by " + sourceNodeId + ": " + componentDescriptionId);
        final Map<String, DistributedComponentEntry> nodeState;
        if (mutableMapOfRemoteEntriesForNextSnapshot.containsKey(remoteNodeKey)) {
            nodeState = mutableMapOfRemoteEntriesForNextSnapshot.get(remoteNodeKey);
        } else {
            nodeState = new HashMap<>();
            mutableMapOfRemoteEntriesForNextSnapshot.put(remoteNodeKey, nodeState);
        }
        
        if (mutableMapOfInaccessibleRemoteEntriesForNextSnapshot.containsKey(remoteNodeKey)) {
            final Map<String, DistributedComponentEntry> inaccessibleNodeState =
                mutableMapOfInaccessibleRemoteEntriesForNextSnapshot.get(remoteNodeKey);
            inaccessibleNodeState.remove(propertyKey);
        }

        final DistributedComponentEntry previousEntry = nodeState.put(propertyKey, newEntry);
        // internal consistency checks
        if (isUpdate) {
            if (previousEntry == null) {
                log.debug("Added a new local entry for remote component id " + propertyKey
                    + " after a remote node property update; typically, this is because local or remote "
                    + "authorization settings have changed, and now allow access to this component");
            }
        } else {
            if (previousEntry != null) {
                log.warn("Unexpected state: received a new property, but there was a previously registered component already; key="
                    + propertyKey);
            }
        }
        return true; // map modified
    }

    private boolean processRemovedProperty(NodeProperty property) {
        final InstanceNodeSessionId sourceNodeId = property.getInstanceNodeSessionId();
        final String remoteNodeKey = sourceNodeId.getInstanceNodeIdString();
        final String propertyKey = property.getKey().substring(SINGLE_INSTALLATION_PROPERTY_PREFIX.length());
        
        boolean modified = false;

        // There are three possibilities for the just removed property: Either it was not known to this node before, it was known, but the
        // component it referred to was inaccessible, or it was known and the component was accessible. In the former case, there is nothing
        // to do. In the second case, the entry is stored in the mutableMapOfInaccessibleRemoteEntriesForNextSnapshot, in the third case it
        // is stored it the mutableMapOfRemoteEntriesForNextSnapshot.
        modified |=
            tryRemovePropertyFromMap(sourceNodeId, remoteNodeKey, propertyKey, mutableMapOfRemoteEntriesForNextSnapshot);
        modified |=
            tryRemovePropertyFromMap(sourceNodeId, remoteNodeKey, propertyKey, mutableMapOfInaccessibleRemoteEntriesForNextSnapshot);
        
        return modified;
    }

    /**
     * @return True if the given property of the given remoteNode was contained in the given map, false otherwise
     */
    private boolean tryRemovePropertyFromMap(final InstanceNodeSessionId sourceNodeId, final String remoteNodeKey, final String propertyKey,
        final Map<String, Map<String, DistributedComponentEntry>> map) {
        Map<String, DistributedComponentEntry> nodeState = map.get(remoteNodeKey);
        if (nodeState == null) {
            // a component was unpublished, but it was not known to this node before, so ignore it
            return false;
        }
        DistributedComponentEntry previousEntry = nodeState.remove(propertyKey);
        if (previousEntry == null) {
            // a component was unpublished, but it was not known to this node before, so ignore it
            return false;
        }
        log.debug("Successfully removed a component installation previously published by " + sourceNodeId + " (key: " + propertyKey + ")");
        return true; // map modified
    }

    private boolean isComponentInstallationProperty(NodeProperty property) {
        return property.getKey().startsWith(SINGLE_INSTALLATION_PROPERTY_PREFIX);
    }

    private void setNewSnapshot(final DistributedComponentKnowledgeSnapshot newSnapshot) {
        currentSnapshot = newSnapshot;
        mutableMapOfRemoteEntriesForNextSnapshot = new HashMap<>(newSnapshot.remoteEntriesByNodeId);
        mutableMapOfInaccessibleRemoteEntriesForNextSnapshot = new HashMap<>(newSnapshot.inaccessibleRemoteEntriesByNodeId);

        componentKnowledgeCallbackManager.enqueueCallback(new AsyncCallback<DistributedComponentKnowledgeListener>() {

            @Override
            public void performCallback(DistributedComponentKnowledgeListener listener) {
                listener.onDistributedComponentKnowledgeChanged(newSnapshot);
            }
        });
    }

}
