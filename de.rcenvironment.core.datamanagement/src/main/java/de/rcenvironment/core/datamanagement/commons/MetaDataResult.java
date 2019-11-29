/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.UUID;


/**
 * Container for a single result entity yielded by a
 * {@link de.rcenvironment.core.datamanagement.QueryService#executeMetaDataQuery(
 * de.rcenvironment.rce.authentication.ProxyCertificate,
 * de.rcenvironment.rce.datamanagement.commons.Query, Integer, Integer)}.
 * 
 * @author Christian Weiss
 */
public class MetaDataResult implements Serializable {

    private static final long serialVersionUID = -4009218252476906957L;

    private final UUID id;

    private final MetaDataSet metaDataSet;

    public MetaDataResult(final UUID id, final MetaDataSet metaDataSet) {
        this.id = id;
        this.metaDataSet = metaDataSet;
    }

    public UUID getId() {
        return id;
    }

    public MetaDataSet getMetaDataSet() {
        return metaDataSet;
    }

}
