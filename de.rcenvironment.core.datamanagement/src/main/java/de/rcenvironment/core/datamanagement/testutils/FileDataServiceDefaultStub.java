/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.testutils;

import java.io.IOException;
import java.io.InputStream;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.datamanagement.RemotableFileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default stub for {@link RemotableFileDataService}. All methods with a return value respond with the default field value for this type
 * (null, 0, false, '\u0000', ...).
 * 
 * This class (and subclasses of it) is intended for test scenarios where an instance of {@link RemotableFileDataService} is required, but
 * where the exact calls to this instance are not relevant. If they are relevant and should be tested, create a mock instance instead (for
 * example, with the EasyMock library).
 * 
 * @author Robert Mischke
 */
public class FileDataServiceDefaultStub implements RemotableFileDataService {

    @Override
    public void deleteReference(String binaryReferenceKey) {}

    @Override
    public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
        throws AuthorizationException {
        return null;
    }

    @Override
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet)
        throws AuthorizationException {
        return null;
    }

    @Override
    public String initializeUpload() throws IOException {
        return null;
    }

    @Override
    public long appendToUpload(String id, byte[] data) throws IOException {
        return 0;
    }

    @Override
    public void finishUpload(String id, MetaDataSet metaDataSet) throws IOException {}

    @Override
    public DataReference pollUploadForDataReference(String id) {
        return null;
    }

    @Override
    public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet) throws IOException {
        return null;
    }

    @Override
    public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote, Boolean decompress)
        throws RemoteOperationException {
        return null;
    }

    @Override
    public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, Boolean alreadyCompressed)
        throws RemoteOperationException {
        return null;
    }

    @Override
    public void finishUpload(String id, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException, RemoteOperationException {
        
    }

    @Override
    public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException,
        RemoteOperationException {
        return null;
    }
}
