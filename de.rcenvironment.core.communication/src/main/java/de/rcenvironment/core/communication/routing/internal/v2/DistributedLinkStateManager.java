/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListenerAdapter;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;

/**
 * Manager class that creates and publishes the local link state, and keeps track of the link states announced by other nodes in the
 * network.
 * 
 * It interacts with the rest of the system by listening for {@link MessageChannel} events (which define the local link state), publishing
 * the local link state as a {@link NodeProperty} via {@link NodePropertiesService}, and listening for {@link NodeProperty} change events to
 * collect all published link states, including the self-published one for consistency.
 * 
 * @author Robert Mischke
 */
public class DistributedLinkStateManager implements AdditionalServicesProvider {

    private static final String LSA_PROPERTY_KEY = "lsa";

    private final AsyncOrderedCallbackManager<LinkStateKnowledgeChangeListener> callbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private NodePropertiesService nodePropertiesService;

    private NodeConfigurationService nodeConfigurationService;

    private final Map<String, Link> localOutgoingLinks;

    private volatile Map<InstanceNodeSessionId, LinkState> linkStateKnowledgeSnapshot;

    private volatile LinkState localLinkStateSnapshot;

    private InstanceNodeSessionId localNodeId;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    private boolean localNodeIsRelay;

    private NodeIdentifierService nodeIdentifierService;

