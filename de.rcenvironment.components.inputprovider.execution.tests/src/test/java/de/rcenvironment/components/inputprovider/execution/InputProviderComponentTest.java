/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * 
 * Integration test for {@link InputProviderComponent}.
 * 
 * @author Marc Stammerjohann
 */
public class InputProviderComponentTest {

    private static final String WORKFLOWSTART_PLACEHOLDER = "${%s}";

    private static final String WORKFLOW_START = "WorkflowStart";

    private static final String EMPTY_VARIABLE = "Empty Variable";

    private static final String SHORTTEXT_VARIABLE = "Short Text";

    private static final String FLOAT_VARIABLE = "Float";

    private static final String BOOLEAN_VARIABLE = "Boolean";

    private static final String INT_VARIABLE = "Integer";

    private static final String FILE_VARIABLE = "newFile";

    private static final String DIR_VARIABLE = "newDir";

    private static final String OUTPUT_FILENAME = "outputFile.file";

    private static final String OUTPUT_DIR = "rceDir";

    /**
     * Expected exception if data type is not supported.
     */
    @Rule
    public ExpectedException dataException = ExpectedException.none();

    private ComponentTestWrapper component;

    private ComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    private TempFileService tempFileService;

    private ComponentDataManagementService componentDataManagementServiceMock;

    /**
     * 
     * Set up input provider test.
     * 
     * @throws Exception e
     */
    @Before
    public void setUp() throws Exception {
        context = new ComponentContextMock();
        component = new ComponentTestWrapper(new InputProviderComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

        // Setup root directory for testing
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();

        componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);

        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();
    }

    /**
     * @throws ComponentException ce
     *
     */
    @Test
    public void testWorkflowStartConfiguration() throws ComponentException {
        String booleanValue = "false";
        String placeholder = StringUtils.format(WORKFLOWSTART_PLACEHOLDER, WORKFLOW_START);
        context.addSimulatedOutput(WORKFLOW_START, "", DataType.Boolean, false,
            generateInputProviderMetadata(placeholder));
        context.setConfigurationValue(WORKFLOW_START, booleanValue);
        component.start();
        assertEquals(typedDatumFactory.createBoolean(Boolean.parseBoolean(booleanValue)), context.getCapturedOutput(WORKFLOW_START).get(0));
    }

    /**
     * @throws ComponentException ce
     *
     */
    @Test
    public void testNotSupportedDataType() throws ComponentException {
        DataType type = DataType.Empty;
        context.addSimulatedOutput(EMPTY_VARIABLE, "", type, false, null);
        dataException.expect(ComponentException.class);
        dataException.expectMessage("Given data type is not supported: " + type);
        component.start();
    }

