/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.net.URI;
import java.util.UUID;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.datamanagement.DataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link DataService}.
 * 
 * @author Juergen Klein
 */
abstract class DataServiceImpl implements DataService {

    protected static final String PASSED_USER_IS_NOT_VALID = "Passed user representation is not valid.";

    protected CommunicationService communicationService;

    protected PlatformService platformService;

    protected BundleContext context;

    @Override
    @AllowRemoteAccess
    public void deleteReference(String binaryReferenceKey) {

        // MetaDataBackendService catalogBackend = BackendSupport.getMetaDataBackend();
        //
        // DataReference catalogDataReference = catalogBackend.getDataReference(dataReference.getDataReferenceKey());
        // if (catalogDataReference == null) {
        // throw new IllegalArgumentException("Data reference not available in catalog: " + dataReference.getDataReferenceKey());
        // }
        // catalogBackend.deleteDataReference(catalogDataReference.getDataReferenceKey());
        DataBackend dataService =
            BackendSupport.getDataBackend();
        // String gzipReferenceKey = null;
        // for (BinaryReference br : dataReference.getBinaryReferences()) {
        // if (br.getCompression().equals(CompressionFormat.GZIP)) {
        // gzipReferenceKey = br.getBinaryReferenceKey();
        // }
        // }
        URI location =
            dataService.suggestLocation(
                UUID.fromString(binaryReferenceKey));
        dataService.delete(location);
    }
}
