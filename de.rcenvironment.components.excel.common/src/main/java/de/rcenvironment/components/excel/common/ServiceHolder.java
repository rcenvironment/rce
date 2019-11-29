/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;


/**
 * Holds any OSGi-Service for common classes.
 *
 * @author Markus Kunde
 */
public class ServiceHolder {

    
    private static TypedDatumSerializer serializer;

    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        serializer = newTypedDatumService.getSerializer();
    }
  
    protected void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {}
    
    protected TypedDatumSerializer getSerializer() {
        return serializer;
    }
}
