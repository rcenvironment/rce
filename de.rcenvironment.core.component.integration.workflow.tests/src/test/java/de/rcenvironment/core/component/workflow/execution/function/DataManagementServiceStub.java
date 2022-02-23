/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.function;

import java.io.File;
import java.io.IOException;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.datamanagement.DataManagementService;

public class DataManagementServiceStub {

    private DataManagementService dataManagementService = EasyMock.createMock(DataManagementService.class);

    private final Capture<String> copiedFileReference = Capture.newInstance(CaptureType.FIRST);

    private final Capture<String> copiedDirectoryReference = Capture.newInstance(CaptureType.FIRST);

    private final Capture<File> copyTarget = Capture.newInstance(CaptureType.FIRST);

    public void bindToComponentContext(final ComponentContextMock context) {
        context.addService(DataManagementService.class, this.dataManagementService);
    }

    public void expectCopyReferenceToLocalFile() {
        try {
            dataManagementService.copyReferenceToLocalFile(EasyMock.capture(copiedFileReference),
                EasyMock.capture(copyTarget), (NetworkDestination) EasyMock.anyObject(NetworkDestination.class));
        } catch (IOException | CommunicationException e) {
            // Will never occur since we only call the potentially throwing method on a mock object
        }
        EasyMock.expectLastCall();
        EasyMock.replay(dataManagementService);
    }

    public void expectCopyReferenceToLocalFileAndThrowException(Throwable underlyingException) {
        try {
            dataManagementService.copyReferenceToLocalFile(EasyMock.capture(copiedFileReference),
                EasyMock.capture(copyTarget), (NetworkDestination) EasyMock.anyObject(NetworkDestination.class));
        } catch (IOException | CommunicationException e) {
            // Will never occur since we only call the potentially throwing method on a mock object
        }
        EasyMock.expectLastCall().andStubThrow(underlyingException);
        EasyMock.replay(dataManagementService);
    }

    public void expectCopyReferenceToLocalDirectory() {
        try {
            dataManagementService.copyReferenceToLocalDirectory(EasyMock.capture(copiedDirectoryReference),
                EasyMock.capture(copyTarget), (NetworkDestination) EasyMock.anyObject(NetworkDestination.class));
        } catch (IOException | CommunicationException e) {
            // Will never occur since we only call the potentially throwing method on a mock object
        }
        EasyMock.expectLastCall();
        EasyMock.replay(dataManagementService);
    }

    public void expectCopyReferenceToLocalDirectoryAndThrowException(Throwable underlyingException) {
        try {
            dataManagementService.copyReferenceToLocalDirectory(EasyMock.capture(copiedFileReference),
                EasyMock.capture(copyTarget), (NetworkDestination) EasyMock.anyObject(NetworkDestination.class));
        } catch (IOException | CommunicationException e) {
            // Will never occur since we only call the potentially throwing method on a mock object
        }
        EasyMock.expectLastCall().andStubThrow(underlyingException);
        EasyMock.replay(dataManagementService);
    }

    public String getCapturedFileReference() {
        return this.copiedFileReference.getValue();
    }

    public String getCapturedDirectoryReference() {
        return this.copiedDirectoryReference.getValue();
    }

    public File getCapturedCopyTarget() {
        return this.copyTarget.getValue();
    }
}
