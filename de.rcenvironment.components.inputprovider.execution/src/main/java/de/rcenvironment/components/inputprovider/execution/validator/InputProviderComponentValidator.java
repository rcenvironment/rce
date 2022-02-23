/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.execution.validator;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * Validator for Input Provider component.
 *
 * @author Marc Stammerjohann
 * @author Jascha Riedel
 * @author Kathrin Schaffert (#17716)
 */
public class InputProviderComponentValidator extends AbstractComponentValidator {

    private static final String INVALID_PATH_MESSAGE = "The specified output \"%s\" is not valid on the current system. "
        + "\n Please make sure that the path exists when running the workflow. \n (%s)";

    private static final String MISSING_PATH_MESSAGE = "The specified output \"%s\" is missing on the current system. "
        + "\n Please make sure that the output exists when running the workflow.";

    @Override
    public String getIdentifier() {
        return InputProviderComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();
        validateExistingEndpoints(componentDescription, messages, DataType.FileReference);
        validateExistingEndpoints(componentDescription, messages, DataType.DirectoryReference);
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

    private void validateExistingEndpoints(ComponentDescription componentDescription,
        List<ComponentValidationMessage> messages, DataType dataType) {
        Set<EndpointDescription> outputs = getOutputs(componentDescription, dataType);
        for (EndpointDescription outputDescription : outputs) {
            Map<String, String> metaData = outputDescription.getMetaData();
            String name = metaData.get(InputProviderComponentConstants.META_VALUE);
            Collection<String> values = metaData.values();
            if (dataType.equals(DataType.FileReference)) {
                validateExistingFiles(messages, outputDescription.getName(), values, name);
            } else if (dataType.equals(DataType.DirectoryReference)) {
                validateExistingDirectories(messages, outputDescription.getName(), values, name);
            }
        }
    }

    /**
     * Validate the input provider, if selected files exists.
     * 
     * @param fileName of the file
     * @param values of the {@link EndpointDescription}
     */
    private void validateExistingFiles(List<ComponentValidationMessage> messages, String outputName,
        Collection<String> values, String fileName) {
        if (!values.contains(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART)) {
            try {
                if (Paths.get(fileName).isAbsolute()) {
                    validateFileFromFileSystem(messages, fileName);
                } else {
                    validateFileFromProject(messages, outputName, fileName);
                }
            } catch (InvalidPathException e) {
                String text = StringUtils.format(INVALID_PATH_MESSAGE, fileName, e.getMessage());
                messages.add(
                    new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, "", text, text, true));
            }
        }
    }

    private void validateFileFromProject(List<ComponentValidationMessage> messages, String outputName,
        String fileName) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IResource member = workspaceRoot.findMember(fileName);

        if (member == null || !member.exists()) {
            createFileDoesNotExistMessage(messages, fileName);
        } else {
            if (!(member instanceof IFile)) {
                String text = StringUtils.format("'%s': '%s' is not a file", outputName, fileName);
                messages.add(
                    new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, "", text, text, true));
            }
        }
    }

    private void validateFileFromFileSystem(List<ComponentValidationMessage> messages, String fileName) {
        File newFile = new File(fileName);
        if (!newFile.isFile()) {
            createFileDoesNotExistMessage(messages, fileName);
        }
    }

    private void createFileDoesNotExistMessage(List<ComponentValidationMessage> messages, String fileName) {
        String text = StringUtils.format(MISSING_PATH_MESSAGE, fileName);
        messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, "", text, text, true));
    }

    /**
     * Validate the input provider, if a selected directory exists.
     * 
     * @param name of the directory
     * @param values of the {@link EndpointDescription}
     */
    private void validateExistingDirectories(List<ComponentValidationMessage> messages, String outputName,
        Collection<String> values, String pathName) {
        if (pathName.equals(RemoteAccessConstants.WF_PLACEHOLDER_INPUT_DIR)) {
            String text = StringUtils.format("'%s': '%s' only valid if used via remote access", outputName, pathName);
            messages.add(
                new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, "", text, text, false));
            return;
        }

        if (!values.contains(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART)) {
            try {
                if (Paths.get(pathName).isAbsolute()) {
                    validateDirFromFileSystem(messages, pathName);
                } else {
                    validateDirFromProject(messages, outputName, pathName);
                }
            } catch (InvalidPathException e) {
                String text = StringUtils.format(INVALID_PATH_MESSAGE, pathName, e.getMessage());
                messages.add(
                    new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, "", text, text, true));
            }
        }
    }

    private void validateDirFromProject(List<ComponentValidationMessage> messages, String outputName, String pathName) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IResource member = workspaceRoot.findMember(pathName);
        if (member == null || !member.exists()) {
            createDirDoesNotExistMessage(messages, pathName);
        } else {
            if (!(member instanceof IFolder)) {
                String text = StringUtils.format("'%s': '%s' is not a directory", outputName, pathName);
                messages.add(
                    new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, "", text, text, true));
            }
        }
    }

    private void validateDirFromFileSystem(List<ComponentValidationMessage> messages, String pathName) {
        File newFile = new File(pathName);
        if (!newFile.isDirectory()) {
            createDirDoesNotExistMessage(messages, pathName);
        }
    }

    private void createDirDoesNotExistMessage(List<ComponentValidationMessage> messages, String pathName) {
        String text = StringUtils.format(MISSING_PATH_MESSAGE, pathName);
        messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, "", text, text, true));
    }

}
