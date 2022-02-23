/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.documentation.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.integration.documentation.RemoteToolIntegrationDocumentationService;
import de.rcenvironment.core.component.integration.documentation.ToolDocumentationProvider;
import de.rcenvironment.core.component.integration.documentation.ToolIntegrationDocumentationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessClientService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.FileCompressionFormat;
import de.rcenvironment.core.utils.common.FileCompressionService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link ToolIntegrationDocumentationService}.
 * 
 * @author Sascha Zur
 * @author Thorsten Sommer (integration of {@link FileCompressionService})
 * @author Brigitte Boden
 * 
 */
@Component(immediate = true)
public class ToolIntegrationDocumentationServiceImpl
    implements ToolIntegrationDocumentationService, RemoteToolIntegrationDocumentationService {

    protected static DistributedComponentKnowledgeService componentKnowledgeService;

    protected static CommunicationService communicationService;

    private static final String DE_RCENVIRONMENT_REMOTEACCESS = "de.rcenvironment.remoteaccess";

    private static final String METADATA_FILE_NAME = ".metadata";

    private static final String KEY_HASH = "hash";

    private static final String KEY_LAST_USED = "lastUsed";

    private static final String KEY_DOCUMENTATION_DIR_NAME = "documentationDir";

    private static final String CACHE_NAME = "toolDocCache";

    private static final Log LOGGER = LogFactory.getLog(ToolIntegrationDocumentationServiceImpl.class);

    private Map<String, Map<String, Map<String, String>>> toolDocumentationCache;

    private Map<String, ToolDocumentationProvider> toolDocProviderMap = new HashMap<String, ToolDocumentationProvider>();

    private ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private ConfigurationService configService;

    private SshRemoteAccessClientService sshClientService;

    @Reference
    // private UplinkToolAccessClientService uplinkClientService;

    private ToolIntegrationService toolIntegrationService;

    private final long time90Days = 7776000000L;

    @Override
    public Map<String, String> getComponentDocumentationList(String identifier) {
        Map<String, String> docs = new HashMap<>();
        for (DistributedComponentEntry entry : componentKnowledgeService.getCurrentSnapshot().getAllInstallations()) {
            ComponentInstallation ci = entry.getComponentInstallation();
            if (ci.getComponentInterface().getIdentifierAndVersion().equals(identifier)
                && !ci.getComponentInterface().getDocumentationHash().isEmpty()) {
                docs.put(ci.getComponentInterface().getDocumentationHash(), ci.getNodeId());
            }
            // If the tool is SSH-forwarded, also get list of remote documentation nodes
            if (ci.getComponentInterface().getIdentifierAndVersion().equals(identifier)
                && identifier.startsWith(DE_RCENVIRONMENT_REMOTEACCESS)) {
                final RemoteToolIntegrationDocumentationService rtis =
                    communicationService.getRemotableService(RemoteToolIntegrationDocumentationService.class,
                        NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(ci.getNodeId()));

                Map<String, String> componentInstallationsWithDocumentation;
                try {
                    componentInstallationsWithDocumentation = rtis.getComponentDocumentationListForRemoteAccessTools(identifier);
                    for (String hash : componentInstallationsWithDocumentation.keySet()) {
                        docs.put(hash, componentInstallationsWithDocumentation.get(hash));
                    }
                } catch (RemoteOperationException e) {
                    LOGGER.error("Could not retreive remote tool documenation: ", e);
                }
            }
        }
        loadDocumentationCache();
        if (toolDocumentationCache.get(identifier) != null) {
            for (String nodeIdentifier : toolDocumentationCache.get(identifier).keySet()) {
                String hash = toolDocumentationCache.get(identifier).get(nodeIdentifier).get(KEY_HASH);
                if (docs.get(hash) != null && !docs.get(hash).isEmpty()) {
                    docs.put(hash, nodeIdentifier + ToolIntegrationConstants.DOCUMENTATION_CACHED_SUFFIX);
                }
            }
        }

        return docs;
    }

    private File loadDocumentationCache() {
        File cacheDir = new File(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA), CACHE_NAME);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        if (toolDocumentationCache == null) {
            try {
                toolDocumentationCache = new TreeMap<>();
                readToolDocumentationCache();
            } catch (IOException e) {
                LOGGER.error("Could not read documentation cache: ", e);
                toolDocumentationCache = new TreeMap<>();
            }
        }
        return cacheDir;
    }

    @Override
    public File getToolDocumentation(final String identifier, final String nodeId, final String hashValue)
        throws FileNotFoundException, IOException, RemoteOperationException {
        final File cacheDir = loadDocumentationCache();

        final String documentationNodeId;
        if (nodeId.endsWith(ToolIntegrationConstants.DOCUMENTATION_CACHED_SUFFIX)) {
            documentationNodeId = nodeId.substring(0, nodeId.length() - 3);
        } else {
            documentationNodeId = nodeId;
        }

        if (toolDocumentationCache.get(identifier) != null
            && toolDocumentationCache.get(identifier).get(documentationNodeId) != null
            && toolDocumentationCache.get(identifier).get(documentationNodeId).get(KEY_HASH).equals(hashValue)) {

            // documentation in cache
            final File docuDir =
                new File(cacheDir, toolDocumentationCache.get(identifier).get(documentationNodeId).get(KEY_DOCUMENTATION_DIR_NAME));
            toolDocumentationCache.get(identifier).get(documentationNodeId).put(KEY_LAST_USED, String.valueOf(System.currentTimeMillis()));
            return docuDir;
        } else {
            // documentation not in cache, retrieve
            // TODO improve method by passing the id object into it (instead of a node id string)
            final RemoteToolIntegrationDocumentationService rtis =
                communicationService.getRemotableService(RemoteToolIntegrationDocumentationService.class,
                    NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(documentationNodeId));

            final byte[] documentation = rtis.loadToolDocumentation(identifier, nodeId, hashValue);
            if (documentation == null && identifier.startsWith(DE_RCENVIRONMENT_REMOTEACCESS)) {
                // Retrieve over SSH
                sshClientService.downloadToolDocumentation(identifier, documentationNodeId, hashValue);
            }
            if (documentation != null) {
                final File tempDir = findFirstUnusedDirectory(cacheDir);
                tempDir.mkdirs();

                if (!FileCompressionService.expandCompressedDirectoryFromByteArray(documentation, tempDir,
                    FileCompressionFormat.ZIP)) {
                    LOGGER.error("Was not able to retrieve the tool documentation due to an issue with the archive expansion.");
                    throw new IOException("Was not able to retrive the tool documentation due to an issue with the archive expansion.");
                }

                final Map<String, Map<String, String>> nodeIDMap = new HashMap<>();
                final Map<String, String> values = new HashMap<>();
                values.put(KEY_DOCUMENTATION_DIR_NAME, tempDir.getName());
                values.put(KEY_HASH, hashValue);
                values.put(KEY_LAST_USED, String.valueOf(System.currentTimeMillis()));

                nodeIDMap.put(documentationNodeId, values);
                toolDocumentationCache.put(identifier, nodeIDMap);

                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(cacheDir, METADATA_FILE_NAME), toolDocumentationCache);
                return tempDir;
            }
        }
        return null;
    }

    private File findFirstUnusedDirectory(File cacheDir) {
        long i = 0;
        File tmpFile = new File(cacheDir, String.valueOf(i));
        while (tmpFile.exists() && tmpFile.isDirectory()) {
            i++;
            tmpFile = new File(cacheDir, String.valueOf(i));
        }

        return tmpFile;
    }

    private void readToolDocumentationCache() throws JsonParseException, JsonMappingException, IOException {
        File cacheDir = new File(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA), CACHE_NAME);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File metadataFile = new File(cacheDir, METADATA_FILE_NAME);
        if (metadataFile.exists()) {
            mapper.readValue(metadataFile, toolDocumentationCache.getClass());

            // clean up cache
            Set<String> toolIdsToRemove = new HashSet<>();
            for (String toolID : toolDocumentationCache.keySet()) {
                Set<String> nodeIdsToRemove = new HashSet<>();
                for (String nodeId : toolDocumentationCache.get(toolID).keySet()) {
                    checkDocuDirectory(cacheDir, toolID, nodeIdsToRemove, nodeId);
                }
                for (String id : nodeIdsToRemove) {
                    toolDocumentationCache.get(toolID).remove(id);
                }
                if (toolDocumentationCache.get(toolID).isEmpty()) {
                    toolIdsToRemove.add(toolID);
                }
            }

            for (String id : toolIdsToRemove) {
                toolDocumentationCache.remove(id);
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile, toolDocumentationCache);

    }

    private void checkDocuDirectory(File cacheDir, String toolID, Set<String> nodeIdsToRemove, String nodeId) throws IOException {
        if (toolDocumentationCache.get(toolID).get(nodeId).get(KEY_DOCUMENTATION_DIR_NAME) != null) {
            File docDir = new File(cacheDir, toolDocumentationCache.get(toolID).get(nodeId).get(KEY_DOCUMENTATION_DIR_NAME));
            if (!(docDir.exists() && docDir.isDirectory())) {
                nodeIdsToRemove.add(nodeId);
            }
            if (toolDocumentationCache.get(toolID).get(nodeId).get(KEY_LAST_USED) != null) {
                long lastUsed = Long.parseLong(toolDocumentationCache.get(toolID).get(nodeId).get(KEY_LAST_USED));
                if (System.currentTimeMillis() - lastUsed > time90Days) {
                    nodeIdsToRemove.add(nodeId);
                    if (docDir.exists()) {
                        FileUtils.deleteDirectory(docDir);
                    }
                }
            }
        }
    }

    /**
     * Public for testing.
     * 
     * @param incoming service.
     */
    @Reference
    public void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService incoming) {
        componentKnowledgeService = incoming;
    }

    protected void unbindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService incoming) {
        componentKnowledgeService = null;
    }

    /**
     * Public for testing.
     * 
     * @param incoming service.
     */
    @Reference
    public void bindCommunicationService(CommunicationService incoming) {
        communicationService = incoming;

    }

    protected void unbindCommunicationService(CommunicationService incoming) {
        communicationService = null;
    }

    /**
     * Public for testing.
     * 
     * @param incoming service.
     */
    @Reference
    public void bindConfigurationService(ConfigurationService incoming) {
        configService = incoming;

    }

    protected void unbindConfigurationService(ConfigurationService incoming) {
        configService = null;
    }

    /**
     * Public for testing.
     * 
     * @param incoming service.
     */
    @Reference
    public void bindSshRemoteAccessClientService(SshRemoteAccessClientService incoming) {
        sshClientService = incoming;
    }

    /**
     * Public for testing.
     * 
     * @param incoming service.
     */
    @Reference
    public void bindToolIntegrationService(ToolIntegrationService incoming) {
        toolIntegrationService = incoming;
    }

    @Override
    @AllowRemoteAccess
    public byte[] loadToolDocumentation(final String identifier, String nodeId, String hashValue) throws RemoteOperationException {

        LogicalNodeId nodeIdObj;
        try {
            nodeIdObj = NodeIdentifierUtils.parseLogicalNodeIdString(nodeId);
        } catch (IdentifierException e) {
            throw new RemoteOperationException("Failed to parse logical node id: " + e.getMessage());
        }

        byte[] documentation = null;
        if (nodeIdObj.getLogicalNodePart().equals(LogicalNodeId.DEFAULT_LOGICAL_NODE_PART)) {
            documentation = toolIntegrationService.getToolDocumentation(identifier);
        } else if (identifier.startsWith(DE_RCENVIRONMENT_REMOTEACCESS)) {
            File documentationDir = sshClientService.downloadToolDocumentation(identifier, nodeId, hashValue);

            documentation =
                FileCompressionService.compressDirectoryToByteArray(documentationDir, FileCompressionFormat.ZIP, false);
        } else {
            ToolDocumentationProvider toolDocumentationProvider = toolDocProviderMap.get(nodeId);
            if (toolDocumentationProvider != null) {
                try {
                    documentation = toolDocumentationProvider.provideToolDocumentation(identifier, nodeId, hashValue);
                } catch (IOException e) {
                    // FIXME better exception handling; matching the existing behavior for now
                    LOGGER.error("Error retrieving documentation", e);
                    return null;
                }
            }
        }

        return documentation;
    }

    @Override
    @AllowRemoteAccess
    public Map<String, String> getComponentDocumentationListForRemoteAccessTools(String identifier) throws RemoteOperationException {
        Map<String, String> componentInstallationsWithDocumentation =
            sshClientService.getListOfToolsWithDocumentation(identifier);
        return componentInstallationsWithDocumentation;
    }

    @Override
    public void registerToolDocumentationProvider(ToolDocumentationProvider provider, String nodeId) {
        toolDocProviderMap.put(nodeId, provider);
    }
}
