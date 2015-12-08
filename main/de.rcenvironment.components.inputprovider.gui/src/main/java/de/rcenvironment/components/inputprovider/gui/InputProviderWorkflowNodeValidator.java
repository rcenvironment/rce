/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.inputprovider.gui;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants.FileSourceType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * Validator for Input Provider component.
 *
 * @author Marc Stammerjohann
 */
public class InputProviderWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private Collection<WorkflowNodeValidationMessage> messages;

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        messages = new LinkedList<WorkflowNodeValidationMessage>();
        validateExistingEndpoints(DataType.FileReference);
        validateExistingEndpoints(DataType.DirectoryReference);
        return messages;
    }

    @Override
    protected Collection<WorkflowNodeValidationMessage> validateOnStart() {
        return validate();
    }

    private void validateExistingEndpoints(DataType dataType) {
        Set<EndpointDescription> outputs = getOutputs(dataType);
        for (EndpointDescription outputDescription : outputs) {
            Map<String, String> metaData = outputDescription.getMetaData();
            String name = metaData.get(InputProviderComponentConstants.META_VALUE);
            Collection<String> values = metaData.values();
            if (dataType.equals(DataType.FileReference)) {
                validateExistingFiles(outputDescription.getName(), values, name);
            } else if (dataType.equals(DataType.DirectoryReference)) {
                validateExistingDirectories(outputDescription.getName(), values, name);
            }
        }
    }

    /**
     * Validate the input provider, if selected files exists.
     * 
     * @param fileName of the file
     * @param values of the {@link EndpointDescription}
     */
    private void validateExistingFiles(String outputName, Collection<String> values, String fileName) {
        if (!values.contains(FileSourceType.atWorkflowStart.toString())) {
            if (values.contains(FileSourceType.fromProject.toString())) {
                validateFileFromProject(outputName, fileName);
            } else {
                validateFileFromFileSystem(outputName, fileName);
            }
        }
    }

    private void validateFileFromProject(String outputName, String fileName) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IFile newFile = (IFile) workspaceRoot
            .findMember(fileName);
        if (newFile == null || !newFile.exists()) {
            createFileDoesNotExistMessage(outputName, fileName);
        }
    }

    private void validateFileFromFileSystem(String outputName, String fileName) {
        File newFile = new File(fileName);
        if (!newFile.isFile()) {
            createFileDoesNotExistMessage(outputName, fileName);
        }
    }

    private void createFileDoesNotExistMessage(String outputName, String fileName) {
        String text = StringUtils.format("'%s': missing file '%s'", outputName, fileName);
        messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, "", text, text, true));
    }

    /**
     * Validate the input provider, if a selected directory exists.
     * 
     * @param name of the directory
     * @param values of the {@link EndpointDescription}
     */
    private void validateExistingDirectories(String outputName, Collection<String> values, String pathName) {
        if (pathName.equals(RemoteAccessConstants.WF_PLACEHOLDER_INPUT_DIR)) {
            String text = StringUtils.format("'%s': '%s' only valid if used via remote access", outputName, pathName);
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING, "", text, text, false));
            return;
        }
        if (!values.contains(FileSourceType.atWorkflowStart.toString())) {
            if (values.contains(FileSourceType.fromProject.toString())) {
                validateDirFromProject(outputName, pathName);
            } else {
                validateDirFromFileSystem(outputName, pathName);
            }
        }
    }

    private void validateDirFromProject(String outputName, String pathName) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IFolder folder = (IFolder) workspaceRoot.findMember(pathName);
        if (folder == null || !folder.exists()) {
            createDirDoesNotExistMessage(outputName, pathName);
        }
    }

    private void validateDirFromFileSystem(String outputName, String pathName) {
        File newFile = new File(pathName);
        if (!newFile.isDirectory()) {
            createDirDoesNotExistMessage(outputName, pathName);
        }
    }

    private void createDirDoesNotExistMessage(String outputName, String pathName) {
        String text = StringUtils.format("'%s': missing directory '%s'", outputName, pathName);
        messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, "", text, text, true));
    }


}
