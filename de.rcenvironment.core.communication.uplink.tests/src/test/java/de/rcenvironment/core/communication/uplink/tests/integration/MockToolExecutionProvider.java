/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.tests.integration;

import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryDownloadReceiver;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProviderEventCollector;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Test helper to simulate the execution of a published tool triggered by a remote request.
 *
 * @author Robert Mischke
 */
class MockToolExecutionProvider implements ToolExecutionProvider {

    public final ToolExecutionRequest request;

    private final List<MockFile> receivedInputFiles = new ArrayList<>();

    private List<String> receivedListOfInputSubDirectories;

    private final List<MockFile> mockOutputFiles = new ArrayList<>();

    private String mockError = null; // default = do not simulate an error

    private boolean executeCalled;

    private volatile boolean cancelCalled; // volatile as this cannot use global synchronization

    private final Log log = LogFactory.getLog(getClass());

    private boolean contextClosingCalled;

    // TODO add methods to specify the execution result and output files to simulate

    MockToolExecutionProvider(ToolExecutionRequest request) {
        this.request = request;
    }

    public void addMockOutputFile(MockFile file) {
        mockOutputFiles.add(file);
    }

    public void setMockError(String mockError) {
        this.mockError = mockError;
    }

    @Override
    public DirectoryDownloadReceiver getInputDirectoryReceiver() {
        return new DirectoryDownloadReceiver() {

            @Override
            public void receiveDirectoryListing(List<String> relativePaths) throws IOException {
                assertNull(receivedListOfInputSubDirectories); // fail on duplicate call
                receivedListOfInputSubDirectories = relativePaths;
            }

            @Override
            public void receiveFile(FileDataSource dataStream) throws IOException {
                log.debug("Simulating download of mock input file " + dataStream.getRelativePath());
                getReceivedInputFiles()
                    .add(new MockFile(dataStream.getRelativePath(), dataStream.getSize(), IOUtils.toByteArray(dataStream.getStream())));
            }
        };
    }

    @Override
    public DirectoryUploadProvider getOutputDirectoryProvider() {
        return new DirectoryUploadProvider() {

            @Override
            public List<String> provideDirectoryListing() throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void provideFiles(DirectoryUploadContext uploadContext) throws IOException {
                for (MockFile mockFile : mockOutputFiles) {
                    log.debug("Simulating upload of mock output file " + mockFile.relativePath);
                    uploadContext.provideFile(
                        new FileDataSource(mockFile.relativePath, mockFile.announcedSize, new ByteArrayInputStream(mockFile.content)));
                }
            }

        };
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionProviderEventCollector eventCollector) throws OperationFailureException {
        executeCalled = true;
        eventCollector.submitEvent("mockEventType", "mockEventData");
        final ToolExecutionResult mockResult = new ToolExecutionResult();
        mockResult.successful = true; // mock result
        mockResult.cancelled = false; // mock result
        return mockResult;
    }

    @Override
    public void requestCancel() {
        cancelCalled = true;
    }

    @Override
    public void onContextClosing() {
        contextClosingCalled = true;
    }

    public List<MockFile> getReceivedInputFiles() {
        return receivedInputFiles;
    }

    public List<String> getReceivedListOfInputSubDirectories() {
        return receivedListOfInputSubDirectories;
    }

    public boolean wasExecuteCalled() {
        return executeCalled;
    }

    public boolean wasCancelCalled() {
        return cancelCalled;
    }

    public boolean wasContextClosingCalled() {
        return contextClosingCalled;
    }
}