    /**
     * Test with three outputs.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testMultipleOutputs() throws ComponentException {
        String booleanValue = "true";
        String shorttext = "RCE Test";
        String floatValue = "7.6";

        context.addSimulatedOutput(BOOLEAN_VARIABLE, "", DataType.Boolean, false, generateInputProviderMetadata(booleanValue));
        context.addSimulatedOutput(SHORTTEXT_VARIABLE, "", DataType.ShortText, false, generateInputProviderMetadata(shorttext));
        context.addSimulatedOutput(FLOAT_VARIABLE, "", DataType.Float, false, generateInputProviderMetadata(floatValue));
        component.start();
        assertEquals(typedDatumFactory.createBoolean(Boolean.parseBoolean(booleanValue)), context.getCapturedOutput(BOOLEAN_VARIABLE)
            .get(0));
        assertEquals(typedDatumFactory.createShortText(shorttext), context.getCapturedOutput(SHORTTEXT_VARIABLE)
            .get(0));
        assertEquals(typedDatumFactory.createFloat(Double.parseDouble(floatValue)), context.getCapturedOutput(FLOAT_VARIABLE)
            .get(0));
    }

    /**
     * Test with one output.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testShortTextOutput() throws ComponentException {
        String value = "RCE Next Generation";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(SHORTTEXT_VARIABLE, "", DataType.ShortText, false, metaData);
        component.start();
        assertEquals(typedDatumFactory.createShortText(value), context.getCapturedOutput(SHORTTEXT_VARIABLE).get(0));
    }

    /**
     * Test with one output.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testBooleanOutput() throws ComponentException {
        String value = "false";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(BOOLEAN_VARIABLE, "", DataType.Boolean, false, metaData);
        component.start();
        assertEquals(typedDatumFactory.createBoolean(Boolean.parseBoolean(value)), context.getCapturedOutput(BOOLEAN_VARIABLE).get(0));
    }

    /**
     * Test with one output.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testFloatOutput() throws ComponentException {
        String value = "5.0";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(FLOAT_VARIABLE, "", DataType.Float, false, metaData);
        component.start();
        assertEquals(typedDatumFactory.createFloat(Double.parseDouble(value)), context.getCapturedOutput(FLOAT_VARIABLE).get(0));
    }

    /**
     * Test with one output.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testIntegerOutput() throws ComponentException {
        String value = "10";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(INT_VARIABLE, "", DataType.Integer, false, metaData);
        component.start();
        assertEquals(typedDatumFactory.createInteger(Integer.parseInt(value)), context.getCapturedOutput(INT_VARIABLE).get(0));
    }

    /**
     * Test with one output.
     *
     * @throws ComponentException ce
     * @throws IOException ioe
     */
    @Test
    public void testFileOutput() throws ComponentException, IOException {
        File createTempDir = createTempDir();
        File testFile = createAndVerifyFile(createTempDir, OUTPUT_FILENAME);
        FileReferenceTD dummyFileReference = typedDatumFactory.createFileReference(testFile.getAbsolutePath(), OUTPUT_FILENAME);

        EasyMock.reset(componentDataManagementServiceMock);
        EasyMock.expect(componentDataManagementServiceMock.createFileReferenceTDFromLocalFile(context, testFile, testFile.getName()))
            .andReturn(dummyFileReference);
        EasyMock.replay(componentDataManagementServiceMock);

        Map<String, String> metaData = generateInputProviderMetadata(testFile.getAbsolutePath());
        context.addSimulatedOutput(FILE_VARIABLE, "", DataType.FileReference, false, metaData);
        component.start();
        assertEquals(dummyFileReference, context.getCapturedOutput(FILE_VARIABLE).get(0));
        removeTempDirOrFile(createTempDir);
    }

    /**
     * Test with one output with an empty value.
     *
     * @throws ComponentException ce
     */
    @Test
    public void testFileIsNull() throws ComponentException {
        String value = "";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(FILE_VARIABLE, "", DataType.FileReference, false, metaData);
        dataException.expect(ComponentException.class);
        dataException.expectMessage(StringUtils.format("Internal error: No file given for output '%s'", FILE_VARIABLE));
        component.start();
    }

    /**
     * Test with one output with an not existing file value.
     *
     * @throws ComponentException ce
     * @throws IOException on unexpected error
     */
    @Test
    public void testFileNotExist() throws ComponentException, IOException {
        File emptyTempDir = tempFileService.createManagedTempDir();
        Map<String, String> metaData = generatedMetaDataForNonExistentFileOrDir(emptyTempDir);
        context.addSimulatedOutput(FILE_VARIABLE, "", DataType.FileReference, false, metaData);
        dataException.expect(ComponentException.class);
        component.start();
        removeTempDirOrFile(emptyTempDir);
    }

    private Map<String, String> generatedMetaDataForNonExistentFileOrDir(File emptyTempDir) {
        String value = new File(emptyTempDir, "not_existent").getAbsolutePath();
        Map<String, String> metaData = generateInputProviderMetadata(value);
        return metaData;
    }

