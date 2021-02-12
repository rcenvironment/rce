/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.stateful;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * A mock {@link StatefulComponentDataManagementService} that creates local temporary files and uses their absolute paths as references.
 * Useful for testing wrappers without a full data management backend.
 * 
 * @author Robert Mischke
 */
public class TempFileMockComponentDataManagementService implements StatefulComponentDataManagementService {

    private static Log log = LogFactory.getLog(TempFileMockComponentDataManagementService.class);

    private TempFileService tempFileService = TempFileServiceAccess.getInstance();

    // method does not make sense in this context, because this method implementation is only
    // intended to work locally
    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, Collection<NetworkDestination> platforms) throws IOException {
        // false = do not preserve timestamp
        FileUtils.copyFile(new File(reference), targetFile, false);
    }

    // method does not make sense in this context, because this method implementation is only
    // intended to work locally
    @Override
    public void copyReferenceToLocalFile(String reference, File targetFile, NetworkDestination platform) throws IOException {
        // false = do not preserve timestamp
        FileUtils.copyFile(new File(reference), targetFile, false);
    }

    @Override
    public String retrieveStringFromReference(String reference, InstanceNodeSessionId nodeId) throws IOException {
        return FileUtils.readFileToString(new File(reference));
    }

    @Override
    public String createTaggedReferenceFromLocalFile(File file, String filename) throws IOException {
        // copy the file to a temporary file to take an immutable snapshot
        File tmpFile = tempFileService.createTempFileFromPattern("DMMock-" + file.getName() + "-*.tmp");
        // tmpFile.deleteOnExit();
        // false = do not preserve timestamp
        FileUtils.copyFile(file, tmpFile, false);
        String reference = tmpFile.getAbsolutePath();
        if (log.isTraceEnabled()) {
            log.trace("Created reference " + reference + " for local file " + file.getAbsolutePath());
        }

        return reference;
    }

    @Override
    public String createTaggedReferenceFromString(String object) throws IOException {
        // copy the file to a temporary file to take an immutable snapshot
        File tmpFile = tempFileService.createTempFileFromPattern("DMMock-*.tmp");
        FileUtils.writeStringToFile(tmpFile, object);
        String reference = tmpFile.getAbsolutePath();
        if (log.isTraceEnabled()) {
            log.trace("Created reference " + reference + " for String");
        }

        return reference;
    }

    @Override
    public void addHistoryDataPoint(Serializable historyData, String userInfoText) throws IOException {
        // not supported, calls are silently ignored
    }

    @Override
    public FileReferenceTD createFileReferenceTDFromLocalFile(File file, String filename) throws IOException {
        // not supported, return null
        return null;
    }

    @Override
    public DirectoryReferenceTD createDirectoryReferenceTDFromLocalDirectory(File dir, String dirname) throws IOException {
        // not supported, return null
        return null;
    }

    @Override
    public void copyFileReferenceTDToLocalFile(FileReferenceTD fileReference, File targetFile, InstanceNodeSessionId node)
        throws IOException {
        // not supported, calls are silently ignored
    }

    @Override
    public void copyDirectoryReferenceTDToLocalDirectory(DirectoryReferenceTD dirReference, File targetDir, InstanceNodeSessionId node)
        throws IOException {
        // not supported, calls are silently ignored
    }

}
