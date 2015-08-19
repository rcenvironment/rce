/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.testutils;

import java.io.IOException;
import java.io.InputStream;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;

/**
 * Default stub for {@link FileDataService}. All methods with a return value respond with the default field value for this type (null, 0,
 * false, '\u0000', ...).
 * 
 * This class (and subclasses of it) is intended for test scenarios where an instance of {@link FileDataService} is required, but where the
 * exact calls to this instance are not relevant. If they are relevant and should be tested, create a mock instance instead (for example,
 * with the EasyMock library).
 * 
 * @author Robert Mischke
 */
public class FileDataServiceDefaultStub implements FileDataService {

    @Override
    public void deleteReference(String binaryReferenceKey) {}

    @Override
    public InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
        throws AuthorizationException {
        return null;
    }

    @Override
    public DataReference newReferenceFromStream(User proxyCertificate, InputStream inputStream, MetaDataSet metaDataSet)
        throws AuthorizationException {
        return null;
    }

    @Override
    public String initializeUpload(User user) throws IOException {
        return null;
    }

    @Override
    public long appendToUpload(User user, String id, byte[] data) throws IOException {
        return 0;
    }

    @Override
    public void finishUpload(User user, String id, MetaDataSet metaDataSet) throws IOException {}

    @Override
    public DataReference pollUploadForDataReference(User user, String id) {
        return null;
    }

}
