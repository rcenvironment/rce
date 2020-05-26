/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

// import static org.junit.Assert.assertEquals;
//
// import java.beans.PropertyChangeListener;
// import java.io.File;
// import java.io.IOException;
// import java.util.Collection;
// import java.util.HashSet;
// import java.util.Set;
//
// import org.easymock.EasyMock;
// import org.junit.Before;
// import org.junit.Test;
//
// import
// de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
// import de.rcenvironment.core.component.model.api.ComponentDescription;
// import
// de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
// import
// de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
// import
// de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
// import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
// import de.rcenvironment.core.datamodel.api.DataType;
// import de.rcenvironment.core.utils.common.TempFileServiceAccess;
//
/// ***Tests for{
//
// @link
// InputProviderWorkflowNodeValidator}.**@author
// Tobias Rodehutskors*/public class InputProviderWorkflowNodeValidatorTest {
//
// // private static final String TEST_DIR = "testDir";
//
// private static final String TEST_FILE = "testFile";
//
// // private static final String PROJECT_NAME = "testProject";
// //
// // private static IProject project;
//
// // /**
// // * Create and open a new project in the current workspace. Needed for all
// tests involving
// relative paths.
// // *
// // * @throws CoreException not expected
// // */
// // @BeforeClass
// // public static void onlyOnce() throws CoreException {
// // IProjectDescription desc =
// ResourcesPlugin.getWorkspace().newProjectDescription(PROJECT_NAME);
// // project =
// ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
// // project.create(desc, new NullProgressMonitor());
// // project.open(new NullProgressMonitor());
// // }
//
// /**
// * @throws Exception not expected
// */
// @Before
// public void setUp() throws Exception {
// TempFileServiceAccess.setupUnitTestEnvironment();
// }
//
// /**
// * Set up a workflowNode with an end point of the specified data type and add
// the specified meta data value. Executes the validation and
// * return the validation messages.
// */
// private Collection<ComponentValidationMessage> testHelper(DataType
// endpointDataType, String metaDataValue) {
// InputProviderWorkflowNodeValidator validator = new
// InputProviderWorkflowNodeValidator();
//
// WorkflowNode node = EasyMock.createStrictMock(WorkflowNode.class);
// node.addPropertyChangeListener(EasyMock.anyObject(PropertyChangeListener.class));
//
// EndpointDescriptionsManager outputManager =
// EasyMock.createStrictMock(EndpointDescriptionsManager.class);
// EndpointDescription endpoint = new EndpointDescription(null, "test");
// endpoint.setDataType(endpointDataType);
//
// endpoint.setMetaDataValue(InputProviderComponentConstants.META_VALUE,
// metaDataValue);
//
// Set<EndpointDescription> endpoints = new HashSet<EndpointDescription>();
// endpoints.add(endpoint);
//
// EasyMock.expect(outputManager.getEndpointDescriptions()).andReturn(endpoints).atLeastOnce();
//
// ComponentDescription description =
// EasyMock.createStrictMock(ComponentDescription.class);
// EndpointDescriptionsManager inputManager =
// EasyMock.createStrictMock(EndpointDescriptionsManager.class);
// EasyMock.expect(inputManager.getEndpointDescriptions()).andReturn(new
// HashSet<EndpointDescription>());
// EasyMock.expect(description.getInputDescriptionsManager()).andReturn(inputManager);
//
// EasyMock.expect(node.getOutputDescriptionsManager()).andReturn(outputManager).atLeastOnce();
// EasyMock.expect(node.getComponentDescription()).andReturn(description);
// EasyMock.expect(node.getIdentifier()).andReturn("test");
//
// EasyMock.replay(inputManager);
// EasyMock.replay(outputManager);
// EasyMock.replay(node);
// EasyMock.replay(description);
//
// validator.setWorkflowNode(node, false);
// Collection<ComponentValidationMessage> messages = validator.getMessages();
//
// EasyMock.verify(inputManager);
// EasyMock.verify(outputManager);
// EasyMock.verify(node);
// EasyMock.verify(description);
//
// return messages;
// }
//
// // /**
// // * Create a new file in the current project.
// // */
// // private void createProjectAndFile(String fileName, String resource)
// // throws
// CoreException
//
// {
// // IFile testFile = project.getFile(fileName);
// // if (!testFile.exists()) {
// // InputStream source =
// getClass().getClassLoader().getResourceAsStream(resource);
// // testFile.create(source, IFile.FORCE, null);
// // }
// // }
// //
// // /**
// // * Create a new directory in the current project.
// // */
// // private void createProjectAndDir(String dirName) throws CoreException {
// //
// // IFolder testFolder = project.getFolder(dirName);
// // if (!testFolder.exists()) {
// // testFolder.create(false, true, null);
// // }
// // }
//
// /**
// * Check the collection for the existence of an error message.
// */
// private void expectOneErrorMessage(Collection<ComponentValidationMessage>
// messages) {
// assertEquals(1, messages.size());
// ComponentValidationMessage message = messages.toArray(new
// ComponentValidationMessage[1])[0];
// assertEquals(ComponentValidationMessage.Type.ERROR, message.getType());
// }
//
// /**
// * Test FileReference with a valid absolute reference to a file.
// *
// * @throws IOException
// * not expected
// */
// @Test
// public void testFileWithAbsoluteFile() throws IOException {
// File testFile =
// TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(TEST_FILE);
// Collection<ComponentValidationMessage> messages =
// testHelper(DataType.FileReference,
// testFile.getAbsolutePath());
//
// // no error expected
// assertEquals(0, messages.size());
// }
//
// // /**
// // * Test FileReference with a valid relative reference to a file.
// // *
// // * @throws IOException not expected
// // * @throws CoreException not expected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testFileWithRelativeFile() throws IOException, CoreException
// // {
// //
// // String fileName = TEST_FILE;
// // String resource = "sampleFile";
// // createProjectAndFile(fileName, resource);
// //
// // Collection<WorkflowNodeValidationMessage> messages =
// testHelper(DataType.FileReference, PROJECT_NAME + File.separator + fileName);
// //
// // // no error expected
// // assertEquals(0, messages.size());
// // }
//
// /**
// * Test FileReference with an invalid absolute reference to a directory.
// *
// * @throws IOException
// * not expected
// */
// @Test
// public void testFileWithAbsoluteDir() throws IOException {
// File testFile = TempFileServiceAccess.getInstance().createManagedTempDir();
// Collection<ComponentValidationMessage> messages =
// testHelper(DataType.FileReference,
// testFile.getAbsolutePath());
//
// expectOneErrorMessage(messages);
// }
//
// // /**
// // * Test FileReference with an invalid relative reference to a directory.
// // *
// // * @throws IOException not expected
// // * @throws CoreException not expected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testFileWithRelativeDir() throws IOException, CoreException {
// //
// // String dirName = TEST_DIR;
// // createProjectAndDir(dirName);
// //
// // Collection<WorkflowNodeValidationMessage> messages =
// testHelper(DataType.FileReference, PROJECT_NAME + File.separator + dirName);
// //
// // expectOneErrorMessage(messages);
// // }
// //
// // /**
// // * Test FileReference with an non existing reference.
// // *
// // * @throws IOException not expected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testFileWithNonExistingReference() throws IOException {
// // Collection<WorkflowNodeValidationMessage> messages =
// testHelper(DataType.FileReference, "fsadfsadfsadfds");
// //
// // expectOneErrorMessage(messages);
// // }
//
// /**
// * Test DirectoryReference with an invalid absolute reference to a file.
// *
// * @throws IOException
// * not expected
// */
// @Test
// public void testDirWithAbsoluteFile() throws IOException {
// File testFile =
// TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(TEST_FILE);
// Collection<ComponentValidationMessage> messages =
// testHelper(DataType.DirectoryReference,
// testFile.getAbsolutePath());
//
// expectOneErrorMessage(messages);
// }
//
// // /**
// // * Test DirectoryReference with an invalid relative reference to a file.
// // *
// // * @throws IOException not expected
// // * @throws CoreException not exptected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testDirWithRelativeFile() throws IOException, CoreException {
// //
// // String fileName = TEST_FILE;
// // String resource = "sampleFile";
// // createProjectAndFile(fileName, resource);
// //
// // Collection<WorkflowNodeValidationMessage> messages =
// // testHelper(DataType.DirectoryReference, PROJECT_NAME + File.separator +
// fileName);
// //
// // expectOneErrorMessage(messages);
// // }
//
// /**
// * Test DirectoryReference with a valid absolute reference to a directory.
// *
// * @throws IOException not expected
// */
// @Test
// public void testDirWithAbsoluteDir() throws IOException {
// File testDir = TempFileServiceAccess.getInstance().createManagedTempDir();
// Collection<ComponentValidationMessage> messages =
// testHelper(DataType.DirectoryReference, testDir.getAbsolutePath());
//
// // no error expected
// assertEquals(0, messages.size());
// }
//
// // /**
// // * Test DirectoryReference with a valid relative reference to a directory.
// // *
// // * @throws IOException not expected
// // * @throws CoreException not expected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testDirWithRelativeDir() throws IOException, CoreException {
// //
// // String dirName = TEST_DIR;
// // createProjectAndDir(dirName);
// //
// // Collection<WorkflowNodeValidationMessage> messages =
// // testHelper(DataType.DirectoryReference, PROJECT_NAME + File.separator +
// dirName);
//
// //
// // // no error expected
// // assertEquals(0, messages.size());
// // }
// //
// // /**
// // * Test DirectoryReference with a non existing reference.
// // *
// // * @throws IOException not expected
// // */
// // // TODO JUnit PluginTest
// // @Test
// // public void testDirWithNonExistingReference() throws IOException {
// // Collection<WorkflowNodeValidationMessage> messages =
// testHelper(DataType.FileReference, "fsadfsadfsadfds");
// //
// // expectOneErrorMessage(messages);
// // }
//
// }
