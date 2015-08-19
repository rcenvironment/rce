/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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

        DataBackend dataService =
            BackendSupport.getDataBackend();
        URI location =
            dataService.suggestLocation(
                UUID.fromString(binaryReferenceKey));
        dataService.delete(location);
    }
}
