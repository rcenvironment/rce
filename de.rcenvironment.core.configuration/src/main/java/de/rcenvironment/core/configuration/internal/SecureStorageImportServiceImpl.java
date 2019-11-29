/*
 * Copyright (C) 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.function.BiFunction;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.configuration.SecureStorageImportService;
import de.rcenvironment.core.configuration.SecureStorageService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link SecureStorageImportService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class SecureStorageImportServiceImpl implements SecureStorageImportService {

    @Reference
    private SecureStorageService secureStorageService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void processImportDirectory(File importDirectory, String secureStoreParentPath, BiFunction<String, String, String> keyMapping,
        BiFunction<String, String, String> valueMapping, boolean trim, boolean failOnMultiLine) throws OperationFailureException {
        if (!importDirectory.isDirectory()) {
            log.debug(
                "Import directory " + importDirectory.getAbsolutePath() + " does not exist or is not a directory, so it will be ignored");
            return;
        }
        log.debug("Checking import directory " + importDirectory.getAbsolutePath());
        final Collection<File> files = FileUtils.listFiles(importDirectory, null, false); // no filtering, not recursive
        for (File file : files) {
            processImportFile(file, secureStoreParentPath, keyMapping, valueMapping, trim, failOnMultiLine);
        }
    }

    @Override
    public boolean processImportFile(File importFile, String secureStoreParentPath, BiFunction<String, String, String> keyMapping,
        BiFunction<String, String, String> valueMapping, boolean trim, boolean failOnMultiLine) throws OperationFailureException {
        importFile = importFile.getAbsoluteFile(); // resolve this once for all places, especially logging
        log.debug("Parsing import file " + importFile);
        String content;
        try {
            content = FileUtils.readFileToString(importFile, Charsets.UTF_8);
        } catch (IOException e) {
            log.warn(StringUtils.format("Failed to read %s: %s", importFile, e.toString()));
            return false;
        }
        if (trim) {
            content = org.apache.commons.lang3.StringUtils.trim(content);
        }
        if (failOnMultiLine && org.apache.commons.lang3.StringUtils.containsAny(content, '\r', '\n')) {
            log.warn("Ignoring import file " + importFile + " as it contains more than one line of text");
            return false;
        }

        // at this point, the actual import can begin; determine key and value for the new (or updated) entry

        String sanitizedFileName = importFile.getName();

        // TODO very simple sanitation for now (only cut away optional .txt extension); could certainly be improved
        if (sanitizedFileName.endsWith(".txt")) {
            sanitizedFileName = sanitizedFileName.substring(0, sanitizedFileName.length() - 4);
        }

        String key = deriveStorageKey(sanitizedFileName, content, keyMapping);
        if (key == null) {
            log.info("Ignoring input file " + importFile.getAbsolutePath() + "; check recent log output for details");
            return false;
        }

        String value = deriveStorageValue(sanitizedFileName, content, valueMapping);
        if (value != null) {
            // normal import
            try {
                secureStorageService.getSecureStorageSection(secureStoreParentPath).store(key, value);
            } catch (IOException e) {
                // if this fails, actually throw an exception
                throw new OperationFailureException("Failed to store the imported data read from " + importFile + ": " + e.toString());
            }
        } else {
            // deletion
            try {
                secureStorageService.getSecureStorageSection(secureStoreParentPath).delete(key);
            } catch (IOException e) {
                // if this fails, actually throw an exception
                throw new OperationFailureException(
                    "Failed to delete an entry based on the data read from " + importFile + ": " + e.toString());
            }
        }

        // import successful -> delete the import file
        try {
            Files.delete(importFile.toPath());
            log.info("Successfully imported and deleted " + importFile);
            return true;
        } catch (IOException e) {
            log.warn("Successfully imported, but failed to delete " + importFile);
            // for now, return "true" anyway as the import itself was successful; adapt if necessary
            return true;
        }
    }

    private String deriveStorageKey(String sanitizedFileName, String content, BiFunction<String, String, String> keyMapping) {
        if (keyMapping != null) {
            return keyMapping.apply(sanitizedFileName, content);
        } else {
            return sanitizedFileName;
        }
    }

    private String deriveStorageValue(String sanitizedFileName, String content, BiFunction<String, String, String> valueMapping) {
        if (valueMapping != null) {
            return valueMapping.apply(sanitizedFileName, content);
        } else {
            return content;
        }
    }

}
