/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.utils.testing.CaptureMatchers;

final class WorkflowIntegrationServiceImplMatchers {
    private WorkflowIntegrationServiceImplMatchers() {};

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasIntegratedWorkflowComponent(String expectedComponentName) {

        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            @Override
            public void describeTo(Description description) {
                final String descriptionString =
                    String.format("has called ToolIntegrationService.registerRecursively(\"%s\", WorkflowIntegrationContext)",
                        expectedComponentName);
                description.appendText(descriptionString);
            }

            @Override
            protected void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description mismatchDescription) {
                final List<String> toolIntegrationNameParameters = service.getToolIntegrationNameParameter().getValues();
                if (!toolIntegrationNameParameters.contains(expectedComponentName)) {
                    final String mismatchDescriptionText = String.format(
                        "has not called ToolIntegrationService#registerRecursively with first parameter '%s'", expectedComponentName);
                    mismatchDescription.appendText(mismatchDescriptionText);
                } else {
                    final int indexOfParameter = service.getToolIntegrationNameParameter().getValues().indexOf(expectedComponentName);
                    final ToolIntegrationContext actualContext =
                        service.getToolIntegrationContextParameter().getValues().get(indexOfParameter);
                    final String mismatchDescriptionText =
                        String.format("has called ToolIntegrationService#registerRecursively(\"%s\", %s)", expectedComponentName,
                            actualContext);
                    mismatchDescription.appendText(mismatchDescriptionText);
                }
            }

