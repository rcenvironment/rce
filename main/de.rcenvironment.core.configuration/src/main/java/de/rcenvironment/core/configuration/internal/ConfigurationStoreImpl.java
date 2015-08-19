/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.util.DefaultPrettyPrinter.Lf2SpacesIndenter;

import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * Default {@link ConfigurationStore} implementation.
 * 
 * @author Robert Mischke
 */
public class ConfigurationStoreImpl implements ConfigurationStore {

    private File storageFile;

    private final Log log = LogFactory.getLog(getClass());

    public ConfigurationStoreImpl(File storageFile) {
        this.storageFile = storageFile;
    }

    @Override
    public ConfigurationSegment getSnapshotOfRootSegment() throws IOException {
        try {
            if (!storageFile.exists()) {
                // return empty placeholder
                return new WritableConfigurationSegmentImpl(null);
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(org.codehaus.jackson.JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(org.codehaus.jackson.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            JsonNode node = mapper.readTree(storageFile);
            return new WritableConfigurationSegmentImpl(node);
        } catch (JsonParseException e) {
            throw new IOException("Malformed configuration file: " + e.toString());
        }
    }

    @Override
    public ConfigurationSegment createEmptyPlaceholder() {
        return new WritableConfigurationSegmentImpl(null);
    }

    /**
     * Custom Jackson JSON indenter that uses 4 spaces instead of 2.
     * 
     * @author Robert Mischke
     */
    private static class CustomIndenter extends Lf2SpacesIndenter {

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException, JsonGenerationException {
            super.writeIndentation(jg, level * 2);
        }
    }

    @Override
    public void update(ConfigurationSegment configuration) throws IOException {
        // TODO >6.0.0: quick & dirty; make more reliable
        File backupFile = new File(storageFile.getParentFile(), storageFile.getName() + "." + System.currentTimeMillis() + ".bak");
        log.debug("Creating backup of existing configuration file at " + backupFile);
        Files.move(storageFile.toPath(), backupFile.toPath());
        writeJsonFile(((WritableConfigurationSegmentImpl) configuration).getRootSegment().getSegmentRootNode(), storageFile);
        // NOTE: for debugging only
        // log.debug(FileUtils.readFileToString(storageFile));
    }

    @Override
    public void exportToFile(ConfigurationSegment configurationSegment, File destinationFile) throws IOException {
        JsonNode segmentRootNode = ((WritableConfigurationSegmentImpl) configurationSegment).getSegmentRootNode();
        if (segmentRootNode != null) {
            writeJsonFile(segmentRootNode, destinationFile);
        } else {
            FileUtils.write(destinationFile, "{}");
        }
    }

    private void writeJsonFile(JsonNode jsonRootNode, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(file, JsonEncoding.UTF8);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        CustomIndenter customIndenter = new CustomIndenter();
        prettyPrinter.indentObjectsWith(customIndenter);
        prettyPrinter.indentArraysWith(customIndenter);
        jsonGenerator.setPrettyPrinter(prettyPrinter);
        mapper.writeTree(jsonGenerator, jsonRootNode);
    }
}
