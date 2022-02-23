/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.workflow.execution.function.InputAdapterComponent;

/**
 * @author Alexander Weinert
 */
public class InputAdapterComponentTest {

    private TypedDatumSerializer deserializer;

    private ComponentContextMock componentContext;

    private ComponentDataManagementService componentDataManagementService;

    private class InputAdapterComponentUnderTest extends InputAdapterComponent {

        private final Capture<String> parsedInputDirectories = Capture.newInstance(CaptureType.ALL);

        private Map<String, String> inputMap = new HashMap<>();

        @Override
        protected Map<String, String> readInputMapFromInputDirectory(String inputDirectoryPath) throws ComponentException {
            parsedInputDirectories.setValue(inputDirectoryPath);

            return inputMap;
        }

        protected void addInputValue(String outputName, String serializedValue) {
            this.inputMap.put(outputName, serializedValue);
        }

        private Exception startAndExpectException() {
            final Capture<Exception> caughtException = Capture.newInstance(CaptureType.LAST);
            try {
                this.start();
            } catch (ComponentException thrownException) {
                caughtException.setValue(thrownException);
            }
            
            assertTrue(caughtException.hasCaptured());
            return caughtException.getValue();
        }

    }

    @Before
    public void setup() {
        componentContext = new ComponentContextMock();

        createAndInitializeTypedDatumDeserializer();
        createAndInitializeComponentDataManagementService();
    }

    private void createAndInitializeTypedDatumDeserializer() {
        deserializer = EasyMock.createMock(TypedDatumSerializer.class);

        final TypedDatumService typedDatumService = EasyMock.createMock(TypedDatumService.class);
        EasyMock.expect(typedDatumService.getSerializer()).andStubReturn(deserializer);
        EasyMock.replay(typedDatumService);

        componentContext.addService(TypedDatumService.class, typedDatumService);
    }

    private void createAndInitializeComponentDataManagementService() {
        componentDataManagementService = EasyMock.createMock(ComponentDataManagementService.class);

        componentContext.addService(ComponentDataManagementService.class, componentDataManagementService);
    }

    @Test
    public void whenNoOutputsExistThenComponentDoesNotRunAtStart() throws ComponentException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        final ComponentContext context = EasyMock.createMock(ComponentContext.class);
        EasyMock.expect(context.getOutputs()).andStubReturn(new HashSet<>());
        EasyMock.replay(context);

        component.setComponentContext(context);