            @Override
            protected boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                final List<String> toolIntegrationNameParameters = service.getToolIntegrationNameParameter().getValues();
                if (!toolIntegrationNameParameters.contains(expectedComponentName)) {
                    return false;
                }
                final int indexOfParameter = service.getToolIntegrationNameParameter().getValues().indexOf(expectedComponentName);
                return service.getToolIntegrationContextParameter().getValues().get(indexOfParameter) instanceof WorkflowIntegrationContext;
            }
        };
    }

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasDisabledFileWatcherDuringIntegration() {

        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(
                    "has dis- and subsequently enabled filewatchers by calling ToolIntegrationService#setFileWatcherActive");
            }

            @Override
            protected void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description mismatchDescription) {
                final List<Boolean> parameters = service.getSetFileWatcherActiveParameters().getValues();
                if (parameters.size() == 0) {
                    mismatchDescription.appendText("has not called ToolIntegrationService#setFileWatcherActive at all");
                } else if (parameters.size() == 1) {
                    if (Boolean.TRUE.equals(parameters.get(0))) {
                        mismatchDescription.appendText("has only called ToolIntegrationService#setFileWatcherActive(true)");
                    } else {
                        mismatchDescription.appendText("has only called ToolIntegrationService#setFileWatcherActive(false)");
                    }
                } else if (parameters.size() == 2) {
                    mismatchDescription.appendText(
                        String.format("has called ToolIntegrationService#setFileWatcherActive with parameters %s and %s",
                            parameters.get(0), parameters.get(1)));
                } else if (parameters.size() >= 3) {
                    mismatchDescription.appendText("has called ToolIntegrationService#setFileWatcherActive more than twice");
                }
            }

            @Override
            protected boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                final List<Boolean> parameters = service.getSetFileWatcherActiveParameters().getValues();
                return parameters.size() == 2 && Boolean.FALSE.equals(parameters.get(0)) && Boolean.TRUE.equals(parameters.get(1));
            }

        };
    }

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasWrittenKeyValueToWorkflowFile(String key, String value) {
        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("has written key-value-pair {\"%s\" : \"%s\"} to configuration file", key, value));
            }

            @Override
            protected void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description mismatchDescription) {
                mismatchDescription.appendText(String.format("does not contain key-value pair {\"%s\":\"%s\"}", key, value));

            }

            @Override
            protected boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                final String contentWrittenToConfigurationFile =
                    new String(service.getWorkflowFileOutputStream().toByteArray(), StandardCharsets.UTF_8);
                try {
                    final Map<String, Object> writtenWorkflowFile = parseWorkflowFile(contentWrittenToConfigurationFile);
                    return writtenWorkflowFile.containsKey(key) && value.equals(writtenWorkflowFile.get(key));
                } catch (JsonProcessingException e) {
                    return false;
                }
            }

            @SuppressWarnings("unchecked")
            private Map<String, Object> parseWorkflowFile(final String contentWrittenToConfigurationFile)
                throws JsonProcessingException, JsonMappingException {
                return new ObjectMapper().readValue(contentWrittenToConfigurationFile, new HashMap<String, Object>().getClass());
            }

        };
    }

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasWrittenKeyValueToConfigurationFile(String key, String value) {
        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("has written key-value-pair {\"%s\" : \"%s\"} to configuration file", key, value));
            }

            @Override
            protected void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description mismatchDescription) {
                mismatchDescription.appendText(String.format("does not contain key-value pair {\"%s\":\"%s\"}", key, value));

            }

            @Override
            protected boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                final String contentWrittenToConfigurationFile =
                    new String(service.getConfigurationFileOutputStream().toByteArray(), StandardCharsets.UTF_8);
                try {
                    final Map<String, Object> writtenWorkflowFile = parseWorkflowFile(contentWrittenToConfigurationFile);
                    return writtenWorkflowFile.containsKey(key) && value.equals(writtenWorkflowFile.get(key));
                } catch (JsonProcessingException e) {
                    return false;
                }
            }

            @SuppressWarnings("unchecked")
            private Map<String, Object> parseWorkflowFile(final String contentWrittenToConfigurationFile)
                throws JsonProcessingException, JsonMappingException {
                return new ObjectMapper().readValue(contentWrittenToConfigurationFile, new HashMap<String, Object>().getClass());
            }

        };
    }

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasWrittenEmptyArrayToConfigurationFile(String key) {
        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("has written key-value-pair {\"%s\" : []} to configuration file", key));
            }

            @Override
            protected void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description mismatchDescription) {
                mismatchDescription.appendText(String.format("does not contain key-value pair {\"%s\":[]}", key));

            }

            @Override
            protected boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                final String contentWrittenToConfigurationFile =
                    new String(service.getConfigurationFileOutputStream().toByteArray(), StandardCharsets.UTF_8);
                return removeWhitespace(contentWrittenToConfigurationFile).contains(String.format("\"%s\":[]", key));
            }
        };
    }

    public static Matcher<WorkflowIntegrationServiceImplUnderTest> hasCreatedFilesInToolIntegrationDirectory(String... args) {
        return new TypeSafeMatcher<WorkflowIntegrationServiceImplUnderTest>() {

            // We ignore the conversion warning from Matcher[] to Matcher<String>[] since explicitly creating a Matcher<String>[] would
            // require us to do so explicitly in an extracted method instead of inline in this stream, thus decreasing readability
            @SuppressWarnings("unchecked")
            private final Matcher<Capture<String>> captureMatcher = CaptureMatchers.hasCapturedInAnyOrder(
                Arrays.asList(args).stream()
                    .map(arg -> CoreMatchers.endsWith(getWorkflowIntegrationDirectory() + File.separator + arg))
                    .toArray(Matcher[]::new));

            @Override
            public boolean matchesSafely(WorkflowIntegrationServiceImplUnderTest service) {
                return captureMatcher.matches(service.getCreatedFiles());
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("has created files ");
                arg0.appendValueList("", " and ", "", args);
                arg0.appendText(" in directory " + getWorkflowIntegrationDirectory());
            }

            @Override
            public void describeMismatchSafely(WorkflowIntegrationServiceImplUnderTest service, Description description) {
                if (!service.getCreatedFiles().hasCaptured()) {
                    description.appendText("has not created any files");
                } else {
                    description.appendText("has created files ");
                    description.appendValueList("", ", ", "", service.getCreatedFiles().getValues());
                }
            }

            public String getWorkflowIntegrationDirectory() {
                return "tools" + File.separator + "workflow";
            }
        };
    }

    private static String removeWhitespace(final String string) {
        return string
            .replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "");
    }

}
