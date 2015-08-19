/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.internal;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.authentication.SingleUser;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementUtil;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * Implementation of {@link ComponentDataManagemenService}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ComponentDataManagementServiceImpl implements ComponentDataManagementService {

    private TypedDatumFactory typedDatumFactory;

    private DataManagementService dataManagementService;

    @Override
    public String createTaggedReferenceFromLocalFile(ComponentContext componentContext, File file, String filename) throws IOException {
        MetaDataSet mds = new MetaDataSet();
        ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);

        // if filename parameter is not null, tag the reference with a filename
        if (filename != null) {
            String effectiveFilename;
            if (filename == ComponentDataManagementService.SAME_FILENAME) {
                // "magic" constant: use the filename of the provided file
                effectiveFilename = file.getName();
            } else {
                // otherwise, use provided custom filename
                effectiveFilename = filename;
            }
            ComponentDataManagementUtil.setAssociatedFilename(mds, effectiveFilename);
        }

        try {
            return dataManagementService.createReferenceFromLocalFile(createUser(), file, mds,
                componentContext.getDefaultStorageNodeId());
        } catch (InterruptedException e) {
            // reduce exception types
            throw new IOException(e);
        }
    }

    @Override
    public String createTaggedReferenceFromString(ComponentContext componentContext, String stringValue) throws IOException {
        MetaDataSet mds = new MetaDataSet();
        ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);

        try {
            return dataManagementService.createReferenceFromString(createUser(), stringValue, mds,
                componentContext.getDefaultStorageNodeId());
        } catch (InterruptedException e) {
            // reduce exception types
            throw new IOException(e);
        }
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, NodeIdentifier nodeId) throws IOException {
        dataManagementService.copyReferenceToLocalFile(createUser(), reference, targetFile, nodeId);
    }

    @Override
    public String retrieveStringFromReference(String reference, NodeIdentifier nodeId) throws IOException {
        return dataManagementService.retrieveStringFromReference(createUser(), reference, nodeId);
    }

    @Override
    public FileReferenceTD createFileReferenceTDFromLocalFile(ComponentContext componentContext, File file, String filename)
        throws IOException {
        if (!file.exists()) {
            throw new IOException("File doesn't exist: " + file.getAbsolutePath());
        }
        String reference;
        try {
            MetaDataSet mds = new MetaDataSet();
            ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);
            reference = dataManagementService.createReferenceFromLocalFile(createUser(), file, mds,
                componentContext.getDefaultStorageNodeId());
        } catch (InterruptedException e) {
            // reduce exception types
            throw new IOException(e);
        }
        return typedDatumFactory.createFileReference(reference, filename);
    }

    @Override
    public DirectoryReferenceTD createDirectoryReferenceTDFromLocalDirectory(ComponentContext componentContext, File dir, String dirname)
        throws IOException {
        if (!dir.exists()) {
            throw new IOException("Directory doesn't exist: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IOException("Path doesn't refer to directory: " + dir.getAbsolutePath());
        }
        String reference;
        try {
            MetaDataSet mds = new MetaDataSet();
            ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);
            reference = dataManagementService.createReferenceFromLocalDirectory(createUser(), dir, mds,
                componentContext.getDefaultStorageNodeId());
        } catch (InterruptedException e) {
            // reduce exception types
            throw new IOException(e);
        }
        return typedDatumFactory.createDirectoryReference(reference, dirname);
    }

    @Override
    public void copyFileReferenceTDToLocalFile(ComponentContext componentContext, FileReferenceTD fileReference, File targetFile)
        throws IOException {
        copyReferenceToLocalFile(fileReference.getFileReference(), targetFile, componentContext.getDefaultStorageNodeId());
    }

    @Override
    public void copyDirectoryReferenceTDToLocalDirectory(ComponentContext componentContext, DirectoryReferenceTD dirReference,
        File targetDir) throws IOException {
        dataManagementService.copyReferenceToLocalDirectory(createUser(), dirReference.getDirectoryReference(), targetDir,
            componentContext.getDefaultStorageNodeId());
    }

    @Override
    public void copyDirectoryReferenceTDToLocalDirectory(DirectoryReferenceTD dirReference, File targetDir, NodeIdentifier node)
        throws IOException {
        dataManagementService.copyReferenceToLocalDirectory(createUser(), dirReference.getDirectoryReference(), targetDir, node);
    }

    // bridge code
    private User createUser() {
        final int validityInDays = 9999;
        return new SingleUser(validityInDays);
    }

    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumFactory = typedDatumService.getFactory();
    }

    protected void bindDataManagementService(DataManagementService newDataManagementService) {
        dataManagementService = newDataManagementService;
    }

}