        assertFalse(component.treatStartAsComponentRun());
    }

    @Test
    public void whenOutputsExistThenComponentRunsAtStart() throws ComponentException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        final ComponentContext context = EasyMock.createMock(ComponentContext.class);
        EasyMock.expect(context.getOutputs()).andStubReturn(Collections.singleton("someOutput"));
        EasyMock.replay(context);

        component.setComponentContext(context);

        assertTrue(component.treatStartAsComponentRun());
    }

    @Test
    public void whenInputMapIsEmptyThenNoOutputsAreWritten() throws ComponentException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        setInputDirectoryInComponentContext(someInputFolder());
        
        final TypedDatumService typedDatumService = EasyMock.createMock(TypedDatumService.class);
        EasyMock.expect(typedDatumService.getSerializer()).andStubReturn(null);
        componentContext.addService(TypedDatumService.class, typedDatumService);

        EasyMock.replay(typedDatumService);

        component.setComponentContext(componentContext);

        component.start();

        /*
         * Merely reaching this point without having thrown an exception suffices to verify that no outputs were written. Calling
         * context.writeOutput would trigger an AssertionError in componentContext.
         */
    }

    private void setInputDirectoryInComponentContext(String path) {
        componentContext.setConfigurationValue("inputFolder", path);
    }

    @Test
    public void whenInputMapContainsPrimitiveValueThenItIsWrittenToOutput() throws ComponentException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        component.addInputValue(someOutputName(), someSerializedValue());
        setInputDirectoryInComponentContext(someInputFolder());

        final ShortTextTD deserializedText = createEmptyShortText();
        expectShortTextDeserialization(someSerializedValue(), deserializedText);

        componentContext.addSimulatedOutput(someOutputName(), null, DataType.ShortText, false, null);

        component.setComponentContext(componentContext);
        component.start();

        List<TypedDatum> writtenOutputs = componentContext.getCapturedOutput(someOutputName());
        assertEquals(1, writtenOutputs.size());
        assertTrue(writtenOutputs.contains(deserializedText));
    }

    @Test
    public void whenInputMapContainsFileThenThatFileIsLoadedIntoDataManagement() throws ComponentException, IOException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        component.addInputValue(someOutputName(), someSerializedValue());
        setInputDirectoryInComponentContext(someInputFolder());

        expectShortTextDeserialization(someSerializedValue(), createShortText(someFilePath()));

        componentContext.addSimulatedOutput(someOutputName(), null, DataType.FileReference, false, null);

        final FileReferenceTD fileReference = expectLoadingFileIntoDataManagement(someFilePath(), createFileReference());

        component.setComponentContext(componentContext);
        component.start();

        List<TypedDatum> writtenOutputs = componentContext.getCapturedOutput(someOutputName());
        assertEquals(1, writtenOutputs.size());
        assertTrue(writtenOutputs.contains(fileReference));
    }

    @Test
    public void whenLoadingFileIntoDataManagementFailsThenExceptionIsThrown() throws ComponentException, IOException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        component.addInputValue(someOutputName(), someSerializedValue());
        setInputDirectoryInComponentContext(someInputFolder());

        expectShortTextDeserialization(someSerializedValue(), createShortText(someFilePath()));

        componentContext.addSimulatedOutput(someOutputName(), null, DataType.FileReference, false, null);

        final Exception reason = expectLoadingFileIntoDataManagementAndThrow(someFilePath(), "some error message");

        component.setComponentContext(componentContext);

        final Exception caughtException = component.startAndExpectException();

        assertEquals(reason, caughtException.getCause());
    }

    @Test
    public void whenInputMapContainsDirectoryThenThatDirectoryIsLoadedIntoDataManagement() throws ComponentException, IOException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        component.addInputValue(someOutputName(), someSerializedValue());
        setInputDirectoryInComponentContext(someInputFolder());

        expectShortTextDeserialization(someSerializedValue(), createShortText(someFilePath()));

        componentContext.addSimulatedOutput(someOutputName(), null, DataType.DirectoryReference, false, null);

        final DirectoryReferenceTD directoryReference = createDirectoryReference();
        expectLoadingDirectoryIntoDataManagement(someFilePath(), directoryReference);

        component.setComponentContext(componentContext);

        component.start();

        List<TypedDatum> writtenOutputs = componentContext.getCapturedOutput(someOutputName());
        assertEquals(1, writtenOutputs.size());
        assertTrue(writtenOutputs.contains(directoryReference));
    }

    @Test
    public void whenLoadingDirectoryIntoDataManagementFailsThenExceptionIsThrown() throws ComponentException, IOException {
        final InputAdapterComponentUnderTest component = new InputAdapterComponentUnderTest();

        component.addInputValue(someOutputName(), someSerializedValue());
        setInputDirectoryInComponentContext(someInputFolder());

        expectShortTextDeserialization(someSerializedValue(), createShortText(someFilePath()));

        componentContext.addSimulatedOutput(someOutputName(), null, DataType.DirectoryReference, false, null);

        final Exception expectedCause = expectLoadingDirectoryIntoDataManagementAndThrow(someFilePath(), "some error message");

        component.setComponentContext(componentContext);

        final Exception caughtException = component.startAndExpectException();

        assertEquals(expectedCause, caughtException.getCause());
    }

    private void expectShortTextDeserialization(String serializedValue, ShortTextTD typedDatumToReturn) {
        expectDeserialization(serializedValue, typedDatumToReturn);
        EasyMock.replay(deserializer);
    }

    private TypedDatum expectDeserialization(String serializedValue, TypedDatum deserializedDatum) {
        EasyMock.expect(deserializer.deserialize(serializedValue)).andStubReturn(deserializedDatum);

        return deserializedDatum;
    }

    private FileReferenceTD expectLoadingFileIntoDataManagement(String fileNameParam, FileReferenceTD fileReferenceToReturn) {
        final ComponentContextMock context = EasyMock.eq(componentContext);
        final File file = EasyMock.anyObject();
        final String fileName = EasyMock.eq(fileNameParam);

        try {
            EasyMock.expect(componentDataManagementService.createFileReferenceTDFromLocalFile(context, file, fileName))
                .andStubReturn(fileReferenceToReturn);
            EasyMock.replay(componentDataManagementService);
        } catch (IOException e) {
            // Does not occur since we only call the potentially throwing method on a mock 
        }

        return fileReferenceToReturn;
    }

    private Exception expectLoadingFileIntoDataManagementAndThrow(String directoryNameParam, String errorMessage) {
        final Exception reason = new IOException(errorMessage);

        final ComponentContextMock contextMatcher = EasyMock.eq(componentContext);
        final File fileMatcher = EasyMock.anyObject();
        final String directoryName = EasyMock.eq(directoryNameParam);
    
        try {
            EasyMock.expect(componentDataManagementService.createFileReferenceTDFromLocalFile(contextMatcher, fileMatcher, directoryName))
                .andThrow(reason);
            EasyMock.replay(componentDataManagementService);
        } catch (IOException e) {
            // Does not occur since we only call the potentially throwing method on a mock 
        }

        return reason;
    }

    private void expectLoadingDirectoryIntoDataManagement(String directoryName, final DirectoryReferenceTD directoryReferenceToReturn) {
        final ComponentContextMock context = EasyMock.eq(componentContext);
        final File file = EasyMock.anyObject();
        final String fileName = EasyMock.eq(directoryName);

        try {
            EasyMock.expect(componentDataManagementService.createDirectoryReferenceTDFromLocalDirectory(context, file, fileName))
                .andStubReturn(directoryReferenceToReturn);
            EasyMock.replay(componentDataManagementService);
        } catch (IOException e) {
            // Does not occur since we only call the potentially throwing method on a mock 
        }
    }

    private Exception expectLoadingDirectoryIntoDataManagementAndThrow(String directoryNameParam, String errorMessage) {
        final Exception reason = new IOException(errorMessage);

        final ComponentContextMock contextMatcher = EasyMock.eq(componentContext);
        final File fileMatcher = EasyMock.anyObject();
        final String directoryName = EasyMock.eq(directoryNameParam);

        try {
            EasyMock.expect(
                componentDataManagementService.createDirectoryReferenceTDFromLocalDirectory(contextMatcher, fileMatcher, directoryName))
                .andThrow(reason);
            EasyMock.replay(componentDataManagementService);
        } catch (IOException e) {
            // Does not occur since we only call the potentially throwing method on a mock 
        }

        return reason;
    }

    private static ShortTextTD createEmptyShortText() {
        final ShortTextTD deserializedDatum = EasyMock.createMock(ShortTextTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.ShortText);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static ShortTextTD createShortText(final String content) {
        final ShortTextTD deserializedDatum = EasyMock.createMock(ShortTextTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.ShortText);
        EasyMock.expect(deserializedDatum.getShortTextValue()).andStubReturn(content);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static FileReferenceTD createFileReference() {
        final FileReferenceTD deserializedDatum = EasyMock.createMock(FileReferenceTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.FileReference);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static DirectoryReferenceTD createDirectoryReference() {
        final DirectoryReferenceTD deserializedDatum = EasyMock.createMock(DirectoryReferenceTD.class);
        EasyMock.expect(deserializedDatum.getDataType()).andStubReturn(DataType.DirectoryReference);
        EasyMock.replay(deserializedDatum);
        return deserializedDatum;
    }

    private static String someInputFolder() {
        return "someInputFolder";
    }

    private static String someOutputName() {
        return "someInternalOutput";
    }

    private static String someSerializedValue() {
        return "someInputValue";
    }

    private static String someFilePath() {
        return "someFilePath";
    }
}
