/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

/**
 * Test cases for {@link EvaluationMemoryFileAccessServiceImpl}.
 * 
 * @author Doreen Seider
 *
 */
public class EvaluationMemoryFileAccessHandlerServiceImplTest {

    /**
     * Tests exclusive access to file.
     * @throws IOException on unexpected failure
     */
    @Test
    public void testExclusiveAccess() throws IOException {
        EvaluationMemoryFileAccessServiceImpl service = new EvaluationMemoryFileAccessServiceImpl();
        String someFilePath = "some_file_path";
        service.acquireAccessToMemoryFile(someFilePath);
        try {
            service.acquireAccessToMemoryFile(someFilePath);
            fail("Exception expected as file already in use");
        } catch (IOException e) {
            assertTrue(true);
        }
        
        String someOtherFilePath = "some_other_file_path";
        service.acquireAccessToMemoryFile(someOtherFilePath);
        
        service.releaseAccessToMemoryFile(someFilePath);
        service.acquireAccessToMemoryFile(someFilePath);
        
        service.releaseAccessToMemoryFile(someFilePath);
        service.releaseAccessToMemoryFile(someFilePath);
    }

}
