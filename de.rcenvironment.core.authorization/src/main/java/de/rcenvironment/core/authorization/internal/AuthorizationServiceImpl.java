/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupListener;
import de.rcenvironment.core.authorization.api.AuthorizationIdRules;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.configuration.SecureStorageService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsConsumer;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Default {@link AuthorizationService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final int PRE_RELEASE_KEY_DATA_LENGTH = 23;

    private static final String SECURE_STORAGE_NODE_ID = "authorization.accessGroups";

    private static final Pattern GROUP_EXPORT_FORMAT_PARSE_PATTERN = Pattern.compile("^(.+):([^:]+):(\\d:[^:]+)$");

    private final DefaultAuthorizationObjects defaultPermissionSets;

    private SortedMap<AuthorizationAccessGroup, AuthorizationAccessGroupKeyData> accessibleGroupKeyData = new TreeMap<>();

    private final List<AuthorizationAccessGroupListener> groupChangeListeners = new ArrayList<>();

    private CryptographyOperationsProvider cryptographyOperations;

    private Object storageLock = new Object(); // used to synchronize groupDataStorageNode, even if it is null

    private SecureStorageSection groupDataStorage; // may be null if the store could not be fetched

    private volatile boolean initializingFromPersistedData;

    private final Log log = LogFactory.getLog(getClass());

    public AuthorizationServiceImpl() {

        defaultPermissionSets = new DefaultAuthorizationObjects() {

            @Override
            public AuthorizationAccessGroup accessGroupPublicInLocalNetwork() {
                return AuthorizationConstants.GROUP_OBJECT_PUBLIC_IN_LOCAL_NETWORK;
            }

            @Override
            public AuthorizationPermissionSet permissionSetPublicInLocalNetwork() {
                return AuthorizationConstants.PERMISSION_SET_PUBLIC_IN_LOCAL_NETWORK;
            }

            @Override
            public AuthorizationPermissionSet permissionSetLocalOnly() {
                return AuthorizationConstants.PERMISSION_SET_LOCAL_ONLY;
            }

        };

    }

    /**
     * OSGi-DS activation method.
     */
    @Activate
    public void activate() {
        initializingFromPersistedData = true;
        restorePersistedGroups();
        initializingFromPersistedData = false;
    }

    @Override
    public boolean isPersistentStorageAvailable() {
        synchronized (storageLock) {
            return groupDataStorage != null;
        }
    }

    @Override
    public DefaultAuthorizationObjects getDefaultAuthorizationObjects() {
        return defaultPermissionSets;
    }

    @Override
    public CryptographyOperationsProvider getCryptographyOperationsProvider() {
        return cryptographyOperations;
    }

    @Override
    public AuthorizationPermissionSet buildPermissionSet(AuthorizationAccessGroup... groups) {
        return new AuthorizationPermissionSetImpl(groups);
    }

    @Override
    public AuthorizationPermissionSet buildPermissionSet(Collection<AuthorizationAccessGroup> groups) {
        return new AuthorizationPermissionSetImpl(groups);
    }

    @Override
    public List<AuthorizationAccessGroup> listAccessibleGroups(boolean includeHardcodedGroups) {
        synchronized (accessibleGroupKeyData) {
            int size = accessibleGroupKeyData.size();
            if (includeHardcodedGroups) {
                size++;
            }
            final ArrayList<AuthorizationAccessGroup> result = new ArrayList<>(size);
            result.addAll(accessibleGroupKeyData.keySet()); // sorted
            if (includeHardcodedGroups) {
                result.add(AuthorizationConstants.GROUP_OBJECT_PUBLIC_IN_LOCAL_NETWORK);
            }
            return result;
        }
    }

    @Override
    public boolean isValidGroupName(String displayName) {
        // simply delegate
        return !AuthorizationIdRules.validateAuthorizationGroupId(displayName).isPresent();
    }

    @Override
    public AuthorizationAccessGroup createLocalGroup(String displayName) throws OperationFailureException {
        Optional<String> validationError = AuthorizationIdRules.validateAuthorizationGroupId(displayName);
        if (validationError.isPresent()) {
            throw new OperationFailureException(validationError.get());
        }
        final String idPart = IdGenerator.secureRandomHexString(GROUP_ID_SUFFIX_LENGTH); // note: secure id may not be necessary
        final AuthorizationAccessGroupKeyData keyMaterial = generateNewKeyData();
        return createAndRegisterGroup(displayName, idPart, keyMaterial);
    }

    @Override
    public AuthorizationAccessGroup representRemoteGroupId(String fullGroupId) throws OperationFailureException {
        fullGroupId = fullGroupId.trim();
        Optional<String> validationError = AuthorizationIdRules.validateAuthorizationGroupFullId(fullGroupId);
        if (validationError.isPresent()) {
            throw new OperationFailureException(validationError.get());
        }
        String[] parts = fullGroupId.split(":");
        final String groupName = parts[0];
        final String idPart = parts[1];
        return new AuthorizationAccessGroupImpl(groupName, idPart, fullGroupId, defaultGroupDisplayName(groupName, idPart));
    }

    @Override
    public AuthorizationAccessGroup findLocalGroupById(String searchString) throws OperationFailureException {
        // local group lists are typically short, so just iterate over them instead of maintaining maps
        synchronized (accessibleGroupKeyData) {
            // look for full id match first
            for (AuthorizationAccessGroup group : accessibleGroupKeyData.keySet()) {
                if (group.getFullId().equals(searchString)) {
                    return group;
                }
            }
            AuthorizationAccessGroup match = null;
            // look for unique match for user-given id or generated suffix
            for (AuthorizationAccessGroup group : accessibleGroupKeyData.keySet()) {
                if (group.getName().equals(searchString) || group.getIdPart().equals(searchString)) {
                    if (match != null) {
                        throw new OperationFailureException(
                            StringUtils.format(" The group id %s is ambiguous - it is matched by local groups %s and %s",
                                searchString, match.getFullId(), group.getFullId()));
                    }
                    match = group;
                }
            }
            return match;
        }
    }

    @Override
    public AuthorizationAccessGroupKeyData getKeyDataForGroup(AuthorizationAccessGroup group) {
        synchronized (accessibleGroupKeyData) {
            return accessibleGroupKeyData.get(group);
        }
    }

    @Override
    public void deleteLocalGroupData(AuthorizationAccessGroup group) {
        List<AuthorizationAccessGroup> immutableGroupList;
        synchronized (accessibleGroupKeyData) {
            accessibleGroupKeyData.remove(group);
            immutableGroupList = createImmutableCopyOfGroupList();
        }

        if (!initializingFromPersistedData) {
            deleteGroupFromPersistence(group);
        } else {
            log.warn("Unexpected state - group deleted during initialization?");
        }

        notifyChangeListeners(immutableGroupList);
    }

    @Override
    public boolean isGroupAccessible(AuthorizationAccessGroup group) {
        synchronized (accessibleGroupKeyData) {
            return accessibleGroupKeyData.containsKey(group);
        }
    }

    @Override
    public String exportToString(AuthorizationAccessGroup group) {
        synchronized (accessibleGroupKeyData) {
            if (!isGroupAccessible(group)) {
                return null;
            }
            final String encodedKeyData = accessibleGroupKeyData.get(group).getEncodedStringForm();
            return group.getFullId() + ID_SEPARATOR + encodedKeyData;
        }
    }

    @Override
    public AuthorizationAccessGroup importFromString(String externalFormat) throws OperationFailureException {
        // TODO (p1) 9.0.0: check for collision with existing groups
        final Matcher matcher = GROUP_EXPORT_FORMAT_PARSE_PATTERN.matcher(externalFormat);
        if (!matcher.matches()) {
            throw new OperationFailureException("Invalid exported group data: " + externalFormat);
        }
        final String keyString = matcher.group(3);
        SymmetricKey groupKey;
        try {
            groupKey = cryptographyOperations.decodeSymmetricKey(keyString);
        } catch (RuntimeException e) {
            throw new OperationFailureException("Invalid exported access key: " + e.toString());
        }
        return createAndRegisterGroup(matcher.group(1), matcher.group(2), new AuthorizationAccessGroupKeyData(groupKey));
    }

    @Reference
    protected void bindSecureStorageService(SecureStorageService secureStorageService) {
        try {
            synchronized (storageLock) {
                groupDataStorage = secureStorageService.getSecureStorageSection(SECURE_STORAGE_NODE_ID);
            }
        } catch (IOException e) {
            log.error("Failed to acquire Secure Storage, authorization information will not be persisted: " + e.toString());
            groupDataStorage = null;
        }
    }

    @Reference
    protected void bindObjectBindingsService(ObjectBindingsService objectBindingsService) {
        objectBindingsService.setConsumer(AuthorizationAccessGroupListener.class,
            new ObjectBindingsConsumer<AuthorizationAccessGroupListener>() {

                @Override
                public void addInstance(AuthorizationAccessGroupListener instance) {
                    addAuthorizationAccessGroupListener(instance);
                }

                @Override
                public void removeInstance(AuthorizationAccessGroupListener instance) {
                    removeAuthorizationAccessGroupListener(instance);
                }
            });
    }

    /**
     * OSGi-DS injection method; public for access by test utilities.
     * 
     * @param newInstance the instance to set
     */
    @Reference
    public void bindCryptographyOperationsProvider(CryptographyOperationsProvider newInstance) {
        this.cryptographyOperations = newInstance;
    }

    @Override
    public boolean isPublicAccessGroup(AuthorizationAccessGroup accessGroup) {
        return accessGroup.getFullId().equals(AuthorizationConstants.GROUP_ID_PUBLIC_IN_LOCAL_NETWORK);
    }

    private boolean persistGroupData(final AuthorizationAccessGroupImpl group, final AuthorizationAccessGroupKeyData keyData) {
        boolean success = true;
        synchronized (storageLock) {
            if (groupDataStorage != null) {
                try {
                    groupDataStorage.store(group.getFullId(), keyData.getEncodedStringForm());
                } catch (OperationFailureException e) {
                    log.error("Error while saving new key data for access group " + group.getFullId() + ": " + e.toString());
                    success = false;
                }
            } else {
                log.warn("Authorization storage is disabled - the new data for group " + group.getFullId()
                    + " will not be saved! Export it and save the exported data manually to prevent losing group access");
            }
        }
        return success;
    }

    private void restorePersistedGroups() {
        synchronized (storageLock) {
            if (groupDataStorage != null) {
                final String[] ids = groupDataStorage.listKeys();
                if (ids != null && ids.length != 0) {
                    log.debug("Restoring " + ids.length + " persisted access group(s) with access keys");
                    List<AuthorizationAccessGroup> immutableGroupList;
                    synchronized (accessibleGroupKeyData) {
                        for (String id : ids) {
                            restorePersistedGroup(id);
                        }
                        immutableGroupList = createImmutableCopyOfGroupList();
                    }
                    notifyChangeListeners(immutableGroupList);
                }
            }
        }
    }

    private void restorePersistedGroup(String id) {
        try {
            String[] idParts = id.split(ID_SEPARATOR);
            if (idParts.length != 2) {
                log.error("Ignoring invalid access group id in secure storage: " + id);
                return;
            }
            final String keyData = groupDataStorage.read(id, null);
            if (keyData == null || keyData.isEmpty()) {
                log.error("Ignoring invalid stored key data for access group " + id);
                return;
            }
            if (keyData.length() == PRE_RELEASE_KEY_DATA_LENGTH) {
                log.warn("Ignoring pre-release 128 bit key for access group " + id
                    + "; delete the related line in the secure storage file "
                    + "<profile>/internal/settings.secure.dat to get rid of this message");
                return;
            }
            SymmetricKey key = cryptographyOperations.decodeSymmetricKey(keyData);
            createAndRegisterGroup(idParts[0], idParts[1], new AuthorizationAccessGroupKeyData(key));
            log.debug("Restored persisted access group " + id);
        } catch (OperationFailureException e) {
            log.error("Error while restoring access group " + id + ": " + e.toString());
            // continue with other entries; no harm in trying, even if all entries are invalid
        }
    }

    private void deleteGroupFromPersistence(AuthorizationAccessGroup group) {
        synchronized (storageLock) {
            if (groupDataStorage != null) {
                try {
                    groupDataStorage.delete(group.getFullId());
                } catch (OperationFailureException e) {
                    log.error("Failed to delete the local access group " + group.getFullId() + " from authorization storage - "
                        + " it may return after a restart if it was present before startup");
                }
            } else {
                log.warn("Authorization storage is disabled - group " + group.getFullId()
                    + " was not deleted from storage and may return after restart if it was present before startup");
            }
        }
    }

    private AuthorizationAccessGroupKeyData generateNewKeyData() throws OperationFailureException {
        return new AuthorizationAccessGroupKeyData(cryptographyOperations.generateSymmetricKey());
    }

    private AuthorizationAccessGroupImpl createAndRegisterGroup(String name, final String idPart,
        final AuthorizationAccessGroupKeyData keyData) {
        final AuthorizationAccessGroupImpl group =
            new AuthorizationAccessGroupImpl(name, idPart, name + ID_SEPARATOR + idPart,
                defaultGroupDisplayName(name, idPart));

        if (!initializingFromPersistedData) {
            // attempt to save
            if (!persistGroupData(group, keyData)) {
                // if saving failed, do not apply this group locally to avoid inconsistencies
                return null;
            }
        }

        // apply
        final List<AuthorizationAccessGroup> immutableGroupList;
        synchronized (accessibleGroupKeyData) {
            accessibleGroupKeyData.put(group, keyData);
            immutableGroupList = createImmutableCopyOfGroupList();
        }

        notifyChangeListeners(immutableGroupList);
        return group;
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, // forced line break
        unbind = "removeAuthorizationAccessGroupListener")
    public void addAuthorizationAccessGroupListener(AuthorizationAccessGroupListener listener) {
        synchronized (groupChangeListeners) {
            groupChangeListeners.add(listener);
        }
    }

    @Override
    public void removeAuthorizationAccessGroupListener(AuthorizationAccessGroupListener listener) {
        synchronized (groupChangeListeners) {
            if (!groupChangeListeners.remove(listener)) {
                LogFactory.getLog(getClass()).error("Removed a listener that was not previously registered: " + listener.getClass());
            }
        }
    }

    private List<AuthorizationAccessGroup> createImmutableCopyOfGroupList() {
        return Collections.unmodifiableList(listAccessibleGroups(true));
    }

    private String defaultGroupDisplayName(String name, String idPart) {
        if (idPart != null) {
            return StringUtils.format("%s [%s]", name, idPart);
        } else {
            // should usually not be used in this service, but left in for safe fallback
            return name;
        }
    }

    private void notifyChangeListeners(final List<AuthorizationAccessGroup> immutableGroupList) {
        if (initializingFromPersistedData) {
            return;
        }
        // simply using blocking callbacks as these changes are always triggered locally
        synchronized (groupChangeListeners) {
            log.debug("Notifying " + groupChangeListeners.size()
                + " listener(s) of an authorization group change; new group list entry count: " + immutableGroupList.size());
            for (AuthorizationAccessGroupListener listener : groupChangeListeners) {
                listener.onAvailableAuthorizationAccessGroupsChanged(immutableGroupList);
            }
        }
    }
}
