/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;

/**
 * Helper service encapsulating EFS in order to get {@link EFSDataBackend} testable.
 *
 * @author Doreen Seider
 */
public interface EncapsulatedEFSService {

    /**
     * Returns a file from the EFS file system.
     * 
     * @param uri The URI pointing to the file.
     * @return {@link IFileStore} representing the file.
     * @throws CoreException if an error occurs.
     */
    IFileStore getStore(URI uri) throws CoreException;

}
