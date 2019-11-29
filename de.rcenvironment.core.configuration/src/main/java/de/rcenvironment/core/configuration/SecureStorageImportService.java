/*
 * Copyright (C) 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.File;
import java.util.function.BiFunction;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Provides methods to import string data from files into secure storage fields. Currently used to set/import connection passwords in
 * headless operation.
 *
 * @author Robert Mischke
 */
public interface SecureStorageImportService {

    /**
     * Looks for files in the given directory, and attempts to import each one as entries in the specified secure storage node/path. The key
     * and value of the generated entry are derived by the provided mapping functions, or by standard mappings if these parameters are
     * "null". Once a file has been successfully imported, it is deleted from the directory. If the directory does not exist, this method
     * exits without an error.
     * 
     * @param importDirectory the directory to check for files; subdirectories are not entered
     * @param secureStoreParentPath the secure storage node/path to add new entries to
     * @param keyMapping an optional custom mapping function to derive the secure storage entry key from the filename (1st argument) and
     *        content (2nd argument) of the given file; return null to signal that the file should be IGNORED (not imported); not that this
     *        is different from the null behavior of the value mapping. if the mapping function itself is null, the filename (minus any
     *        standard extensions) is used as the key.
     * @param valueMapping an optional custom mapping function to derive the secure storage entry value from the filename (1st argument) and
     *        content (2nd argument) of the given file; return null to signal that the already identified key should be DELETED from secure
     *        storage; note that this is different from the behavior of the key mapping. if the mapping function itself is null, the file's
     *        content is used as the value.
     * @param trim whether the file's content should be whitespace-trimmed before passing it to the mapping functions
     * @param failOnMultiLine if true, log an error and skip if a file's contains \r or \n characters; this is checked <em>after</em> the
     *        optional whitespace trimming
     * @throws OperationFailureException on a critical error, e.g. an unavailable secure storage; note that file I/O errors are logged as
     *         individual warnings, but do not cause this exception to be thrown
     */
    void processImportDirectory(File importDirectory, String secureStoreParentPath, BiFunction<String, String, String> keyMapping,
        BiFunction<String, String, String> valueMapping, boolean trim, boolean failOnMultiLine) throws OperationFailureException;

    /**
     * Attempts to import the given file as an entry in the specified secure storage node/path. The key and value of the generated entry are
     * derived by the provided mapping functions, or by standard mappings if these parameters are "null". Once the file has been
     * successfully imported, it is deleted.
     * 
     * @param importFile the file to import
     * @param secureStoreParentPath the secure storage node/path to add new entries to
     * @param keyMapping an optional custom mapping function to derive the secure storage entry key from the filename (1st argument) and
     *        content (2nd argument) of the given file; return null to signal that the file should not be imported. if the mapping function
     *        itself is null, the filename (minus any standard extensions) is used as the key.
     * @param valueMapping an optional custom mapping function to derive the secure storage entry value from the filename (1st argument) and
     *        content (2nd argument) of the given file; return null to signal that the file should not be imported. if the mapping function
     *        itself is null, the file's content is used as the value.
     * @param trim whether the file's content should be whitespace-trimmed before passing it to the mapping functions
     * @param failOnMultiLine if true, log an error and skip if a file's contains \r or \n characters; this is checked <em>after</em> the
     *        optional whitespace trimming
     * @throws OperationFailureException on a critical error, e.g. an unavailable secure storage; note that file I/O errors are only logged
     *         as a warning, but do not cause this exception to be thrown
     * 
     * @return true if the file was successfully imported
     */
    boolean processImportFile(File importFile, String secureStoreParentPath, BiFunction<String, String, String> keyMapping,
        BiFunction<String, String, String> valueMapping, boolean trim, boolean failOnMultiLine) throws OperationFailureException;
}
