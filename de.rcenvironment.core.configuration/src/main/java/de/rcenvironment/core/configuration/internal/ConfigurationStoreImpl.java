/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Default {@link ConfigurationStore} implementation.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class ConfigurationStoreImpl implements ConfigurationStore {

    private static final AtomicInteger sharedBackupDisambiguationNumberSequence = new AtomicInteger(1);

    private File storageFile;

    private boolean backupFileCreated = false;

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
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            validateJsonSyntax(mapper);
            JsonNode node = mapper.readTree(storageFile);
            return new WritableConfigurationSegmentImpl(node);
        } catch (JsonParseException e) {
            throw new IOException("Malformed configuration file: " + e.toString());
        }
    }

    // mapper.readTree() (the way to read the file in the first place) does not check the syntax of the entire file, but that's needed to
    // inform the user about a malformed configuration file. Feel free to improve the syntax check. Didn't find any nicer.
    private void validateJsonSyntax(ObjectMapper mapper) throws JsonParseException, IOException {
        try (JsonParser parser = mapper.getJsonFactory().createJsonParser(storageFile)) {
            // syntax check of entire file
            while (parser.nextToken() != null) {
                // nothing to do here
                parser.version(); // for checkstyle purposes
            }
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
     * @author Alexander Weinert
     */
    private static class CustomIndenter extends DefaultIndenter {

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException, JsonGenerationException {
            super.writeIndentation(jg, level * 2);
        }
    }

    @Override
    public void update(ConfigurationSegment segment) throws ConfigurationException, IOException {
        final WritableConfigurationSegmentImpl rootSegment = ((WritableConfigurationSegmentImpl) segment).getRootSegment();
        if (rootSegment != segment) {
            // could be made to work nonetheless, but enforce good calling practice
            throw new IOException("The parameter passed to the save() method was not a root segment");
        }
        // TODO >7.0.0: add proper locking! not thread-safe at the moment!
        // TODO >6.0.0: quick & dirty; make more reliable

        // only create single backup file per instance lifetime
        if (!backupFileCreated) {
            File backupFile = new File(storageFile.getParentFile(), storageFile.getName() + "." + System.currentTimeMillis() + "-"
                + sharedBackupDisambiguationNumberSequence.getAndIncrement() + ".bak");
            log.debug("Creating backup of existing configuration file at " + backupFile);
            Files.move(storageFile.toPath(), backupFile.toPath());
            backupFileCreated = true;
        }

        writeJsonFile(rootSegment.getSegmentRootNode(), storageFile);
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
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(file, JsonEncoding.UTF8)) {
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            CustomIndenter customIndenter = new CustomIndenter();
            prettyPrinter.indentObjectsWith(customIndenter);
            prettyPrinter.indentArraysWith(customIndenter);
            jsonGenerator.setPrettyPrinter(prettyPrinter);
            mapper.writeTree(jsonGenerator, jsonRootNode);
        }
    }
}
