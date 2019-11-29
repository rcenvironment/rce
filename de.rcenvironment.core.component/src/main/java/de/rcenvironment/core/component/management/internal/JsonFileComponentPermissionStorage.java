/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.impl.ComponentAuthorizationSelectorImpl;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Implementation of {@link ComponentPermissionStorage} that persists permission data in a JSON file.
 *
 * @author Robert Mischke
 */
@Component
public class JsonFileComponentPermissionStorage implements ComponentPermissionStorage {

    private static final String PROFILE_RELATIVE_FILENAME = "configuration/components.json";

    private static final String BACKUP_FILE_1_SUFFIX = ".1.bak";

    private static final String BACKUP_FILE_2_SUFFIX = ".2.bak";

    private static final String AUTHORIZATION_JSON_NODE_PATH = "authorization";

    private AuthorizationService authorizationService;

    private AuthorizationPermissionSet permissionSetPublicAccess;

    private String permissionSetPublicAccessSignature;

    private ConfigurationService configurationService;

    private final ObjectMapper jsonMapper;

    private boolean initialized;

    private File storageFile;

    private File backupFile1;

    private File backupFile2;

    private JsonNode currentJsonData;

    private final Log log = LogFactory.getLog(getClass());

    public JsonFileComponentPermissionStorage() {
        jsonMapper = JsonUtils.getDefaultObjectMapper();
    }

    @Activate
    protected void activate() {
        storageFile = new File(configurationService.getProfileDirectory(), PROFILE_RELATIVE_FILENAME);
        try {
            if (!storageFile.exists()) {
                FileUtils.writeStringToFile(storageFile, "{}");
            }
            if (!storageFile.isFile() || !storageFile.canWrite()) {
                log.error("Failed to initialize component authorization storage " + storageFile.getAbsolutePath()
                    + ": the file could not be created or is not writable");
            }
            currentJsonData = jsonMapper.readTree(storageFile);

            backupFile1 = new File(storageFile.getParentFile(), storageFile.getName() + BACKUP_FILE_1_SUFFIX);
            backupFile2 = new File(storageFile.getParentFile(), storageFile.getName() + BACKUP_FILE_2_SUFFIX);

            initialized = true;
        } catch (IOException e) {
            log.error("Failed to initialize component authorization storage " + storageFile.getAbsolutePath() + ": " + e.toString());
        }
    }

    @Reference
    protected synchronized void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

    @Reference
    protected synchronized void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
        this.permissionSetPublicAccess = authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
        this.permissionSetPublicAccessSignature = this.permissionSetPublicAccess.getSignature();
    }

    @Override
    public synchronized void persistAssignment(final ComponentAuthorizationSelector selector, final AuthorizationPermissionSet permissions)
        throws OperationFailureException {
        if (!initialized) {
            // do not automatically fail the operation to support the case of read-only configuration
            log.warn("Authorization storage is disabled - the new data for component-group assignment for selector " + selector.getId()
                + " will not be saved, and previous settings may return after restarting!");
            return;
        }

        // shift/rotate backup files; after this, the storage file has been moved away, and 1-2 backup files exist instead
        if (backupFile2.exists() && !backupFile2.delete()) {
            throw new OperationFailureException(
                "Failed to delete backup file " + backupFile2.getAbsolutePath());
        }
        if (backupFile1.exists() && !backupFile1.renameTo(backupFile2)) {
            throw new OperationFailureException(
                "Failed to move backup file " + backupFile1.getAbsolutePath() + " to " + backupFile2.getAbsolutePath());
        }
        if (!storageFile.renameTo(backupFile1)) {
            throw new OperationFailureException(
                "Failed to move " + storageFile.getAbsolutePath() + " to backup location " + backupFile1.getAbsolutePath());
        }

        final ObjectNode authNode = getAuthorizationDataJsonNode();

        // NOTE: this is disabled code for the approach of storing group ids as an array, instead of a comma-separated string
        // final ArrayNode arrayNode = ((ObjectNode) authNode).putArray(selector.getId());
        // for (AuthorizationAccessGroup group : permissions.getAccessGroups()) {
        // arrayNode.add(group.getFullId());
        // }
        if (!permissions.isLocalOnly()) {
            authNode.put(selector.getId(), permissions.getSignature());
        } else {
            // local-only -> delete entry
            authNode.remove(selector.getId());
        }
        try {
            FileUtils.writeStringToFile(storageFile, jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentJsonData));
        } catch (IOException e) {
            throw new OperationFailureException("Failed to save updated component-group assignment data: " + e.toString());
        }
    }

    @Override
    public synchronized Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> restorePersistedAssignments() {
        Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> resultMap = new HashMap<>();
        if (!initialized) {
            log.warn("Error loading access group data from secure storage - "
                + "not initializing component permissions to avoid erronous deletion");
            return resultMap;
        }

        ObjectNode authNode;
        try {
            authNode = getAuthorizationDataJsonNode();
        } catch (OperationFailureException e) {
            log.warn("Error loading persisted component permissions from secure storage "
                + "(disabling further write operations to prevent accidental deletion): " + e.toString());
            initialized = false;
            return resultMap; // may return a partial result, but this is irrelevant at this point
        }

        Iterator<Entry<String, JsonNode>> i = authNode.fields();
        int count = 0;
        while (i.hasNext()) {
            Entry<String, JsonNode> entry = i.next();
            String id = entry.getKey();
            String permSetSignature = entry.getValue().asText();
            restorePersistedAssignment(id, permSetSignature, resultMap);
            count++;
        }
        log.debug("Restored " + count + " persisted component-group assignment(s)");

        return resultMap;
    }

    private void restorePersistedAssignment(String id, String permSetSignature,
        Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> resultMap) {
        if (permSetSignature == null || permSetSignature.length() == 0) {
            log.error("Ignoring invalid (empty) stored permission data for component selector " + id);
            return;
        }

        final AuthorizationPermissionSet restoredPermSet;
        if (permissionSetPublicAccessSignature.equals(permSetSignature)) {
            // component published with public access
            restoredPermSet = permissionSetPublicAccess;
        } else {
            List<AuthorizationAccessGroup> groups = new ArrayList<>();
            for (String part : permSetSignature.split(",")) {
                try {
                    groups.add(authorizationService.representRemoteGroupId(part.trim()));
                } catch (OperationFailureException e) {
                    log.error("Ignoring invalid stored group id " + part + " for component " + id + "; reason: " + e.getMessage());
                }
            }
            restoredPermSet = authorizationService.buildPermissionSet(groups);
        }
        if (!restoredPermSet.isLocalOnly()) {
            resultMap.put(new ComponentAuthorizationSelectorImpl(id), restoredPermSet);
            log.debug("Restored permission set " + restoredPermSet.getSignature() + " for component selector " + id);
        }
    }

    private ObjectNode getAuthorizationDataJsonNode() throws OperationFailureException {
        JsonNode authNode = currentJsonData.get(AUTHORIZATION_JSON_NODE_PATH);
        if (authNode == null || authNode.isNull()) {
            authNode = jsonMapper.createObjectNode();
            ((ObjectNode) currentJsonData).set(AUTHORIZATION_JSON_NODE_PATH, authNode);
        }
        if (!(authNode instanceof ObjectNode)) {
            throw new OperationFailureException("Unexpected data node type at path: " + AUTHORIZATION_JSON_NODE_PATH);
        }
        return (ObjectNode) authNode;
    }
}
