/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.impl;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.api.ComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.authorization.api.RemotableComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextHolder;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Default implementation of {@link ComponentExecutionAuthorizationService} and {@link RemotableComponentExecutionAuthorizationService}.
 *
 * @author Robert Mischke
 */
@Component
public class ComponentExecutionAuthorizationServiceImpl
    implements ComponentExecutionAuthorizationService, RemotableComponentExecutionAuthorizationService {

    /**
     * Controls the time until unused tokens become invalid, and can also be discarded. This timeout needs to be longer than the maximum
     * time from workflow controller initiation until all component controller instantiations have occurred. Its duration should not affect
     * security; it only serves to control garbage collection on aborted/incomplete workflow starts. The only downside of setting it too
     * high is that a permission that was revoked after acquiring the token has no effect, effectively delaying the new permission settings
     * becoming active by up to the duration of the timeout. -- misc_ro
     */
    // Note: set fairly high to avoid timeouts when large workflows use rate limiting for remote component initialization
    private static final long TOKEN_LIFETIME_MSEC = TimeUnit.MINUTES.toMillis(5);

    private static final long GC_INTERVAL_MSEC = 15000;

    private static final int TOKEN_HEX_STRING_LENGTH = 32; // 128 bit should suffice for now

    // new tokens are added sequentially, so garbage collection only needs to check the first element; the expiration time for each token is
    // fetched from the map
    private final Deque<String> gcQueue = new LinkedList<>();

    // active tokens and their timeouts
    private final Map<String, Long> activeTokens = new HashMap<>();

    private AuthorizationService authorizationService;

    private LocalComponentRegistrationService localComponentRegistrationService;

    private CryptographyOperationsProvider cryptographyOperationsProvider;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Standard OSGi-DS activation method.
     */
    @Activate
    public void activate() {
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay("Component Authorization Token Garbage Collection",
            this::runGarbageCollection, GC_INTERVAL_MSEC);
    }

    @Override
    @AllowRemoteAccess
    public synchronized String requestExecutionTokenForPublicComponent(String internalComponentId, String componentVersion)
        throws OperationFailureException {
        final LogicalNodeSessionId destinationNodeId = getLogicalNodeDestinationOfServiceCall();

        ComponentAuthorizationSelector componentSelector = localComponentRegistrationService.getComponentSelector(internalComponentId);
        // TODO ugly quick fix: if the target node is a non-default LogicalNodeId, assume a proxy component and adjust the selector
        if (!destinationNodeId.getLogicalNodePart().equals(CommonIdBase.DEFAULT_LOGICAL_NODE_PART)) {
            componentSelector = patchComponentSelectorWithNonDefaultLogicalNodeId(componentSelector, componentVersion, destinationNodeId);
        }
        AuthorizationPermissionSet permissionSet = localComponentRegistrationService.getComponentPermissionSet(componentSelector, true);

        if (permissionSet.isPublic()) {
            log.debug("Generating access token for public component \"" + internalComponentId + "\" on logical node :"
                + destinationNodeId.getLogicalNodePart());
            return generateAndRegisterToken(internalComponentId + ":public");
        } else {
            log.debug(StringUtils.format(
                "Rejecting request for a public execution permission token for component \"%s\" on logical node :%s "
                    + "as it is not currently public; actual permissions: %s",
                internalComponentId, destinationNodeId.getLogicalNodePart(), permissionSet.getSignature()));
            throw new OperationFailureException(StringUtils.format(
                "Public execution permission was requested for component \"%s\", "
                    + "but it is not currently public; maybe its permissions have been changed very recently",
                internalComponentId));
        }
    }

    @Override
    @AllowRemoteAccess
    public String requestEncryptedExecutionTokenViaGroupMembership(String internalComponentId, String componentVersion, String groupId)
        throws OperationFailureException {
        final LogicalNodeSessionId destinationNodeId = getLogicalNodeDestinationOfServiceCall();

        ComponentAuthorizationSelector componentSelector = localComponentRegistrationService.getComponentSelector(internalComponentId);
        // TODO ugly quick fix: if the target node is a non-default LogicalNodeId, assume a proxy component and adjust the selector
        if (!destinationNodeId.getLogicalNodePart().equals(CommonIdBase.DEFAULT_LOGICAL_NODE_PART)) {
            componentSelector = patchComponentSelectorWithNonDefaultLogicalNodeId(componentSelector, componentVersion, destinationNodeId);
        }
        AuthorizationPermissionSet permissionSet = localComponentRegistrationService.getComponentPermissionSet(componentSelector, true);
        AuthorizationAccessGroup groupReference = authorizationService.representRemoteGroupId(groupId);

        if (permissionSet.includesAccessGroup(groupReference)) {
            log.debug(StringUtils.format(
                "Generating access token for component \"%s\" on logical node :%s, accessible via group membership in \"%s\"",
                internalComponentId, destinationNodeId.getLogicalNodePart(), groupId));
            final String accessToken = generateAndRegisterToken(internalComponentId + ":group");
            final AuthorizationAccessGroupKeyData groupKeyData = authorizationService.getKeyDataForGroup(groupReference);
            final SymmetricKey secretGroupKey = groupKeyData.getSymmetricKey();
            return cryptographyOperationsProvider.encryptAndEncodeString(secretGroupKey, accessToken);
        } else {
            log.debug(StringUtils.format(
                "Rejecting request for a group-based execution permission token for component \"%s\" on logical node :%s by group "
                    + "as it is not currently available for that group; actual permissions: %s",
                internalComponentId, destinationNodeId.getLogicalNodePart(), groupId, permissionSet.getSignature()));
            throw new OperationFailureException(StringUtils.format(
                "Group-based execution permission was requested for component \"%s\" on logical node :%s, "
                    + "but it is not currently available for group \"%s\"; maybe its permissions have been changed very recently",
                internalComponentId, destinationNodeId.getLogicalNodePart(), groupId));
        }
    }

    @Override
    public String createAndRegisterExecutionTokenForLocalComponent(String internalComponentId) {
        log.debug("Generating access token for local component \"" + internalComponentId + "\"");
        return generateAndRegisterToken(internalComponentId + ":local");
    }

    @Override
    public synchronized boolean verifyAndUnregisterExecutionToken(String token) {
        Long expirationTime = activeTokens.remove(token);
        if (expirationTime == null) {
            log.debug("Rejecting component execution authorization token " + token
                + " as it is either invalid, was already used, or was already discarded because it had expired");
            return false;
        }
        if (System.currentTimeMillis() <= expirationTime) {
            // TODO 9.0.0: remove this log output for release? should not be problematic as the token is invalid once this was logged
            log.debug("Accepting component execution authorization token " + token);
            return true;
        } else {
            // not garbage collected yet, but still expired; log this to detect whether token timeouts must be adapted
            log.debug(
                "Rejecting component execution authorization token " + token + ", which is valid and was not used yet, but has expired");
            return false;
        }
    }

    @Reference
    protected void bindLocalComponentRegistrationService(LocalComponentRegistrationService newInstance) {
        this.localComponentRegistrationService = newInstance;
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
    }

    @Reference
    protected void bindCryptographyOperationsProvider(CryptographyOperationsProvider newInstance) {
        this.cryptographyOperationsProvider = newInstance;
    }

    private String generateAndRegisterToken(String id) {
        final String token = id + ":" + IdGenerator.secureRandomHexString(TOKEN_HEX_STRING_LENGTH);
        final long expirationTime = System.currentTimeMillis() + TOKEN_LIFETIME_MSEC;
        activeTokens.put(token, expirationTime);
        gcQueue.addLast(token);
        return token;
    }

    private synchronized void runGarbageCollection() {
        int removedUnused = 0;
        while (!gcQueue.isEmpty()) {
            String firstToken = gcQueue.peekFirst();
            Long expirationTime = activeTokens.get(firstToken);
            if (expirationTime == null) {
                // oldest token was used in the meantime -> simply remove it from GC queue
                gcQueue.removeFirst();
            } else {
                if (System.currentTimeMillis() <= expirationTime) {
                    // oldest token is still valid -> finish GC run
                    break;
                } else {
                    // oldest token has expired -> remove from GC queue and active set, the repeat loop
                    gcQueue.removeFirst();
                    activeTokens.remove(firstToken);
                    // TODO 9.0.0: remove this log output for release?
                    log.debug("Removed unused component execution token " + firstToken + " as it has expired without being used");
                    removedUnused++;
                }
            }
        }
        if (removedUnused > 0) {
            log.debug("Removed " + removedUnused + " unused component execution token(s) that have timed out");
        }
    }

    private LogicalNodeSessionId getLogicalNodeDestinationOfServiceCall() {
        final ServiceCallContext serviceCallContext = ThreadContextHolder.getCurrentContextAspect(ServiceCallContext.class);
        if (serviceCallContext == null) {
            throw new IllegalStateException("No service call context available");
        }
        final LogicalNodeSessionId receivingNodeId = serviceCallContext.getReceivingNode();
        return receivingNodeId;
    }

    private ComponentAuthorizationSelector patchComponentSelectorWithNonDefaultLogicalNodeId(
        ComponentAuthorizationSelector componentSelector, String componentVersion, final LogicalNodeSessionId destinationNodeId) {
        componentSelector = new ComponentAuthorizationSelectorImpl(
            componentSelector.getId() + "/" + componentVersion + "/" + destinationNodeId.getLogicalNodePart());
        return componentSelector;
    }

}
