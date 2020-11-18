/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.easymock.Capture;
import org.easymock.CaptureType;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;

// We deactivate Sonar on this class declaration, as it mistakenly identifies it as a JUnit test suite and asks us to add a test to it
final class WorkflowIntegrationServiceImplUnderTest extends WorkflowIntegrationServiceImpl { // NOSONAR

    private final File configurationTargetFile = new File("configuration.json");

    private final File workflowTargetFile = new File("workflow.wf");

    private final Capture<File> createParentDirectoriesParameters = Capture.newInstance(CaptureType.ALL);

    private final Capture<String> createdFiles = Capture.newInstance(CaptureType.ALL);

    private final Capture<Boolean> setFileWatcherActiveParameters = Capture.newInstance(CaptureType.ALL);

    private final ByteArrayOutputStream configurationFileOutputStream = new ByteArrayOutputStream();

    private final ByteArrayOutputStream workflowFileOutputStream = new ByteArrayOutputStream();

    private final Capture<String> toolIntegrationNameParameter = Capture.newInstance(CaptureType.ALL);

    private final Capture<ToolIntegrationContext> toolIntegrationContextParameter = Capture.newInstance(CaptureType.ALL);

    @Override
    protected InputStream createConfigurationTemplateStream() {
        final String configurationTemplate = "{\n"
            + "    \"launchSettings\" : [ {\n"
            + "        \"limitInstallationInstances\" : \"true\",\n"
            + "        \"host\" : \"RCE\",\n"
            + "        \"rootWorkingDirectory\" : \"\",\n"
            + "        \"toolDirectory\" : \".\",\n"
            + "        \"limitInstallationInstancesNumber\" : \"10\",\n"
            + "        \"version\" : \"0.0\"\n"
            + "  } ],\n"
            + "  \"integrationType\" : \"Workflow\"\n"
            + "}";
        return new ByteArrayInputStream(configurationTemplate.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void createParentDirectories(File file) {
        // Stubbed out for testing
        createParentDirectoriesParameters.setValue(file);
    }

    @Override
    protected OutputStream createFileOutputStream(File file) throws FileNotFoundException {
        if (file == this.workflowTargetFile) {
            return this.getWorkflowFileOutputStream();
        } else if (file == this.configurationTargetFile) {
            return this.getConfigurationFileOutputStream();
        }
        return new ByteArrayOutputStream();
    }

    @Override
    protected File createFile(String filename) {
        getCreatedFiles().setValue(filename);

        if (filename.endsWith("workflow.wf")) {
            return this.workflowTargetFile;
        } else if (filename.endsWith("configuration.json")) {
            return this.configurationTargetFile;
        } else {
            return new File(filename);
        }
    }

    ByteArrayOutputStream getWorkflowFileOutputStream() {
        return workflowFileOutputStream;
    }

    Capture<String> getCreatedFiles() {
        return createdFiles;
    }

    Capture<Boolean> getSetFileWatcherActiveParameters() {
        return setFileWatcherActiveParameters;
    }

    ByteArrayOutputStream getConfigurationFileOutputStream() {
        return configurationFileOutputStream;
    }

    Capture<String> getToolIntegrationNameParameter() {
        return toolIntegrationNameParameter;
    }

    Capture<ToolIntegrationContext> getToolIntegrationContextParameter() {
        return toolIntegrationContextParameter;
    }
}
