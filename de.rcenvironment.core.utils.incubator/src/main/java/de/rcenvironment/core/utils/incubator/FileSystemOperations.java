/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provides reusable file system operations.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public final class FileSystemOperations {

    /**
     * The {@link FileVisitor} of the {@link FileSystemOperations#deleteSandboxDirectory(File)} method. Most of this method's functionality
     * is provided by this visitor's behavior.
     * 
     * @author Robert Mischke
     */
    private static final class FilesAndDirsDeletionFileVisitor extends SimpleFileVisitor<Path> {

        private final Log log;

        private final DirectoryDeletionStats stats;

        private FilesAndDirsDeletionFileVisitor(Log log, DirectoryDeletionStats stats) {
            this.log = log;
            this.stats = stats;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isRegularFile()) {
                try {
                    Files.delete(file);
                    stats.filesDeleted++;
                } catch (IOException e) {
                    log.warn(StringUtils.format("Failed to delete %s: %s", file.toString(), e.toString()));
                    stats.errors++;
                }
            } else if (attrs.isSymbolicLink()) {
                try {
                    log.debug(StringUtils.format("Deleting symbolic link %s", file.toString()));
                    Files.delete(file); // note: this deletes the symlink, NOT the target it points to
                    stats.symlinksDeleted++;
                } catch (IOException e) {
                    log.warn(StringUtils.format("Failed to delete symbolic link %s: %s", file.toString(), e.toString()));
                    stats.errors++;
                }
            } else {
                log.warn("Not deleting a file as it is neither a normal file nor a symbolic link: " + file.toString());
                stats.errors++;
            }
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // conservative check; may never be triggered - misc_ro
            if (!attrs.isDirectory()) {
                log.warn("Unexpected type of directory (please examine manually): " + dir.toString());
                return FileVisitResult.SKIP_SUBTREE;
            }
            // If the directory is a junction, the junction itself is deleted, but the contents are not.
            if (attrs.isOther()) {
                try {
                    log.debug(StringUtils.format("Deleting junction point %s", dir.toString()));
                    Files.delete(dir);
                    stats.junctionsDeleted++;
                } catch (IOException e) {
                    log.warn(StringUtils.format("Failed to delete junction point %s: %s", dir.toString(), e.toString()));
                    stats.errors++;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            try {
                Files.delete(dir);
                stats.directoriesDeleted++;
            } catch (DirectoryNotEmptyException e) {
                log.warn(StringUtils.format("Cannot delete directory %s as it is not empty", dir.toString()));
                // do not count this as an error, as it is usually the follow-up of a previous error
            }
            return super.postVisitDirectory(dir, exc);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
            try {
                /* One possibility why the visit could have failed is that the file is a junction to a directory that does not exist
                 anymore. Therefore, the file is deleted now.*/
                log.debug(StringUtils.format(
                    "Deleting unknown type of file/directory %s (possibly a link/junction to a non-existing target)", file.toString()));
                Files.delete(file);
                stats.unknownDeleted++;
            } catch (IOException e1) {
                log.warn(StringUtils.format("Failed to delete unknown type of file/directory %s: %s", file.toString(), e1.toString()));
                stats.errors++;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Simple holder to share some counters between the deletion {@link FileVisitor} and the calling method.
     * 
     * @author Robert Mischke
     */
    private static final class DirectoryDeletionStats {

        private int filesDeleted = 0; // assuming that 2^31 files are sufficient for now ;-)

        private int symlinksDeleted = 0;

        private int directoriesDeleted = 0;

        private int junctionsDeleted = 0;

        private int unknownDeleted = 0;

        private int errors = 0;
    }

    private FileSystemOperations() {

    }

    /**
     * Recursively deletes the given directory, with additional protection against symbolic links or junction points in that directory that
     * point to locations outside of it. This kind of symbolic links is dangerous as it might delete files outside of the given directory
     * with the permissions of the current system user.
     * 
     * If a symbolic link is encountered, a debug message is logged and an attempt is made to delete the symbolic link (instead of the
     * target it points to).
     * 
     * @param directory the directory to delete (with all its contents)
     */
    public static void deleteSandboxDirectory(File directory) {
        // make absolute for proper log output, if necessary
        final File absoluteDirectory = directory.getAbsoluteFile();
        final Log log = LogFactory.getLog(FileSystemOperations.class);
        final DirectoryDeletionStats stats = new DirectoryDeletionStats();

        try {
            // note: this call does not follow symlinks when traversing
            Files.walkFileTree(absoluteDirectory.toPath(), new FilesAndDirsDeletionFileVisitor(log, stats));
        } catch (IOException e) {
            log.error(StringUtils.format("Uncaught exception while trying to delete directory %s", absoluteDirectory.toString()), e);
        }
        if (!directory.exists()) {
            log.debug(StringUtils
                .format(
                    "Successfully deleted %s (which consisted of %d files, %d symbolic links, %d directories, %d junctions and"
                        + " %d files/directories of unknown type.)",
                    absoluteDirectory.toString(), stats.filesDeleted, stats.symlinksDeleted, stats.directoriesDeleted,
                    stats.junctionsDeleted, stats.unknownDeleted));
        } else {
            log.warn(StringUtils
                .format(
                    "Failed to fully delete directory %s (deleted %d files, %d symbolic links, %d directories %d junctions and"
                        + " %d files/directories of unknown type; encountered %d errors)",
                    absoluteDirectory.toString(), stats.filesDeleted, stats.symlinksDeleted, stats.directoriesDeleted,
                    stats.junctionsDeleted, stats.unknownDeleted, stats.errors));
        }
    }
}