    /**
     * Test with one output.
     *
     * @throws ComponentException ce
     * @throws IOException ioe
     */
    @Test
    public void testDirOutput() throws ComponentException, IOException {
        File createTempDir = createTempDir(OUTPUT_DIR);
        DirectoryReferenceTD createDirectoryReference =
            typedDatumFactory.createDirectoryReference(createTempDir.getAbsolutePath(), OUTPUT_DIR);

        EasyMock.reset(componentDataManagementServiceMock);
        EasyMock.expect(componentDataManagementServiceMock.createDirectoryReferenceTDFromLocalDirectory(context, createTempDir,
                    createTempDir.getName())).andReturn(createDirectoryReference);
        EasyMock.replay(componentDataManagementServiceMock);

        Map<String, String> metaData = generateInputProviderMetadata(createTempDir.getAbsolutePath());
        context.addSimulatedOutput(DIR_VARIABLE, "", DataType.DirectoryReference, false, metaData);
        component.start();
        assertEquals(createDirectoryReference, context.getCapturedOutput(DIR_VARIABLE).get(0));
        removeTempDirOrFile(createTempDir);
    }

    /**
     * Test with one output with an empty value.
     *
     * @throws ComponentException ce
     */
    @Test
    public void testDirIsNull() throws ComponentException {
        String value = "";
        Map<String, String> metaData = generateInputProviderMetadata(value);
        context.addSimulatedOutput(DIR_VARIABLE, "", DataType.DirectoryReference, false, metaData);
        dataException.expect(ComponentException.class);
        component.start();
    }

    /**
     * Test with one output with an not existing file value.
     *
     * @throws ComponentException ce
     * @throws IOException on unexpected error
     */
    @Test
    public void testDirNotExist() throws ComponentException, IOException {
        File emptyTempDir = tempFileService.createManagedTempDir();
        Map<String, String> metaData = generatedMetaDataForNonExistentFileOrDir(emptyTempDir);
        context.addSimulatedOutput(DIR_VARIABLE, "", DataType.DirectoryReference, false, metaData);
        dataException.expect(ComponentException.class);
        component.start();
        
        removeTempDirOrFile(emptyTempDir);
    }

    /**
     * Test with one output. Simulated output from type directory, but it is actually a file.
     *
     * @throws ComponentException ce
     * @throws IOException ioe
     */
    @Test
    public void testDirOutputWithFile() throws ComponentException, IOException {
        File createTempDir = createTempDir();
        File testFile = createAndVerifyFile(createTempDir, OUTPUT_FILENAME);
        FileReferenceTD dummyFileReference = typedDatumFactory.createFileReference(testFile.getAbsolutePath(), OUTPUT_FILENAME);

        EasyMock.reset(componentDataManagementServiceMock);
        EasyMock.expect(componentDataManagementServiceMock.createFileReferenceTDFromLocalFile(context, testFile, testFile.getName()))
            .andReturn(dummyFileReference);
        EasyMock.replay(componentDataManagementServiceMock);

        Map<String, String> metaData = generateInputProviderMetadata(testFile.getAbsolutePath());
        context.addSimulatedOutput(DIR_VARIABLE, "", DataType.DirectoryReference, false, metaData);
        dataException.expect(ComponentException.class);
        try {
            component.start();
        } finally {
            removeTempDirOrFile(createTempDir);
        }
    }

    private Map<String, String> generateInputProviderMetadata(String value) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(InputProviderComponentConstants.META_VALUE, value);
        return metadata;
    }

    /**
     * Creates a new temporary directory.
     * 
     * @return The File object of the directory.
     */
    private File createTempDir() {
        try {
            return tempFileService.createManagedTempDir();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test temp dir", e);
        }
    }

    /**
     * Creates a new temporary directory.
     * 
     * @return The File object of the directory.
     */
    private File createTempDir(String infoText) {
        try {
            return tempFileService.createManagedTempDir(infoText);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test temp dir", e);
        }
    }

    /**
     * Recursively removes a temporary directory.
     * 
     * @param file The File object of the directory.
     */
    private void removeTempDirOrFile(File file) {
        try {
            tempFileService.disposeManagedTempDirOrFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createAndVerifyFile(File parentDir, String name) throws IOException {
        File file = new File(parentDir, name);
        file.createNewFile();
        assertTrue(file.isFile() && file.canRead());
        return file;
    }

}
