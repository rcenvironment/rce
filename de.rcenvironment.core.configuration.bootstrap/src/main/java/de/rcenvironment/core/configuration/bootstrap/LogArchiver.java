/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.regex.Pattern;

/**
 * Keeps archival copies of log files. This was introduced as the configuration abilities of Log4J (like RollingFileAppender) to not cover
 * the desired behavior.
 * 
 * @author Robert Mischke
 */
public final class LogArchiver {

    private static final Pattern CURRENT_LOG_FILE_PATTERN = Pattern.compile("^(debug|warnings)\\.(log(\\.\\d+)?)$");

    private static final Pattern ARCHIVED_LOG_FILE_PATTERN = Pattern.compile("^(\\w+)\\.(previous\\.log(\\.\\d+)?)$");

    private static final String CURRENT_TO_ARCHIVED_LOG_FILE_REPLACEMENT = "$1.previous.$2";

    private LogArchiver() {}

    /**
     * Execute in the given directory.
     * 
     * @param directory the directory to archive log files in
     */
    public static void run(File directory) {

        deleteOldArchiveFiles(directory);

        archiveLastRunsLogs(directory);
    }

    private static void deleteOldArchiveFiles(File directory) {
        final PrintStream sysErr = System.err; // the log system is not available yet, so log errors to SysErr
        File[] oldArchiveFiles = directory.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && ARCHIVED_LOG_FILE_PATTERN.matcher(file.getName()).matches();
            }
        });
        for (File oldFile : oldArchiveFiles) {
            try {
                Files.delete(oldFile.toPath());
                if (oldFile.exists()) {
                    sysErr.println("ERROR: Failed to delete archived log file " + oldFile.getAbsolutePath());
                }
            } catch (IOException e) {
                sysErr.println("ERROR: Failed to delete archived log file " + oldFile.getAbsolutePath() + ": " + e.toString());
            }
        }
    }

    private static void archiveLastRunsLogs(File directory) {
        final PrintStream sysErr = System.err; // the log system is not available yet, so log errors to SysErr
        File[] lastRunsLogs = directory.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && CURRENT_LOG_FILE_PATTERN.matcher(file.getName()).matches();
            }
        });

        for (File logFile : lastRunsLogs) {
            final String oldName = logFile.getName();
            final String newName = CURRENT_LOG_FILE_PATTERN.matcher(oldName).replaceFirst(CURRENT_TO_ARCHIVED_LOG_FILE_REPLACEMENT);
            final File archiveFile = new File(logFile.getParentFile(), newName);
            try {
                Files.move(logFile.toPath(), archiveFile.toPath());
            } catch (IOException e) {
                sysErr.println("ERROR: Failed to archive the previous run's log file " + logFile.getAbsolutePath() + " as "
                    + archiveFile.getAbsolutePath());
            }
        }
    }
}
