/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch.executor.context;

import java.util.Random;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Simple abstraction of remote temporary directory path generation. Could be expanded into a
 * management class that keeps track of and disposes the generated directories.
 * 
 * @author Robert Mischke
 * 
 */
class RemoteTempDirFactory {

    private static final int MIN_PLAUSIBLE_ROOT_DIR_LENGTH = "/tmp".length();

    /**
     * The provided root dir path, normalized to end with "/".
     */
    private final String rootDir;

    private final Random random = new Random();

    RemoteTempDirFactory(String rootDir) {
        // basic root dir checks
        if (rootDir == null || rootDir.length() < MIN_PLAUSIBLE_ROOT_DIR_LENGTH) {
            throw new IllegalArgumentException("Invalid root path: " + rootDir);
        }
        // normalize
        if (rootDir.charAt(rootDir.length() - 1) != '/') {
            rootDir = rootDir + "/";
        }
        this.rootDir = rootDir;
    }

    /**
     * Returns a new, most-likely-unique temp directory. Note that the directory is not actually
     * created; this method only generates a unique path string.
     * 
     * @param contextHint a hint text that is used as part of the directory path to simplify
     *        recognizing created directories; must only contain characters valid for all relevant
     *        file systems
     * @param separator an arbitrary separator part (like "-", "." or similar) to join directory
     *        name segments
     * @return the full path name of the new temp directory
     */
    public String createTempDirPath(String contextHint, String separator) {
        // although the JavaDoc does not state this, nextInt() is implemented thread-safely
        int randInt = random.nextInt();
        return StringUtils.format("%s%s%s%d%s%d", rootDir, contextHint, separator, System.currentTimeMillis(), separator,
            Math.abs(randInt));
    }
    
    public String getRootDir() {
        return rootDir;
    }

}
