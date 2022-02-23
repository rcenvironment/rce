/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * A service that manages access groups with optional associated key material. Currently, group objects are simple wrappers around group ids
 * and display names, while all associated key material is exclusively stored within the service's implementation.
 *
 * @author Robert Mischke
 */
public interface AuthorizationService {

    /**
     * The character used to separate the human-selected group name, the generated id part, and optionally (when exporting) the key material
     * in the string representation of a group.
     */
    String ID_SEPARATOR = ":";

    /**
     * The length of the "id suffix" of generated {@link AuthorizationAccessGroup}s.
     */
    int GROUP_ID_SUFFIX_LENGTH = 16;

    /**
     * The subdirectory of the profile "imports" directory where group key files, if any, are imported from at startup. Successfully
     * imported files are deleted afterwards.
     */
    String GROUP_KEY_FILE_IMPORT_SUBDIRECTORY = "auth-group-keys";

    /**
     * The prefix in an import file that causes the following key (of at least 2 key segments) to be deleted instead of imported.
     */
    String GROUP_KEY_FILE_IMPORT_DELETION_PREFIX = "delete ";

    /**
     * @return provides access to the {@link DefaultAuthorizationObjects}
     */
    DefaultAuthorizationObjects getDefaultAuthorizationObjects();

    /**
     * Convenience access to a {@link CryptographyOperationsProvider} instance.
     * 
     * @return the {@link CryptographyOperationsProvider} instance used internally
     */
    CryptographyOperationsProvider getCryptographyOperationsProvider();

    /**
     * Combines the given {@link AuthorizationAccessGroup}s into a {@link AuthorizationPermissionSet}.
     * 
     * @param groups the groups to include
     * @return the constructed {@link AuthorizationPermissionSet}
     */
    AuthorizationPermissionSet buildPermissionSet(AuthorizationAccessGroup... groups);

    /**
     * Combines the given {@link AuthorizationAccessGroup}s into a {@link AuthorizationPermissionSet}.
     * 
     * @param groups the groups to include
     * @return the constructed {@link AuthorizationPermissionSet}
     */
    AuthorizationPermissionSet buildPermissionSet(Collection<AuthorizationAccessGroup> groups);

    /**
     * @param includeHardcodedGroups whether the result should also include special hard-coded access groups (which currently is exactly the
     *        "public in local networks" group)
     * @return the currently known list of access groups that can be assigned to local resources as part of a
     *         {@link AuthorizationPermissionSet}; currently, the list will be sorted by display name, and any special groups will be put
     *         last
     */
    List<AuthorizationAccessGroup> listAccessibleGroups(boolean includeHardcodedGroups);

    /**
     * Tests whether the given group name/id would be valid for calling {@link #createLocalGroup()}.
     * 
     * @param displayName the name/id to check
     * @return true if the same name could be used for calling {@link #createLocalGroup()}
     */
    boolean isValidGroupName(String displayName);

    /**
     * Creates a new local group with the given display name, a random id part, and randomly-generated key material. The display name and id
     * are represented in the returned {@link AuthorizationAccessGroup} object. The key material is stored in the service's internal state
     * and also in the persistent storage.
     * 
     * @param displayName the display name of the new group; note that character set restrictions apply
     * @return the representation of the new group
     * @throws OperationFailureException on failure to create the new group, typically because the given id is invalid
     */
    AuthorizationAccessGroup createLocalGroup(String displayName) throws OperationFailureException;

    /**
     * Retrieves the group object of a local authorized group by its short or full id. If a short id is given, it must be unique; otherwise
     * an {@link OperationFailureException} is thrown. If no matching group is found, null is returned.
     * 
     * @param groupId the short or full id to match
     * @return the group object, or null if no matching group was found
     * @throws OperationFailureException if a short id was given, and it is ambiguous
     */
    AuthorizationAccessGroup findLocalGroupById(String groupId) throws OperationFailureException;

