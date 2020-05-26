/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.impl.ComponentImageManagerImpl.IconSize;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;

/**
 * 
 * Implementation of {@link ComponentImageCacheService}.
 * 
 * @author Dominik Schneider
 *
 */
@Component(scope = ServiceScope.SINGLETON)
public class ComponentImageCacheImpl implements ComponentImageCacheService, DistributedComponentKnowledgeListener {

    private static final String CACHE_DIRECTORY = "componentIconCache";

    private static final String INDEX = "queue";

    private static final String MAPPING = "mapping";

    private static final int CACHE_SIZE = 512;

    // This is the md5 hash of three null images. All component interfaces without image data have this hash.
    private static final String NULL_HASH = "d41d8cd98f00b204e9800998ecf8427e";

    private Map<String, String> idIconMap; // key: componentId ; value: iconHash

    private List<String> hashesToRemove;

    private LinkedList<String> iconHashes;

    private ConfigurationService configService;

    private DistributedComponentKnowledgeService knowledgService;

    private Path indexFile;

    private Path mappingFile;

    public ComponentImageCacheImpl() {
        // setting up list
        iconHashes = new LinkedList<>(); // LinkedList is used to guarantee a fixed order
        hashesToRemove = new ArrayList<>();
        idIconMap = new HashMap<>();

    }

    @Activate
    private void activate() {
        // load cache from disk, if no cache exists, one will be created
        readCache();
        // adding all unknown components to the cache
        addUnknownComponentsToCache(knowledgService.getCurrentSnapshot());
    }

    /**
     * Writes icon raw data into the RAM representation of the cache. The data is not synchronized to the file system to allow usage of this
     * method in loops without writing to the file system after each iteration.
     * 
     * @param iconHash Hash of the image raw data
     * @param componentId ID of the component to which the icon data belongs
     * @param icon16 icon raw data for an image in size 16x16
     * @param icon24 icon raw data for an image in size 24x24
     * @param icon32 icon raw data for an image in size 32x32
     * 
     */
    private synchronized void putInCache(String iconHash, String componentId, byte[] icon16, byte[] icon24, byte[] icon32) {
        if (!NULL_HASH.equals(iconHash)) {
            componentId = ComponentImageUtility.getNormalId(componentId);
            idIconMap.put(componentId, iconHash); // save mapping, if already existing it overrides the old mapping
            if (!iconHashes.contains(iconHash)) {
                createRawDataFile(iconHash, icon16, icon24, icon32);
                moveItemToTopinQueue(iconHash);
            }
        }
    }

