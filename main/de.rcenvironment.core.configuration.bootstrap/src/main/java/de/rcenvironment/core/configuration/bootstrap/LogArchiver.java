/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * Keeps archival copies of log files. This was introduced as the configuration abilities of Log4J (like RollingFileAppender) to not cover
 * the desired behavior.
 * 
 * @author Robert Mischke
 */
public final class LogArchiver {

    private static final String LOG_FILE_SUFFIX = ".log";

    private static final String ARCHIVE_FILE_SUFFIX = ".previous.log";

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
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(ARCHIVE_FILE_SUFFIX);
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
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(LOG_FILE_SUFFIX);
            }
        });

        for (File logFile : lastRunsLogs) {
            File archiveFile = new File(logFile.getParentFile(), logFile.getName().replace(LOG_FILE_SUFFIX, ARCHIVE_FILE_SUFFIX));
            try {
                Files.move(logFile.toPath(), archiveFile.toPath());
            } catch (IOException e) {
                sysErr.println("ERROR: Failed to archive the previous run's log file " + logFile.getAbsolutePath() + " as "
                    + archiveFile.getAbsolutePath());
            }
        }
    }
}
