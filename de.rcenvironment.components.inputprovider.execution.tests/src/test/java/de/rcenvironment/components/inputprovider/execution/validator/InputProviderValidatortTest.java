/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.execution.validator;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreator;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test for the validator of the Input Provider.
 * 
 * TODO : All tests that are commented out need a open project in a workspace. The setup of this project is not clear though.
 *
 * @author Jascha Riedel
 */
public class InputProviderValidatortTest {

    private static final String TEST_DIR = "testDir";

    private static final String TEST_FILE = "testFile";

    private static final String PROJECT_NAME = "testProject";

    private static final String TEST_INPUT = "testOutput";

    private static IProject project;

    private ComponentDescriptionMockCreator componentDescriptionHelper;

    private InputProviderComponentValidator validator;

    /**
     * Create and open a new project in the current workspace. Needed for all tests involving
     *
     * relative paths.
     *
     * @throws CoreException not expected
     */
    // @BeforeClass
    // public static void onlyOnce() throws CoreException {
    // IProjectDescription desc =
    // ResourcesPlugin.getWorkspace().newProjectDescription(PROJECT_NAME);
    // project =
    // ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    // project.create(desc, new NullProgressMonitor());
    // project.open(new NullProgressMonitor());
    // }

    /**
     * @throws Exception not expected
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        componentDescriptionHelper = new ComponentDescriptionMockCreator();
        validator = new InputProviderComponentValidator();
    }

    /**
     * Test FileReference with a valid absolute reference to a file.
     *
     * @throws IOException not expected
     */
    @Test
    public void testFileWithAbsoluteFile() throws IOException {
        File testFile =
            TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(TEST_FILE);

        addSimulatedOutput(DataType.FileReference, testFile.getAbsolutePath());

        List<ComponentValidationMessage> messages =
            validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());

        // no error expected
        assertEquals(0, messages.size());
    }

    /**
     * Test FileReference with a valid absolute reference to a directory.
     *
     * @throws IOException not expected
     */
    @Test
    public void testFileReferenceIsAbsoluteDirectory() throws IOException {
        File testFile = TempFileServiceAccess.getInstance().createManagedTempDir();
        addSimulatedOutput(DataType.FileReference, testFile.getAbsolutePath());

        List<ComponentValidationMessage> messages =
            validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());

        expectOneWarningMessage(messages);
    }

    /**
     * 
     * Test FileReference with a valid relative reference to a directory.
     * 
     * @throws CoreException not expected
     */
    // @Test
    // public void testFileReferenceIsRelativeDirectory() throws CoreException {
    // String testDir = TEST_DIR;
    // createProjectAndDir(testDir);
    //
    // addSimulatedInput(DataType.FileReference, PROJECT_NAME + File.separator + testDir);
    // List<ComponentValidationMessage> messages =
    // validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
    //
    // expectOneErrorMessage(messages);
    // }

    /**
     * Test with non existing file reference.
     */
    // @Test
    // public void testWithNonExistingFileReference() {
    // addSimulatedInput(DataType.FileReference, "kldsajgklsaj");
    // List<ComponentValidationMessage> messages =
    // validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
    // expectOneErrorMessage(messages);
    // }

    /**
     * Test with non existing directory reference.
     */
    // @Test
    // public void testWithNonExistingDirectoryReference() {
    // addSimulatedInput(DataType.DirectoryReference, "kldsajgklsaj");
    // List<ComponentValidationMessage> messages =
    // validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
    // expectOneErrorMessage(messages);
    // }

    /**
     * 
     * Test existing directory with absolute path.
     * 
     * @throws IOException not expected
     */
    @Test
    public void testWithExistingAbsoluteDirectoryReference() throws IOException {
        File testDir = TempFileServiceAccess.getInstance().createManagedTempDir();
        addSimulatedOutput(DataType.DirectoryReference, testDir.getAbsolutePath());
        List<ComponentValidationMessage> messages =
            validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
        assertEquals(0, messages.size());
    }

    /**
     * 
     * Test existing directory with relative path.
     * 
     * @throws CoreException not expected
     */
    // @Test
    // public void testWithExistingRelativeDirectoryReference() throws CoreException {
    // String testDir = TEST_DIR;
    // createProjectAndDir(testDir);
    // addSimulatedInput(DataType.DirectoryReference, PROJECT_NAME + File.separator + testDir);
    // List<ComponentValidationMessage> messages =
    // validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
    // assertEquals(0, messages.size());
    // }

    /**
     * Test directory with absolute file reference.
     * 
     * @throws IOException not expected
     */
    @Test
    public void testDirectoryWithAbsoluteFileReference() throws IOException {
        File testFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(TEST_FILE);
        addSimulatedOutput(DataType.DirectoryReference, testFile.getAbsolutePath());
        List<ComponentValidationMessage> messages =
            validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
        expectOneWarningMessage(messages);
    }

    /**
     * 
     * Test directory with relative file reference.
     * 
     * @throws CoreException not expected
     */
    // @Test
    // public void testDirectoryWithRelativeFileReference() throws CoreException {
    // String testFile = TEST_FILE;
    // String ressource = "sample_ressource";
    // createProjectAndFile(TEST_FILE, ressource);
    // addSimulatedInput(DataType.DirectoryReference, PROJECT_NAME + File.separator + testFile);
    // List<ComponentValidationMessage> messages =
    // validator.validateComponentSpecific(componentDescriptionHelper.createComponentDescriptionMock());
    // expectOneErrorMessage(messages);
    // }

    /**
     * Create a new file in the current project.
     */
    private void createProjectAndFile(String fileName, String resource)
        throws CoreException {
        IFile testFile = project.getFile(fileName);
        if (!testFile.exists()) {
            InputStream source =
                getClass().getClassLoader().getResourceAsStream(resource);
            testFile.create(source, IFile.FORCE, null);
        }
    }

    /**
     * Create a new directory in the current project.
     */
    private void createProjectAndDir(String dirName) throws CoreException {

        IFolder testFolder = project.getFolder(dirName);
        if (!testFolder.exists()) {
            testFolder.create(false, true, null);
        }
    }

    private void addSimulatedOutput(DataType dataType, String metaDataValue) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put(InputProviderComponentConstants.META_VALUE, metaDataValue);
        componentDescriptionHelper.addSimulatedOutput(TEST_INPUT, dataType, metaData, true);
    }

    private void expectOneErrorMessage(List<ComponentValidationMessage> messages) {
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }
    
    private void expectOneWarningMessage(List<ComponentValidationMessage> messages) {
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.WARNING, messages.get(0).getType());
    }

}
