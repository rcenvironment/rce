/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.workflow.execution.function.OutputAdapterComponent;

public class OutputAdapterComponentTest {

    private ComponentContextMock componentContext;

    private TypedDatumSerializer serializer;

    private TypedDatumFactory typedDatumFactory;
    
    private DataManagementServiceStub dataManagementService;

    private class OutputAdapterComponentUnderTest extends OutputAdapterComponent {

        private final File temporaryFile = new File("someFilePath");

        private final File temporaryDirectory = new File("someDirectoryPath");

        private Capture<Map<String, Object>> outputMap = Capture.newInstance(CaptureType.ALL);

        @Override
        protected void writeOutputsMapToDirectory(Map<String, Object> outputsMap, File outputDirectory) throws ComponentException {
            this.outputMap.setValue(outputsMap);
        }

        private Exception processInputsAndExpectException() {
            final Capture<Exception> caughtException = Capture.newInstance(CaptureType.LAST);
            try {
                this.processInputs();
            } catch (ComponentException thrownException) {
                caughtException.setValue(thrownException);
            }

            assertTrue(caughtException.hasCaptured());
            return caughtException.getValue();
        }

        protected boolean hasWrittenOutputMap() {
            return outputMap.hasCaptured();
        }

        public Map<String, Object> getWrittenOutputMap() {
            return outputMap.getValue();
        }

        @Override
        protected File createTemporaryFile() throws IOException {
            return temporaryFile;
        }

        @Override
        protected File createTemporaryDirectory() throws IOException {
            return temporaryDirectory;
        }
    }

    @Before
    public void setup() {
        componentContext = new ComponentContextMock();

        createAndInitializeTypedDatumService();
        createAndInitializeComponentDataManagementService();
    }

    private void createAndInitializeTypedDatumService() {
        serializer = EasyMock.createMock(TypedDatumSerializer.class);

        typedDatumFactory = EasyMock.createMock(TypedDatumFactory.class);

        final TypedDatumService typedDatumService = EasyMock.createMock(TypedDatumService.class);
        EasyMock.expect(typedDatumService.getSerializer()).andStubReturn(serializer);
        EasyMock.expect(typedDatumService.getFactory()).andStubReturn(typedDatumFactory);

        EasyMock.replay(typedDatumService);

        componentContext.addService(TypedDatumService.class, typedDatumService);
    }

    private void createAndInitializeComponentDataManagementService() {
        dataManagementService = new DataManagementServiceStub();
        
        dataManagementService.bindToComponentContext(componentContext);
    }

    private void setOutputFolderPath() {
        componentContext.setConfigurationValue("outputFolder", "someDirectoryPath");
    }

    @Test
    public void startShallNotBeTreatedAsRun() {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        assertFalse(component.treatStartAsComponentRun());
    }

    @Test
    public void whenShortTextInputExistsThanThatShortTextIsWrittenToOutputMap() throws ComponentException {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        setOutputFolderPath();

        componentContext.addSimulatedInput(someInputName(), null, DataType.ShortText, false, null);
        final ShortTextTD shortText = createShortText(someText());
        componentContext.setInputValue(someInputName(), shortText);

        expectSerialization(shortText, someSerializedValue());

        component.setComponentContext(componentContext);

        component.processInputs();

        assertTrue(component.hasWrittenOutputMap());

        final Map<String, Object> outputMap = component.getWrittenOutputMap();
        assertEquals(1, outputMap.size());
        assertEquals(someSerializedValue(), outputMap.get(someInputName()));
    }

    @Test
    public void whenFileInputExistsThanThatFileIsCopiedFromDataManagement() throws ComponentException, IOException, CommunicationException {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        setOutputFolderPath();

        componentContext.addSimulatedInput(someInputName(), null, DataType.FileReference, false, null);
        final FileReferenceTD fileReference = createFileReference(someFileReference());
        componentContext.setInputValue(someInputName(), fileReference);

        final ShortTextTD fileReferenceText = createShortText("fileReferenceText");
        expectShortTextCreation(fileReferenceText);
        expectSerialization(fileReferenceText, someSerializedValue());

        component.setComponentContext(componentContext);
        
        dataManagementService.expectCopyReferenceToLocalFile();

        component.processInputs();

        assertTrue(component.hasWrittenOutputMap());

        final Map<String, Object> outputMap = component.getWrittenOutputMap();
        assertEquals(1, outputMap.size());
        assertEquals(someSerializedValue(), outputMap.get(someInputName()));

        assertEquals(someFileReference(), dataManagementService.getCapturedFileReference());

        assertEquals(component.temporaryFile, dataManagementService.getCapturedCopyTarget());
    }

