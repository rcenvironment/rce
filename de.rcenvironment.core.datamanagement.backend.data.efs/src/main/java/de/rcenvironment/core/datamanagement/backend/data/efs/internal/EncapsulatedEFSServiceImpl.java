/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;


/**
 * Implementation of {@link EncapsulatedEFSService}.
 *
 * @author Doreen Seider
 */
public class EncapsulatedEFSServiceImpl implements EncapsulatedEFSService {

    @Override
    public IFileStore getStore(URI uri) throws CoreException {
        return EFS.getStore(uri);
    }
}
