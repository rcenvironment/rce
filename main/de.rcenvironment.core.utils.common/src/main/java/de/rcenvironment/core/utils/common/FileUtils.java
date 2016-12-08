/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.File;

/**
 * A utility class for file operations.
 * 
 * @author Tobias Rodehutskors
 */
public final class FileUtils {

    private FileUtils() {

    }
    
    /**
     * Checks if a file is locked by trying to rename it.
     * 
     * TODO We need to check if this implementation is also a valid check on Linux. ~ rode_to
     * 
     * @param file The file to inspect.
     * @return True, if the file is locked.
     */
    public static boolean isLocked(File file) {
        return !file.renameTo(file);
    }
}
