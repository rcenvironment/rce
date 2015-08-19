/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.datamanagement.DataService;
import de.rcenvironment.core.datamanagement.DistributedDataService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;

/**
 * Implementation of the {@link DistributedDataService}.
 * 
 * @author Doreen Seider
 */
abstract class DistributedDataServiceImpl implements DistributedDataService {

    protected CommunicationService communicationService;

    protected BundleContext context;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void deleteReference(DataReference dataReference) throws AuthorizationException {

        DataService dataService = (DataService) communicationService.getService(DataService.class,
            dataReference.getNodeIdentifier(), context);
        try {
            for (BinaryReference br : dataReference.getBinaryReferences()) {
                dataService.deleteReference(br.getBinaryReferenceKey());
            }
        } catch (RuntimeException e) {
            log.warn("Failed to delete reference on platform: " + dataReference.getNodeIdentifier(), e);
        }
    }
}
