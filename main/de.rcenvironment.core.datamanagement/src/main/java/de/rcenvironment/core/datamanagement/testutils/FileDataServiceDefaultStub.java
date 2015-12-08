/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
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
}
