/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.io.IOException;

/**
 * Creates {@link EvaluationMemoryFileAccess} instances.
 * 
 * @author Doreen Seider
 */
public interface EvaluationMemoryFileAccessService {

    /**
     * Gives access to a memory file by creating a {@link EvaluationMemoryFileAccess} instance of no instance related to the given file path
     * exists.
     * 
     * @param memoryFilePath path the the memory file to acquire access
     * @return instance of {@link EvaluationMemoryFileAccess} related to the given file
     * @throws IOException if memory file given is already in use
     */
    EvaluationMemoryAccess acquireAccessToMemoryFile(String memoryFilePath) throws IOException;

    /**
     * Releases access to a memory file.
     * 
     * @param memoryFilePath path the the memory file to release access
     * @return <code>true</code> if the the given memory file was actually locked because accessed before, otherwise <code>false</code>
     */
    boolean releaseAccessToMemoryFile(String memoryFilePath);

}