    /**
     * Represents a remote group id as an {@link AuthorizationAccessGroup} object. Known existing group objects may be reused.
     * 
     * @param fullGroupId the full group id, typically received as part of a network operation
     * @return the group object
     * @throws OperationFailureException if representing the group failed, usually due to an invalid format
     */
    AuthorizationAccessGroup representRemoteGroupId(String fullGroupId) throws OperationFailureException;

    /**
     * Batch equivalent of {@link #representRemoteGroupId(String)}, converting a {@link Collection} of group ids to a {@link Set} of
     * {@link AuthorizationAccessGroup}s. Duplicate (or otherwise equivalent) ids are merged; therefore, the returned set may be smaller
     * than the input collection.
     * 
     * @param fullGroupIds the group ids to convert
     * @return the generated set of {@link AuthorizationAccessGroup}s
     * @throws OperationFailureException if representing a group failed, usually due to an invalid format
     */
    Set<AuthorizationAccessGroup> representRemoteGroupIds(Collection<String> fullGroupIds) throws OperationFailureException;

    /**
     * Accepts a {@link Collection} of {@link AuthorizationAccessGroup}s and returns a {@link Set} containing those elements that are also
     * locally accessible, ie have local key data available and are therefore usable. May return identical group objects, or replace them
     * with equivalent ones; calling code should make no assumptions beyond equality.
     * 
     * @param inputGroups the groups to process
     * @return the filtered Collection as a {@link Set}
     */
    Set<AuthorizationAccessGroup> intersectWithAccessibleGroups(Set<AuthorizationAccessGroup> inputGroups);

    /**
     * Returns the cryptographic key data for the given group, if any is available.
     * 
     * @param group the group to fetch key data for
     * @return the locally generated or imported key data, or null of no key data is available (ie the group is not accessible)
     */
    AuthorizationAccessGroupKeyData getKeyDataForGroup(AuthorizationAccessGroup group);

    /**
     * Deletes any local key material for the given group, making it inaccessible. The key data is also deleted from the persistent storage.
     * 
     * @param group the group to delete all key material for
     */
    void deleteLocalGroupData(AuthorizationAccessGroup group);

    /**
     * @param group the group to test
     * @return true if and only if the local node has access (ie, key material) for the given group; otherwise, it is a remote group that
     *         can be represented, but not used, and its associated remote resources can typically not be accessed
     */
    boolean isGroupAccessible(AuthorizationAccessGroup group);

    /**
     * Represents the given group as an external string, which can then be passed to other users and imported by them to become a member of
     * the same group. (Note that this simple "shared secret" approach should be replaced by a more sophisticated and secure invitation
     * process in the future.)
     * 
     * @param group the group to export
     * @return the export/invitation string to present to the user
     */
    String exportToString(AuthorizationAccessGroup group);

    /**
     * Parses a string generated by {@link #exportToString()} and attempts to recreate the local group, including its attached key material.
     * On success, the key material is stored in the service's internal state and also in the persistent storage.
     * 
     * @param externalFormat the received string
     * @return the representation of the new local and accessible group
     * @throws OperationFailureException if importing the group failed, usually due to an invalid format
     */
    AuthorizationAccessGroup importFromString(String externalFormat) throws OperationFailureException;

    /**
     * Registers a new {@link AuthorizationAccessGroupListener}.
     * 
     * @param listener the listener
     */
    void addAuthorizationAccessGroupListener(AuthorizationAccessGroupListener listener);

    /**
     * Unregisters a previously-registered {@link AuthorizationAccessGroupListener}.
     * 
     * @param listener the listener
     */
    void removeAuthorizationAccessGroupListener(AuthorizationAccessGroupListener listener);

    /**
     * @return whether persistent group storage was properly initialized; used to test whether absence of local groups is due to an
     *         inaccessible storage, in which case permissions for that group should not be deleted (yet)
     */
    boolean isPersistentStorageAvailable();

    /**
     * @param accessGroup the group to test
     * @return true if the given group represents "public in local network" access; see
     *         {@link DefaultAuthorizationObjects#accessGroupPublicInLocalNetwork())
     */
    boolean isPublicAccessGroup(AuthorizationAccessGroup accessGroup);

}
