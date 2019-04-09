/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.internal;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementUtil;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link ComponentDataManagemenService}.
 * 
 * TODO This class has conceptual overlap with ComponentExecutionStorageBridge - consider whether it makes sense to merge them. -- misc_ro
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Brigitte Boden
 * @author Robert Mischke
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
            return dataManagementService.createReferenceFromLocalFile(file, mds,
                getStorageNetworkDestination(componentContext));
        } catch (InterruptedException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
    }

    @Override
    public String createTaggedReferenceFromString(ComponentContext componentContext, String stringValue) throws IOException {
        MetaDataSet mds = new MetaDataSet();
        ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);

        try {
            return dataManagementService.createReferenceFromString(stringValue, mds,
                getStorageNetworkDestination(componentContext));
        } catch (InterruptedException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, NetworkDestination nodeId) throws IOException {
        copyReferenceToLocalFile(reference, targetFile, nodeId, true);
    }

    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, NetworkDestination nodeId,
        boolean decompress) throws IOException {
        try {
            dataManagementService.copyReferenceToLocalFile(reference, targetFile, nodeId);
        } catch (CommunicationException e) {
            throw new RuntimeException(StringUtils.format("Failed to copy data reference from remote node @%s to local file: ",
                nodeId)
                + e.getMessage(), e);
        }
    }

    @Override
    public String retrieveStringFromReference(String reference, NetworkDestination nodeId) throws IOException {
        try {
            return dataManagementService.retrieveStringFromReference(reference, nodeId);
        } catch (CommunicationException e) {
            throw new RuntimeException(StringUtils.format("Failed to retrieve string from data reference from remote node @%s: ",
                nodeId)
                + e.getMessage(), e);
        }
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
            reference = dataManagementService.createReferenceFromLocalFile(file, mds,
                getStorageNetworkDestination(componentContext));
        } catch (InterruptedException | CommunicationException e) {
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
            reference = dataManagementService.createReferenceFromLocalDirectory(dir, mds,
                getStorageNetworkDestination(componentContext));
        } catch (InterruptedException | IOException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
        return typedDatumFactory.createDirectoryReference(reference, dirname);
    }

    @Override
    public void copyFileReferenceTDToLocalFile(ComponentContext componentContext, FileReferenceTD fileReference, File targetFile)
        throws IOException {
        copyReferenceToLocalFile(fileReference.getFileReference(), targetFile, getStorageNetworkDestination(componentContext));
    }

    @Override
    public void copyReferenceTDToLocalCompressedFile(ComponentContext componentContext, TypedDatum fileReference, File targetFile)
        throws IOException {
        if (fileReference instanceof FileReferenceTD) {
            copyReferenceToLocalFile(((FileReferenceTD) fileReference).getFileReference(), targetFile,
                getStorageNetworkDestination(componentContext), false);
        } else if (fileReference instanceof DirectoryReferenceTD) {
            copyReferenceToLocalFile(((DirectoryReferenceTD) fileReference).getDirectoryReference(), targetFile,
                getStorageNetworkDestination(componentContext), false);
        }

    }

    @Override
    public void copyDirectoryReferenceTDToLocalDirectory(ComponentContext componentContext, DirectoryReferenceTD dirReference,
        File targetDir) throws IOException {
        try {
            dataManagementService.copyReferenceToLocalDirectory(dirReference.getDirectoryReference(), targetDir,
                getStorageNetworkDestination(componentContext));
        } catch (CommunicationException e) {
            throw new RuntimeException(StringUtils.format(
                "Failed to copy directory reference from remote node @%s to local directory: ",
                componentContext.getNodeId())
                + e.getMessage(), e);
        }
    }

    @Override
    public void copyDirectoryReferenceTDToLocalDirectory(DirectoryReferenceTD dirReference, File targetDir, NetworkDestination node)
        throws IOException {
        try {
            // performs a direct single-attempt RPC to the given node
            dataManagementService.copyReferenceToLocalDirectory(dirReference.getDirectoryReference(), targetDir, node);
        } catch (CommunicationException e) {
            throw new RuntimeException(StringUtils.format(
                "Failed to copy directory reference from remote node @%s to local directory: ",
                node)
                + e.getMessage(), e);
        }
    }

    @Override
    public FileReferenceTD createFileReferenceTDFromLocalCompressedFile(ComponentContext componentContext, File file, String filename)
        throws IOException {
        if (!file.exists()) {
            throw new IOException("File doesn't exist: " + file.getAbsolutePath());
        }
        String reference;
        try {
            MetaDataSet mds = new MetaDataSet();
            ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);
            reference = dataManagementService.createReferenceFromLocalFile(file, mds,
                getStorageNetworkDestination(componentContext), true);
        } catch (InterruptedException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
        return typedDatumFactory.createFileReference(reference, filename);
    }

    @Override
    public DirectoryReferenceTD createDirectoryReferenceTDFromLocalCompressedFile(ComponentContext componentContext, File dir,
        String dirname) throws IOException {
        if (!dir.exists()) {
            throw new IOException("Directory doesn't exist: " + dir.getAbsolutePath());
        }
        String reference;
        try {
            MetaDataSet mds = new MetaDataSet();
            ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);
            reference = dataManagementService.createReferenceFromLocalFile(dir, mds,
                getStorageNetworkDestination(componentContext), true);
        } catch (InterruptedException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
        return typedDatumFactory.createDirectoryReference(reference, dirname);
    }

    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumFactory = typedDatumService.getFactory();
    }

    protected void bindDataManagementService(DataManagementService newDataManagementService) {
        dataManagementService = newDataManagementService;
    }

    private NetworkDestination getStorageNetworkDestination(ComponentContext componentContext) {
        return componentContext.getStorageNetworkDestination();
    }
}
