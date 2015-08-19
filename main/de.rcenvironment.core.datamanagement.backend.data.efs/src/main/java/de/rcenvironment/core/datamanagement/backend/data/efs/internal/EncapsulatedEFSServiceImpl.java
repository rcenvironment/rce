/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
