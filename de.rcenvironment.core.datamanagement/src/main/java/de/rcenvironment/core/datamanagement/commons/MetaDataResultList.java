/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;


/**
 * Container for an ordered set of results yielded by a
 * {@link de.rcenvironment.core.datamanagement.QueryService#executeMetaDataQuery(
 * de.rcenvironment.rce.authentication.ProxyCertificate,
 * de.rcenvironment.rce.datamanagement.commons.Query, Integer, Integer)}.
 * 
 * @author Christian Weiss
 */
public class MetaDataResultList implements Serializable, Iterable<MetaDataResult> {

    private static final long serialVersionUID = 6743652931478165820L;

    private final List<MetaDataResult> results = new LinkedList<MetaDataResult>();

    /**
     * Appends a {@link MetaDataResult} to the list.
     * 
     * @param result {@link MetaDataResult} to append
     */
    public void add(final MetaDataResult result) {
        results.add(result);
    }

    /**
     * Returns the {@link MetaDataResult} with the given ID.
     * 
     * @param id The ID of the desired {@link MetaDataResult}.
     * @return The {@link MetaDataResult} with the given ID.
     */
    public MetaDataResult getResultById(final UUID id) {
        for (final MetaDataResult result : this) {
            if (result.getId().equals(id)) {
                return result;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * The number of {@link MetaDataResult}s in the list.
     * 
     * @return the number of {@link MetaDataResult}
     */
    public int size() {
        return results.size();
    }

    @Override
    public Iterator<MetaDataResult> iterator() {
        return results.iterator();
    }
    
}
