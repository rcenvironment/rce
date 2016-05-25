/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants.HandleExistingFile;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Integration test for OutputWriter Components.
 *
 * @author Brigitte Boden
 */
public class OutputWriterComponentTest {

    private static final String INDENT = "\t";

    private static final String SOME_SHORT_TEXT = "short text";

    private static final String ENDPOINT_ID_TEXT = "textInput_id";

    private static final String ENDPOINT_NAME_TEXT = "textInput";

    private static final String ENDPOINT_ID_BOOLEAN = "booleanInput_id";

    private static final String ENDPOINT_NAME_BOOLEAN = "booleanInput";

    private static final String ENDPOINT_ID_INT = "intInput_id";

    private static final String ENDPOINT_NAME_INT = "intInput";

    private static final String ENDPOINT_ID_FLOAT = "floatInput_id";

    private static final String ENDPOINT_NAME_FLOAT = "floatInput";

    private static final String EXCEPTION_MESSAGE = "Exception while setting up componentDataManagementServiceMock";

    private static final String TARGET_NAME_FILE = "myfile";

    private static final String TARGET_NAME_DIRECTORY = "myDir";

    private static final String ENDPOINT_ID_DIRECTORY = "endpointId2";

    private static final String ENDPOINT_ID_FILE = "endpointId1";

    private static final String ENDPOINT_NAME_DIRECTORY = "dirInput";

    private static final String ENDPOINT_NAME_FILE = "fileInput";

    private static final String TARGET_SUBFOLDER_NAME = "sub";

    private static final String INPUT_DIRECTORY = "inputDirectory";

    private static final String INPUT_FILENAME = "inputFile";

    private TempFileService tempFileService;

    private File testRootDir;

    private File inputFile;

    private File inputDirectory;

    private final Log log = LogFactory.getLog(getClass());

    private ObjectMapper mapper;

    /**
     * Context mock for Output Writer Component test.
     *
     * @author Brigitte Boden
     */
    private final class OutputWriterComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = 4307822088604760715L;

        @Override
        public String getWorkflowInstanceName() {
            return "TestWorkflowInstanceName";
        }

        @Override
        public String getInstanceName() {
            return "TestInstanceName";
        }

