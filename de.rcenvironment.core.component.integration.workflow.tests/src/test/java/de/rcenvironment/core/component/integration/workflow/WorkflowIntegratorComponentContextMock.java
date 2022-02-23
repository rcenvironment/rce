/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.workflow;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * @author Alexander Weinert
 */
class WorkflowIntegratorComponentContextMock extends ComponentContextMock {

    /**
     * Generated, do not touch.
     */
    private static final long serialVersionUID = -3904256671297331171L;

    public void setEndpointAdapterConfiguration(String... endpointAdapterConfigurations) {
        this.setConfigurationValue("endpointAdapters", "[" + String.join(",", endpointAdapterConfigurations) + "]");
    }

    public void addFileInput(String inputName, String fileReference) {
        this.addSimulatedInput(inputName, "someEndpointdId", DataType.FileReference, false, null);

        final FileReferenceTD inputValue = fileReferenceMock(fileReference);
        this.setInputValue("someExternalName", inputValue);
    }

    public void addDirectoryInput(String inputName, String directoryName,
        String directoryReference) {
        this.addSimulatedInput(inputName, "someEndpointId", DataType.DirectoryReference, false, null);

        final DirectoryReferenceTD inputValue = directoryReferenceMock(directoryName, directoryReference);
        this.setInputValue(inputName, inputValue);
    }

    private static FileReferenceTD fileReferenceMock(String fileReference) {
        final FileReferenceTD returnValue = EasyMock.createMock(FileReferenceTD.class);
        EasyMock.expect(returnValue.getFileReference()).andStubReturn(fileReference);
        EasyMock.replay(returnValue);
        return returnValue;
    }

    private static DirectoryReferenceTD directoryReferenceMock(String name, String reference) {
        final DirectoryReferenceTD directoryReference = EasyMock.createMock(DirectoryReferenceTD.class);
        EasyMock.expect(directoryReference.getDirectoryReference()).andStubReturn(reference);
        EasyMock.expect(directoryReference.getDirectoryName()).andStubReturn(name);
        EasyMock.replay(directoryReference);
        return directoryReference;
    }

}