    public DistributedLinkStateManager() {
        localOutgoingLinks = new HashMap<String, Link>();
        linkStateKnowledgeSnapshot = Collections.unmodifiableMap(new HashMap<InstanceNodeSessionId, LinkState>());
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public synchronized void activate() {
        localNodeId = nodeConfigurationService.getInstanceNodeSessionId();
        localNodeIsRelay = nodeConfigurationService.isRelay();
        localLinkStateSnapshot = new LinkState(localOutgoingLinks.values());
        setNewLocalLinkState(localLinkStateSnapshot);
        if (!localNodeIsRelay) {
            // in non-relay mode, publish an empty pseudo link state to make sure no old property circulates in the network
            String serializedEmptyLinkState = LinkStateSerializer.serialize(new ArrayList<Link>());
            nodePropertiesService.addOrUpdateLocalNodeProperty(LSA_PROPERTY_KEY, serializedEmptyLinkState);
        }
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        Collection<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(RawNodePropertiesChangeListener.class, new RawNodePropertiesChangeListener() {

            @Override
            public void onRawNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties) {
                // forward to main class method
                updateOnNodePropertiesAddedOrModified(newProperties);
            }
        }));
        result.add(new AdditionalServiceDeclaration(MessageChannelLifecycleListener.class, new MessageChannelLifecycleListenerAdapter() {

            @Override
            public void onOutgoingChannelTerminated(MessageChannel connection) {
                // forward to main class method
                updateOnOutgoingChannelTerminated(connection);
            }

            @Override
            public void onOutgoingChannelEstablished(MessageChannel connection) {
                // forward to main class method
                updateOnOutgoingChannelEstablished(connection);
            }
        }));
        return result;
    }

    // linkStateKnowledgeSnapshot is defined as volatile
    public Map<InstanceNodeSessionId, LinkState> getCurrentKnowledge() {
        return linkStateKnowledgeSnapshot;
    }

    /**
     * OSGi-DS "bind" method; made public for integration testing.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindNodePropertiesService(NodePropertiesService newInstance) {
        nodePropertiesService = newInstance;
    }

    /**
     * OSGi-DS "bind" method; made public for integration testing.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindNodeConfigurationService(NodeConfigurationService newInstance) {
        this.nodeConfigurationService = newInstance;
        this.nodeIdentifierService = nodeConfigurationService.getNodeIdentifierService();
    }

    /**
     * OSGi-DS "bind" method; made public for integration testing.
     * 
     * @param listener the new listener instance to add
     */
    public synchronized void addLinkStateKnowledgeChangeListener(LinkStateKnowledgeChangeListener listener) {
        // copy reference in synchronized block
        final Map<InstanceNodeSessionId, LinkState> currentKnowledgeSnapshotCopy = linkStateKnowledgeSnapshot;
        // send initial update
        callbackManager.addListenerAndEnqueueCallback(listener, new AsyncCallback<LinkStateKnowledgeChangeListener>() {

            @Override
            public void performCallback(LinkStateKnowledgeChangeListener listener) {
                listener.onLinkStateKnowledgeChanged(currentKnowledgeSnapshotCopy);
            }
        });
    }

    /**
     * OSGi-DS "unbind" method; made public for integration testing.
     * 
     * @param listener the new listener instance to remove
     */
    public void removeLinkStateKnowledgeChangeListener(LinkStateKnowledgeChangeListener listener) {
        callbackManager.removeListener(listener);
    }

    private synchronized void updateOnNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties) {
        // only used if a relevant change is detected
        Map<InstanceNodeSessionId, LinkState> deltaMap = null;
        for (NodeProperty property : newProperties) {
            if (property.getKey().equals(LSA_PROPERTY_KEY)) {
                String linkStateData = property.getValue();
                InstanceNodeSessionId updateSourceInstanceSessionId = property.getInstanceNodeSessionId();
                if (localNodeId.isSameInstanceNodeSessionAs(updateSourceInstanceSessionId)) {
                    // ignore LSA properties for the local node as they can differ from the actual
                    // local link state in relay mode
                    continue;
                }
                // log.debug("Received LSA data for " + nodeId + ": " + linkStateData);
                try {
                    LinkState deserialized = LinkStateSerializer.deserialize(linkStateData);
                    // log.debug("Parsed LSA: " + deserialized);
                    // lazy init; also serves as marker that there have been relevant changes
                    if (deltaMap == null) {
                        deltaMap = new HashMap<InstanceNodeSessionId, LinkState>();
                    }
                    deltaMap.put(updateSourceInstanceSessionId, deserialized);
                } catch (IOException e) {
                    log.error("Ignoring unreadable link state update for node " + updateSourceInstanceSessionId, e);
                }
            }
        }
        if (deltaMap != null) {
            if (verboseLogging) {
                StringBuilder buffer = new StringBuilder();
                String locationInfo = "";
                if (localNodeId != null) {
                    locationInfo = " " + localNodeId.toString();
                }
                buffer.append(StringUtils.format("Detected %d LSA property changes%s: ", deltaMap.size(), locationInfo));
                for (Entry<InstanceNodeSessionId, LinkState> entry : deltaMap.entrySet()) {
                    buffer.append(StringUtils.format("\n  %s -> %s", entry.getKey(), entry.getValue().getLinks()));
                }
                log.debug(buffer.toString());
            }
            mergeIntoEffectiveLinkStateKnowledge(deltaMap);
        }
    }

    private synchronized void updateOnOutgoingChannelEstablished(MessageChannel connection) {
        final String linkId = connection.getChannelId();
        final String remoteInstanceNodeSessionIdString = connection.getRemoteNodeInformation().getInstanceNodeSessionIdString();
        final Link link = new Link(linkId, remoteInstanceNodeSessionIdString);
        localOutgoingLinks.put(linkId, link);
        localLinkStateSnapshot = new LinkState(localOutgoingLinks.values());
        setNewLocalLinkState(localLinkStateSnapshot);
    }

    private synchronized void updateOnOutgoingChannelTerminated(MessageChannel connection) {
        localOutgoingLinks.remove(connection.getChannelId());
        localLinkStateSnapshot = new LinkState(localOutgoingLinks.values());
        setNewLocalLinkState(localLinkStateSnapshot);
    }

    private void setNewLocalLinkState(final LinkState linkState) {
        if (localNodeIsRelay) {
            // in relay mode, publish the local link state via node properties; the local node
            // then consumes its own link state like the ones published by other nodes - misc_ro
            String serialized = LinkStateSerializer.serialize(linkState.getLinks());
            nodePropertiesService.addOrUpdateLocalNodeProperty(LSA_PROPERTY_KEY, serialized);
        }
        // regardless of relay or non-relay mode, merge the local link state into the effective link
        // state knowledge; to make sure this is not overwritten by the empty pseudo link state
        // property in non-relay mode, "received" local property changes must be ignored - misc_ro
        Map<InstanceNodeSessionId, LinkState> deltaMap = new HashMap<InstanceNodeSessionId, LinkState>();
        deltaMap.put(localNodeId, linkState);
        mergeIntoEffectiveLinkStateKnowledge(deltaMap);

        // TODO move/merge into "mergeIntoEffectiveLinkStateKnowledge"?
        callbackManager.enqueueCallback(new AsyncCallback<LinkStateKnowledgeChangeListener>() {

            @Override
            public void performCallback(LinkStateKnowledgeChangeListener listener) {
                listener.onLocalLinkStateUpdated(linkState);
            }
        });
    }

    // NOTE: must be called from synchronized methods only!
    private void mergeIntoEffectiveLinkStateKnowledge(Map<InstanceNodeSessionId, LinkState> deltaMap) {
        // there have been relevant changes, so replace the current knowledge
        Map<InstanceNodeSessionId, LinkState> tempMap = new HashMap<InstanceNodeSessionId, LinkState>(linkStateKnowledgeSnapshot);
        tempMap.putAll(deltaMap);
        linkStateKnowledgeSnapshot = Collections.unmodifiableMap(tempMap);
        // create immutable copy of new knowledge reference and delta map in synchronized block
        final Map<InstanceNodeSessionId, LinkState> knowledgeSnapshotCopy = linkStateKnowledgeSnapshot;
        final Map<InstanceNodeSessionId, LinkState> deltaMapCopy = Collections.unmodifiableMap(deltaMap);
        // trigger callback
        callbackManager.enqueueCallback(new AsyncCallback<LinkStateKnowledgeChangeListener>() {

            @Override
            public void performCallback(LinkStateKnowledgeChangeListener listener) {
                listener.onLinkStateKnowledgeChanged(knowledgeSnapshotCopy);
                listener.onLinkStatesUpdated(deltaMapCopy);
            }
        });
    }
}