        @Override
        public String getComponentName() {
            return "TestComponentName";
        }

    }

    private ComponentTestWrapper component;

    private OutputWriterComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    private ComponentDataManagementService componentDataManagementServiceMock;

    /*
     * These two IAnswers are called by the mocked ComponentDataManagementService and imitate the behavior of the methods
     * copyReferenceToLocalFile() and copyDirectoryReferenceTDToLocalDirectory (on the local node). This is necessary because the "real"
     * datamanagement service is not available here, but for test purposes the output files/directories have to be really created.
     * 
     * WARNING: Therefore, this test may have to be adapted if the API for the datamanagement service changes.
     */
    private IAnswer<Void> copyReferenceToLocalFileAnswer = new IAnswer<Void>() {

        @Override
        public Void answer() throws Throwable {
            String reference = (String) EasyMock.getCurrentArguments()[0];
            File targetfile = (File) EasyMock.getCurrentArguments()[1];

            FileUtils.copyFile(new File(reference), targetfile);
            return null;
        }
    };

    private IAnswer<Void> copyDirectoryReferenceTDToLocalDirectoryAnswer = new IAnswer<Void>() {

        @Override
        public Void answer() throws Throwable {
            String reference = ((DirectoryReferenceTD) EasyMock.getCurrentArguments()[1]).getDirectoryReference();
            File targetdir = (File) EasyMock.getCurrentArguments()[2];

            FileUtils.copyDirectoryToDirectory(new File(reference), targetdir);
            return null;
        }
    };

    /**
     * Set up the Output Writer test.
     * 
     * @throws Exception e
     */
    @Before
    public void setup() throws Exception {
        context = new OutputWriterComponentContextMock();
        component = new ComponentTestWrapper(new OutputWriterComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

        // Setup root directory for testing
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        log.debug("Testing in temporary directory " + testRootDir.getAbsolutePath());

        inputFile = createAndVerifyFile(testRootDir, INPUT_FILENAME);
        inputDirectory = createAndVerifySubdir(testRootDir, INPUT_DIRECTORY);
        assertEquals(2, testRootDir.listFiles().length);

        // Add some files to the input directory
        createAndVerifyFile(inputDirectory, "fileInDir1");
        createAndVerifyFile(inputDirectory, "fileInDir2");
        assertEquals(2, inputDirectory.listFiles().length);

        componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);

        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
        context.setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ROOT, testRootDir.getAbsolutePath());

        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(JsonMethod.ALL, Visibility.ANY);
    }

    /**
     * Test with no input.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testNoInputs() throws ComponentException {
        component.start();
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test file and directory inputs. Here, no previous file/directory exists (i.e. no autorename etc. is necessary).
     * 
     * @throws ComponentException e
     */
    @Test
    public void testWithFileAndDirInputs() throws ComponentException {
        try {
            EasyMock.reset(componentDataManagementServiceMock);
            // Set expectations for data management calls. Arguments should not be null, except for the NodeIdentifier, which is obtained
            // from the mock component context instead of a "real" context.
            componentDataManagementServiceMock.copyReferenceToLocalFile(notNull(String.class),
                notNull(File.class), anyObject(NodeIdentifier.class));
            EasyMock.expectLastCall().andAnswer(copyReferenceToLocalFileAnswer);
            componentDataManagementServiceMock.copyDirectoryReferenceTDToLocalDirectory(notNull(ComponentContext.class),
                notNull(DirectoryReferenceTD.class),
                notNull(File.class));
            EasyMock.expectLastCall().andAnswer(copyDirectoryReferenceTDToLocalDirectoryAnswer);
            EasyMock.replay(componentDataManagementServiceMock);
        } catch (IOException e1) {
            throw new ComponentException(EXCEPTION_MESSAGE);
        }

        component.start();
        Map<String, String> metadataForFile = generateMetadata(TARGET_NAME_FILE, TARGET_SUBFOLDER_NAME);
        context.addSimulatedInput(ENDPOINT_NAME_FILE, ENDPOINT_ID_FILE, DataType.FileReference, true, metadataForFile);
        Map<String, String> metadataForDir = generateMetadata(TARGET_NAME_DIRECTORY, TARGET_SUBFOLDER_NAME);
        context.addSimulatedInput(ENDPOINT_NAME_DIRECTORY, ENDPOINT_ID_DIRECTORY, DataType.DirectoryReference, true, metadataForDir);

        // Assert that the target file and directory do not exist yet
        String path = testRootDir + File.separator + TARGET_SUBFOLDER_NAME;
        assertFalse(new File(path, TARGET_NAME_FILE).exists());
        assertFalse(new File(path, TARGET_NAME_DIRECTORY).exists());

        // Set a file input value
        String fileReference = inputFile.getAbsolutePath();
        context.setInputValue(ENDPOINT_NAME_FILE, typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();

        // Set a directory input value
        String dirReference = inputDirectory.getAbsolutePath();
        context.setInputValue(ENDPOINT_NAME_DIRECTORY, typedDatumFactory.createDirectoryReference(dirReference, inputDirectory.getName()));
        component.processInputs();

        // Assert that the target file and directory do exist now and the directory contains its two files.
        assertTrue(new File(path, TARGET_NAME_FILE).exists());
        assertTrue(new File(path, TARGET_NAME_DIRECTORY).exists());
        assertEquals(2, new File(path, TARGET_NAME_DIRECTORY).listFiles().length);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test file and directory inputs with already existing targets. Here, the autorename option is tested.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testWithFileAndDirInputsExistingTarget() throws ComponentException {
        try {
            EasyMock.reset(componentDataManagementServiceMock);
            // Set expectations for data management calls. Arguments should not be null, except for the NodeIdentifier, which is obtained
            // from the mock component context instead of a "real" context.
            componentDataManagementServiceMock.copyReferenceToLocalFile(notNull(String.class),
                notNull(File.class), anyObject(NodeIdentifier.class));
            EasyMock.expectLastCall().andAnswer(copyReferenceToLocalFileAnswer);
            componentDataManagementServiceMock.copyDirectoryReferenceTDToLocalDirectory(notNull(ComponentContext.class),
                notNull(DirectoryReferenceTD.class),
                notNull(File.class));
            EasyMock.expectLastCall().andAnswer(copyDirectoryReferenceTDToLocalDirectoryAnswer);
            EasyMock.replay(componentDataManagementServiceMock);
        } catch (IOException e1) {
            throw new ComponentException(EXCEPTION_MESSAGE);
        }

        component.start();
        Map<String, String> metadataForFile = generateMetadata(TARGET_NAME_FILE, TARGET_SUBFOLDER_NAME);
        context.addSimulatedInput(ENDPOINT_NAME_FILE, ENDPOINT_ID_FILE, DataType.FileReference, true, metadataForFile);
        Map<String, String> metadataForDir = generateMetadata(TARGET_NAME_DIRECTORY, TARGET_SUBFOLDER_NAME);
        context.addSimulatedInput(ENDPOINT_NAME_DIRECTORY, ENDPOINT_ID_DIRECTORY, DataType.DirectoryReference, true, metadataForDir);

        // Create file and directory with the given names, such that the Output Writer has to use the autorename option.
        File folderForSaving = createAndVerifySubdir(testRootDir, TARGET_SUBFOLDER_NAME);
        try {
            createAndVerifyFile(folderForSaving, TARGET_NAME_FILE);
            createAndVerifySubdir(folderForSaving, TARGET_NAME_DIRECTORY);
        } catch (IOException e) {
            log.error("Error while creating test file: " + e);
        }
        assertEquals(2, folderForSaving.listFiles().length);

        // Set a file input value
        String fileReference = inputFile.getAbsolutePath();
        context.setInputValue(ENDPOINT_NAME_FILE, typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();

        // Set a directory input value
        String dirReference = inputDirectory.getAbsolutePath();
        context.setInputValue(ENDPOINT_NAME_DIRECTORY, typedDatumFactory.createDirectoryReference(dirReference, inputDirectory.getName()));
        component.processInputs();

        // Due to the autorename option, there should now be 2 files and 2 directories
        assertEquals(4, folderForSaving.listFiles().length);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test the replacement of placeholders.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testPlaceholders() throws ComponentException {
        try {
            EasyMock.reset(componentDataManagementServiceMock);
            // Set expectations for data management calls. Arguments should not be null, except for the NodeIdentifier, which is obtained
            // from the mock component context instead of a "real" context.
            componentDataManagementServiceMock.copyReferenceToLocalFile(notNull(String.class),
                notNull(File.class), anyObject(NodeIdentifier.class));
            EasyMock.expectLastCall().andAnswer(copyReferenceToLocalFileAnswer).times(7);
            EasyMock.replay(componentDataManagementServiceMock);
        } catch (IOException e1) {
            throw new ComponentException(EXCEPTION_MESSAGE);
        }

        component.start();

        // Set an input for each supported placeholder
        final String end = "yyy";
        final String beginning = "xxx";
        context.addSimulatedInput("workflowname", "workflowname_id", DataType.FileReference, true,
            generateMetadata(beginning + OutputWriterComponentConstants.PH_WORKFLOWNAME + end, null));
        context.addSimulatedInput("inputname", "inputname_id", DataType.FileReference, true,
            generateMetadata(beginning + OutputWriterComponentConstants.PH_INPUTNAME + end, null));
        context.addSimulatedInput("start_ts", "start_ts_id", DataType.FileReference, true,
            generateMetadata(beginning + "starttime" + OutputWriterComponentConstants.PH_WF_START_TS + end, null));
        context.addSimulatedInput("comp_name", "comp_name_id", DataType.FileReference, true,
            generateMetadata(beginning + OutputWriterComponentConstants.PH_COMP_NAME + end, null));
        context.addSimulatedInput("comp_type", "comp_type_id", DataType.FileReference, true,
            generateMetadata(beginning + OutputWriterComponentConstants.PH_COMP_TYPE + end, null));
        context.addSimulatedInput("ts", "ts_id", DataType.FileReference, true,
            generateMetadata(beginning + "time" + OutputWriterComponentConstants.PH_TIMESTAMP + end, null));
        context.addSimulatedInput("ec", "ec_id", DataType.FileReference, true,
            generateMetadata(beginning + "execution_count" + OutputWriterComponentConstants.PH_EXECUTION_COUNT + end, null));

        // Send input file to each input and test if placeholders are replaced
        String fileReference = inputFile.getAbsolutePath();
        context.setInputValue("workflowname", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("inputname", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("ts", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("start_ts", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("comp_name", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("comp_type", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();
        context.setInputValue("ec", typedDatumFactory.createFileReference(fileReference, inputFile.getName()));
        component.processInputs();

        assertEquals(9, testRootDir.listFiles().length); // input file, input directory + 7 output files

        // Assert that filenames do not contain placeholders and the text besides the placeholders has not been changed.
        for (int i = 0; i < testRootDir.listFiles().length; i++) {
            String filename = testRootDir.listFiles()[i].getName();
            if (!filename.equals(INPUT_DIRECTORY) && !filename.equals(INPUT_FILENAME)) {
                if (!filename.endsWith(end) || !filename.startsWith(beginning)
                    || filename.contains(OutputWriterComponentConstants.PH_PREFIX)
                    || filename.contains(OutputWriterComponentConstants.PH_SUFFIX)) {
                    fail();
                }
            }
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
        EasyMock.verify(componentDataManagementServiceMock);
    }

    /**
     * Test with inputs of simple data types and without targets.
     * 
     * @throws ComponentException e
     */
    @Test
    public void testSimpleDataInputsWithoutTargets() throws ComponentException {

        EasyMock.reset(componentDataManagementServiceMock);

        component.start();
        context.addSimulatedInput(ENDPOINT_NAME_FLOAT, ENDPOINT_ID_FLOAT, DataType.Float, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_INT, ENDPOINT_ID_INT, DataType.Integer, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_BOOLEAN, ENDPOINT_ID_BOOLEAN, DataType.Boolean, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_TEXT, ENDPOINT_ID_TEXT, DataType.Float, true, null);

        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();

        assertTrue(testRootDir.listFiles().length == 2); // This test should not produce output, thus only the input file/dir exist.

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with inputs of simple data types and with several targets. ("Append" Option)
     * 
     * @throws ComponentException e
     * @throws IOException e
     */
    @Test
    public void testSimpleDataInputsWithTargetsAppend() throws ComponentException, IOException {

        EasyMock.reset(componentDataManagementServiceMock);

        createSimpleDataInputsAndTargets(HandleExistingFile.APPEND);

        component.start();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();

        assertTrue(testRootDir.listFiles().length == 4);

        for (File file : testRootDir.listFiles()) {
            if (!file.isDirectory() && !file.getName().equals(INPUT_FILENAME)) {
                checkSimpleDataOutputFile(file);
            }
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with inputs of simple data types and with several targets. ("Override" Option)
     * 
     * @throws ComponentException e
     * @throws IOException e
     */
    @Test
    public void testSimpleDataInputsWithTargetsOverride() throws ComponentException, IOException {

        EasyMock.reset(componentDataManagementServiceMock);

        createSimpleDataInputsAndTargets(HandleExistingFile.OVERRIDE);

        component.start();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();

        assertTrue(testRootDir.listFiles().length == 4);

        for (File file : testRootDir.listFiles()) {
            if (!file.isDirectory() && !file.getName().equals(INPUT_FILENAME)) {
                checkSimpleDataOutputFile(file);
            }
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with inputs of simple data types and with several targets. ("Auto-Rename" Option)
     * 
     * @throws ComponentException e
     * @throws IOException e
     */
    @Test
    public void testSimpleDataInputsWithTargetsAutorename() throws ComponentException, IOException {

        EasyMock.reset(componentDataManagementServiceMock);

        createSimpleDataInputsAndTargets(HandleExistingFile.AUTORENAME);

        component.start();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_FLOAT, typedDatumFactory.createFloat(0.0));
        context.setInputValue(ENDPOINT_NAME_INT, typedDatumFactory.createInteger(0));
        component.processInputs();
        context.setInputValue(ENDPOINT_NAME_BOOLEAN, typedDatumFactory.createBoolean(false));
        context.setInputValue(ENDPOINT_NAME_TEXT, typedDatumFactory.createShortText(SOME_SHORT_TEXT));
        component.processInputs();

        assertTrue(testRootDir.listFiles().length == 6);

        for (File file : testRootDir.listFiles()) {
            if (!file.isDirectory() && !file.getName().equals(INPUT_FILENAME)) {
                checkSimpleDataOutputFile(file);
            }
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Generates meta data for output writer test.
     * 
     * @param from Start value of parametric study range
     * @param to End value of parametric study range
     * @param step Step size of parametric study range
     * @return
     */
    private Map<String, String> generateMetadata(String filename, String foldername) {
        Map<String, String> metadata = new HashMap<>();
        String folderForSaving = OutputWriterComponentConstants.ROOT_DISPLAY_NAME;
        if (foldername != null && !foldername.isEmpty()) {
            folderForSaving = folderForSaving + "\\" + foldername;
        }
        metadata.put(OutputWriterComponentConstants.CONFIG_KEY_FILENAME, filename);
        metadata.put(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING, folderForSaving);
        return metadata;
    }

    private File createAndVerifySubdir(File parentDir, String name) {
        File dir = new File(parentDir, name);
        dir.mkdir();
        assertTrue(dir.isDirectory());
        return dir;
    }

    private File createAndVerifyFile(File parentDir, String name) throws IOException {
        File file = new File(parentDir, name);
        file.createNewFile();
        assertTrue(file.isFile() && file.canRead());
        return file;
    }

    private void createSimpleDataInputsAndTargets(HandleExistingFile handling) {
        context.addSimulatedInput(ENDPOINT_NAME_FLOAT, ENDPOINT_ID_FLOAT, DataType.Float, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_INT, ENDPOINT_ID_INT, DataType.Integer, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_BOOLEAN, ENDPOINT_ID_BOOLEAN, DataType.Boolean, true, null);
        context.addSimulatedInput(ENDPOINT_NAME_TEXT, ENDPOINT_ID_TEXT, DataType.Float, true, null);

        String header1 =
            OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME
                + OutputWriterComponentConstants.PH_DELIM + ENDPOINT_NAME_FLOAT + OutputWriterComponentConstants.PH_SUFFIX + INDENT
                + OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME
                + OutputWriterComponentConstants.PH_DELIM + ENDPOINT_NAME_INT + OutputWriterComponentConstants.PH_SUFFIX;

        String header2 =
            OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME
                + OutputWriterComponentConstants.PH_DELIM + ENDPOINT_NAME_BOOLEAN + OutputWriterComponentConstants.PH_SUFFIX + INDENT
                + OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME
                + OutputWriterComponentConstants.PH_DELIM + ENDPOINT_NAME_TEXT + OutputWriterComponentConstants.PH_SUFFIX;

        String format1 =
            OutputWriterComponentConstants.PH_PREFIX + ENDPOINT_NAME_FLOAT + OutputWriterComponentConstants.PH_SUFFIX + INDENT
                + OutputWriterComponentConstants.PH_PREFIX + ENDPOINT_NAME_INT
                + OutputWriterComponentConstants.PH_SUFFIX + OutputWriterComponentConstants.PH_LINEBREAK;

        String format2 =
            OutputWriterComponentConstants.PH_PREFIX + ENDPOINT_NAME_BOOLEAN + OutputWriterComponentConstants.PH_SUFFIX
                + INDENT + OutputWriterComponentConstants.PH_PREFIX + ENDPOINT_NAME_TEXT
                + OutputWriterComponentConstants.PH_SUFFIX + OutputWriterComponentConstants.PH_LINEBREAK;

        List<String> inputs1 = new ArrayList<String>();
        inputs1.add(ENDPOINT_NAME_FLOAT);
        inputs1.add(ENDPOINT_NAME_INT);
        OutputLocation ol1 =
            new OutputLocation("output1.txt", OutputWriterComponentConstants.ROOT_DISPLAY_NAME, header1, format1,
                handling,
                inputs1);
        List<String> inputs2 = new ArrayList<String>();
        inputs2.add(ENDPOINT_NAME_BOOLEAN);
        inputs2.add(ENDPOINT_NAME_TEXT);
        OutputLocation ol2 =
            new OutputLocation("output2.txt", OutputWriterComponentConstants.ROOT_DISPLAY_NAME, header2, format2,
                handling,
                inputs2);

        OutputLocationList list = new OutputLocationList();
        list.addOrReplaceOutputLocation(ol1);
        list.addOrReplaceOutputLocation(ol2);
        try {
            context.setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS, mapper.writeValueAsString(list));
        } catch (IOException e) {
            fail("Json Error");
        }
    }

    // Checks if the file is not empty and does not contain placeholders
    private void checkSimpleDataOutputFile(File output) throws IOException {
        String content = FileUtils.readFileToString(output);
        if (content.isEmpty()) {
            fail("Output file " + output.getName() + " is empty.");
        } else {
            if (content.contains(OutputWriterComponentConstants.PH_PREFIX) && content.contains(OutputWriterComponentConstants.PH_SUFFIX)) {
                fail("In output file " + output.getName() + ", not all placeholders were resolved.");
            }
        }
    }
}
