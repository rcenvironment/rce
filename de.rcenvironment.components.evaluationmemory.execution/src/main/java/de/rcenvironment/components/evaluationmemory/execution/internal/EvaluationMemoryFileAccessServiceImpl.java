/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;


/**
 * Implementation of {@link EvaluationMemoryFileAccessService}.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryFileAccessServiceImpl implements EvaluationMemoryFileAccessService {
    
    private TypedDatumSerializer typedDatumSerializer;
    
    private Set<String> memoryFilesInUse = new HashSet<>();

    @Override
    public synchronized EvaluationMemoryAccess acquireAccessToMemoryFile(String memoryFilePath)
        throws IOException {

        if (memoryFilesInUse.contains(memoryFilePath)) {
            throw new IOException(StringUtils.format("Failed to give read access to memory file: '%s',"
                + " because it seems to be already in use by another 'Evaluation Memory' component", memoryFilePath));
        }
        memoryFilesInUse.add(memoryFilePath);
        EvaluationMemoryFileAccessImpl memoryAccess = new EvaluationMemoryFileAccessImpl(memoryFilePath);
        memoryAccess.setTypedDatumSerializer(typedDatumSerializer);
        return memoryAccess;
    }

    @Override
    public synchronized boolean releaseAccessToMemoryFile(String memoryFilePath) {
        return memoryFilesInUse.remove(memoryFilePath);
    }
    
    protected void bindTypedDatumService(TypedDatumService service) {
        typedDatumSerializer = service.getSerializer();
    }

}