    @Override
    public byte[] getImageData(String iconHash, IconSize size) {
        if (idIconMap.containsValue(iconHash)) {
            Path rawDataPath = Paths.get(indexFile.getParent().toString(), iconHash);
            try {
                byte[] toReturn;
                List<String> rawDataList;
                synchronized (this) {
                    rawDataList = Files.readAllLines(rawDataPath, StandardCharsets.UTF_8);
                }
                switch (size) {
                case ICON16:
                    toReturn = Base64.getDecoder().decode(rawDataList.get(0));
                    break;
                case ICON24:
                    toReturn = Base64.getDecoder().decode(rawDataList.get(1));
                    break;
                case ICON32:
                    toReturn = Base64.getDecoder().decode(rawDataList.get(2));
                    break;
                default:
                    throw new IllegalArgumentException("Requested icon size is not defined inside this switch case");
                }

                synchronized (this) {
                    moveItemToTopinQueue(iconHash);
                    syncFilesWithQueue();
                }

                return toReturn;

            } catch (IOException e) {
                LogFactory.getLog(getClass())
                    .error("Could not read raw data file of the ComponentImageCache for hash " + iconHash, e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Reads the cache from disk and saves it into the queue.
     */
    private void readCache() {
        setMainCacheFiles();
        readIndex();
        readMappingFile();
    }

    /**
     * This method moves items inside the queue. If the the element is not existing, it will be added at first position
     * 
     * @param iconhash of the image which has to be moved in queue.
     */
    private synchronized void moveItemToTopinQueue(String iconhash) {
        if (iconHashes.contains(iconhash)) {
            iconHashes.remove(iconhash);
        }
        iconHashes.addFirst(iconhash);
        if (iconHashes.size() > CACHE_SIZE) {
            hashesToRemove.add(iconHashes.removeLast());
        }
    }

    /**
     * Sync all changes inside the queue to the cache files.
     */
    private synchronized void syncFilesWithQueue() {
        for (String entry : iconHashes) {
            // prevents removing hashes from file system if an entry was removed and added again during the same loop of component updates
            hashesToRemove.remove(entry);
        }
        for (String entry : hashesToRemove) {
            try {
                // deleting all unnecessary icon files
                Path pathToDelete = Paths.get(indexFile.getParent().toString(), entry);
                if (pathToDelete.toFile().exists()) {
                    Files.delete(pathToDelete);
                }
            } catch (IOException e) {
                LogFactory.getLog(getClass())
                    .error("Could not delete raw data file of the ComponentImageCache with icon hash: " + entry, e);
            }

            if (idIconMap.containsValue(entry)) {
                // removing all entries in the mapping file with deleted icon hashes
                Iterator<Map.Entry<String, String>> iterator = idIconMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> tmpEntry = iterator.next();
                    if (tmpEntry.getValue().equals(entry)) {
                        iterator.remove();
                    }
                }
            }

        }
        hashesToRemove.clear();
        try {
            // overrides the whole file with the queue state at the moment of execution
            Files.write(indexFile, iconHashes, StandardOpenOption.WRITE);
            // override the whole mapping file with the mapping state at the moment of execution
            writeMappingFile();
        } catch (IOException e) {
            LogFactory.getLog(getClass())
                .error("Could not write index file of the ComponentImageCache", e);
        }
    }

    /**
     * Creates a new file with the raw image data inside. If the file is already existing it will be overwritten. The raw data will be
     * encoded in Base64.
     * 
     * @param iconhash hash of the icon group, will be the file name
     * @param icon16 raw image data
     * @param icon24 raw image data
     * @param icon32 raw image data
     */
    private synchronized void createRawDataFile(String iconhash, byte[] icon16, byte[] icon24, byte[] icon32) {
        Path rawDataPath = Paths.get(indexFile.getParent().toString(), iconhash);
        if (!rawDataPath.toFile().exists()) {
            try {
                rawDataPath.toFile().createNewFile();
            } catch (IOException e) {
                LogFactory.getLog(getClass())
                    .error("Could not create raw data file to cache for icon hash: " + iconhash, e);
            }
            try {
                LinkedList<String> byteList = new LinkedList<>();
                // avoids null pointer on encoding byte array
                if (icon16 == null) {
                    icon16 = new byte[0];
                }
                byteList.add(Base64.getEncoder().encodeToString(icon16));
                if (icon24 == null) {
                    icon24 = new byte[0];
                }
                byteList.add(Base64.getEncoder().encodeToString(icon24));
                if (icon32 == null) {
                    icon32 = new byte[0];
                }
                byteList.add(Base64.getEncoder().encodeToString(icon32));

                Files.write(rawDataPath, byteList, StandardOpenOption.WRITE);

            } catch (IOException e) {
                LogFactory.getLog(getClass())
                    .error("Could not write to raw data file to cache for icon hash: " + iconhash, e);
            }
        }

    }

    @Reference
    private void bindConfigurationService(ConfigurationService configurationService) {
        this.configService = configurationService;
    }

    @Reference
    private void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService knowledgeService) {
        this.knowledgService = knowledgeService;
    }

    /**
     * Sets the path of the index file of the ComponentImageCache. If the file or parent directory is not existing, it will be created. In
     * case of failure null is returned.
     * 
     */
    private void setMainCacheFiles() {
        Path cachePath;
        if (configService != null) {
            cachePath =
                Paths.get(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA).toString(), CACHE_DIRECTORY);
            if (!cachePath.toFile().exists()) {
                cachePath.toFile().mkdir();
            }
            indexFile = Paths.get(cachePath.toString(), INDEX);
            mappingFile = Paths.get(cachePath.toString(), MAPPING);

            List<Path> pathList = new ArrayList<>();
            pathList.add(indexFile);
            pathList.add(mappingFile);

            for (Path entry : pathList) {
                if (!entry.toFile().exists()) {
                    try {
                        entry.toFile().createNewFile();
                    } catch (IOException e) {
                        LogFactory.getLog(getClass()).error("Could not create " + entry.toString() + " file of component image cache.", e);
                    }
                }
            }
        } else {
            LogFactory.getLog(getClass())
                .debug("Could not set files of component image cache because ConfigurationService was not available.");

        }
    }

    /**
     * Reads the cache from disk and saves it into the queue.
     */
    private void readIndex() {
        if (indexFile != null) {
            try (Stream<String> stream = Files.lines(indexFile, StandardCharsets.UTF_8)) {
                stream.forEach(s -> iconHashes.add(s));
            } catch (IOException e) {
                LogFactory.getLog(getClass())
                    .error("Could not read index file of the ComponentImageCache", e);
            }
        }

    }

    /**
     * Writes the componendId/iconHash mapping into the mapping file. Expression: componendId:iconHash
     */
    private void writeMappingFile() {
        List<String> linesToWrite = new ArrayList<>();
        StringBuilder stringBuilder;
        String divider = ":";
        for (Map.Entry<String, String> entry : idIconMap.entrySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(entry.getKey());
            stringBuilder.append(divider);
            stringBuilder.append(entry.getValue());
            linesToWrite.add(stringBuilder.toString());
        }
        try {
            Files.write(mappingFile, linesToWrite, StandardOpenOption.WRITE);
        } catch (IOException e) {
            LogFactory.getLog(getClass()).error("Could not create to mapping file of component image cache.", e);
        }

    }

    private void readMappingFile() {
        List<String> mappingList = new ArrayList<>();
        try (Stream<String> stream = Files.lines(mappingFile, StandardCharsets.UTF_8)) {
            stream.forEach(mappingList::add);

            for (String entry : mappingList) {
                String[] stringParts = entry.split(":");
                idIconMap.put(stringParts[0], stringParts[1]);
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass())
                .error("Could not read mapping file of the ComponentImageCache", e);
        }
    }

    /**
     * Updates the cache with the newest images. Local images are preferred to shared ones.
     * 
     * @param knowledge the newest knowledge about components
     */
    private synchronized void addUnknownComponentsToCache(DistributedComponentKnowledge knowledge) {
        Collection<DistributedComponentEntry> installations =
            ComponentImageUtility.getDistinctInstallations(knowledge.getAllInstallations());
        for (DistributedComponentEntry entry : installations) {
            ComponentInterface tmpCi = entry.getComponentInterface();
            putInCache(tmpCi.getIconHash(), tmpCi.getIdentifierAndVersion(), tmpCi.getIcon16(), tmpCi.getIcon24(),
                tmpCi.getIcon32());
        }
        syncFilesWithQueue();
    }

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        addUnknownComponentsToCache(newState);
    }

    @Override
    public String getIconHash(String componentId) {
        if (idIconMap.containsKey(componentId)) {
            return idIconMap.get(componentId);
        } else {
            return null;
        }
    }
}
