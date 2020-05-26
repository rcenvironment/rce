/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context.spi;

import java.io.IOException;

import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/**
 * An Interface that abstracts away all operations that depend on the selected delegation mode
 * (strategy pattern).
 * 
 * @author Robert Mischke
 */
public interface ExecutorContext {

    /**
     * Performs context operations at the start of a session. A session is a scope for one or more
     * command line executions that may share properties and/or resources.
     * 
     * @throws IOException on general I/O errors
     * @throws ValidationFailureException on validation errors caused by properties passed to the
     *         concrete implementation
     */
    void setUpSession() throws IOException, ValidationFailureException;

    /**
     * Releases resources that were acquired during the session.
     * 
     * @throws IOException on general I/O errors
     */
    void tearDownSession() throws IOException;

    /**
     * Creates a new {@link CommandLineExecutor} that is configured to execute in a newly-created,
     * individual directory (the "sandbox").
     * 
     * @return a new executor with a clean work directory
     * @throws IOException on general I/O errors
     */
    CommandLineExecutor setUpSandboxedExecutor() throws IOException;

    /**
     * Discards the sandbox directory created in {@link #setUpSandboxedExecutor()}.
     * 
     * @param executor an executor acquired via {@link #setUpSandboxedExecutor()}
     * @throws IOException on general I/O errors
     */
    void tearDownSandbox(CommandLineExecutor executor) throws IOException;

    /**
     * Creates the path of a new temporary directory in the execution context. Its properties depend
     * on the context implementation.
     * 
     * IMPORTANT: Callers should *not* make assumptions whether the actual directory has already
     * been created or not; this behavior is implementation-dependent.
     * 
     * @param contextHint a string that may be used as part of a generated temp directory name,
     *        which is useful to recognize directories during debugging; should only contain
     *        characters that are valid for all relevant filesystems
     * @return the platform-specific full path of the generated temp directory.
     * @throws IOException on general I/O errors
     */
    String createUniqueTempDir(String contextHint) throws IOException;

}