    @Test
    public void whenCopyingFileReferenceFailsThenExceptionIsThrownAndNoOutputIsWritten() 
        throws ComponentException, IOException, CommunicationException {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        setOutputFolderPath();

        componentContext.addSimulatedInput(someInputName(), null, DataType.FileReference, false, null);
        final FileReferenceTD fileReference = createFileReference(someFileReference());
        componentContext.setInputValue(someInputName(), fileReference);

        final ShortTextTD fileReferenceText = createShortText("fileReferenceText");
        expectShortTextCreation(fileReferenceText);
        expectSerialization(fileReferenceText, someSerializedValue());

        component.setComponentContext(componentContext);
        
        final IOException underlyingException = new IOException("some error message");
        dataManagementService.expectCopyReferenceToLocalFileAndThrowException(underlyingException);

        final Exception caughtException = component.processInputsAndExpectException();

        assertFalse(component.hasWrittenOutputMap());
        assertNotNull(caughtException);
        assertSame(underlyingException, caughtException.getCause());
    }

    @Test
    public void whenDirectoryInputExistsThanThatFileIsCopiedFromDataManagement()
        throws ComponentException, IOException, CommunicationException {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        setOutputFolderPath();

        componentContext.addSimulatedInput(someInputName(), null, DataType.DirectoryReference, false, null);
        final DirectoryReferenceTD fileReference = createDirectoryReference(someDirectoryReference());
        componentContext.setInputValue(someInputName(), fileReference);

        final ShortTextTD fileReferenceText = createShortText("directoryReferenceText");
        expectShortTextCreation(fileReferenceText);
        expectSerialization(fileReferenceText, someSerializedValue());

        component.setComponentContext(componentContext);
        
        dataManagementService.expectCopyReferenceToLocalDirectory();

        component.processInputs();

        assertTrue(component.hasWrittenOutputMap());

        final Map<String, Object> outputMap = component.getWrittenOutputMap();
        assertEquals(1, outputMap.size());
        assertEquals(someSerializedValue(), outputMap.get(someInputName()));

        assertEquals(someDirectoryReference(), dataManagementService.getCapturedDirectoryReference());

        assertEquals(component.temporaryDirectory, dataManagementService.getCapturedCopyTarget());
    }

    @Test
    public void whenCopyingDirectoryReferenceFailsThenExceptionIsThrownAndNoOutputIsWritten() 
        throws ComponentException, IOException, CommunicationException {
        final OutputAdapterComponentUnderTest component = new OutputAdapterComponentUnderTest();

        setOutputFolderPath();

        componentContext.addSimulatedInput(someInputName(), null, DataType.DirectoryReference, false, null);
        final DirectoryReferenceTD fileReference = createDirectoryReference(someDirectoryReference());
        componentContext.setInputValue(someInputName(), fileReference);

        final ShortTextTD fileReferenceText = createShortText("directoryReferenceText");
        expectShortTextCreation(fileReferenceText);
        expectSerialization(fileReferenceText, someSerializedValue());

        component.setComponentContext(componentContext);
        
        final IOException underlyingException = new IOException("some error message");
        dataManagementService.expectCopyReferenceToLocalDirectoryAndThrowException(underlyingException);

        final Exception caughtException = component.processInputsAndExpectException();

        assertFalse(component.hasWrittenOutputMap());
        assertNotNull(caughtException);
        assertSame(underlyingException, caughtException.getCause());
    }

    private static String someText() {
        return "someText";
    }

    private static String someInputName() {
        return "someInputName";
    }

    private static String someSerializedValue() {
        return "someSerializedValue";
    }

    private static String someFileReference() {
        return "someFileReference";
    }

    private static String someDirectoryReference() {
        return "someDirectoryReference";
    }

    private void expectShortTextCreation(final ShortTextTD valueToReturn) {
        EasyMock.expect(typedDatumFactory.createShortText(EasyMock.anyString())).andStubReturn(valueToReturn);
        EasyMock.replay(typedDatumFactory);
    }

    private void expectSerialization(final TypedDatum valueToSerialize, final String serializedValue) {
        EasyMock.expect(serializer.serialize(valueToSerialize)).andStubReturn(serializedValue);
        EasyMock.replay(serializer);
    }

    private static ShortTextTD createShortText(final String content) {
        final ShortTextTD deserializedDatum = EasyMock.createMock(ShortTextTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.ShortText);
        EasyMock.expect(deserializedDatum.getShortTextValue()).andStubReturn(content);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static FileReferenceTD createFileReference(String fileReference) {
        final FileReferenceTD deserializedDatum = EasyMock.createMock(FileReferenceTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.FileReference);
        EasyMock.expect(deserializedDatum.getFileReference()).andStubReturn(fileReference);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static DirectoryReferenceTD createDirectoryReference(String directoryReference) {
        final DirectoryReferenceTD deserializedDatum = EasyMock.createMock(DirectoryReferenceTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.DirectoryReference);
        EasyMock.expect(deserializedDatum.getDirectoryReference()).andStubReturn(directoryReference);
        EasyMock.expect(deserializedDatum.getDirectoryName()).andStubReturn(directoryReference);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

}
