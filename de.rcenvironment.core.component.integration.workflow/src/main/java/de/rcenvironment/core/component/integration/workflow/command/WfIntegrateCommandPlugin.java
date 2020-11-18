/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationService;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

/**
 * A CommandPlugin that provides the command `wf integrate`.
 * 
 * @author Alexander Weinert
 */
@Component
public class WfIntegrateCommandPlugin implements CommandPlugin {

    private WorkflowIntegrationService workflowIntegrationService;

    private PersistentWorkflowDescriptionLoaderService workflowLoaderService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        return Arrays.asList(
            new CommandDescription("wf integrate", "<toolname> <workflow file>", false, "integrate a workflow file as a component"));
    }

    @Override
    public void execute(CommandContext commandContext) throws CommandException {
        commandContext.consumeExpectedToken("wf");
        commandContext.consumeExpectedToken("integrate");
        performWfIntegrate(commandContext);
    }

    private void performWfIntegrate(CommandContext context) throws CommandException {
        final boolean verbose = context.consumeNextTokenIfEquals("-v");

        final String componentname = context.consumeNextToken();
        if (componentname == null) {
            throw CommandException.syntaxError("Missing component name", context);
        }

        final Optional<String> idValidationError = ComponentIdRules.validateComponentIdRules(componentname);
        if (idValidationError.isPresent()) {
            throw CommandException.syntaxError("Invalid component ID", context);
        }

        final String filename = context.consumeNextToken();
        if (filename == null) {
            throw CommandException.missingFilename(context);
        }

        final WorkflowDescription desc;
        try {
            desc = workflowLoaderService.loadWorkflowDescriptionFromFile(new File(filename), null);
        } catch (WorkflowFileException e1) {
            throw CommandException.executionError(StringUtils.format("Workflow file at '%s' could not be parsed", filename), context);
        }

        final WfIntegrateCommandParser parser = new WfIntegrateCommandParser();
        final Collection<EndpointAdapter> unpackedParseResults = new LinkedList<>();
        final Collection<String> parseErrors = new LinkedList<>();
        while (context.hasRemainingTokens()) {
            final Collection<ParseResult<EndpointAdapter>> exposureParameters =
                parser.parseEndpointAdapterDefinition(context, desc);
            for (ParseResult<EndpointAdapter> parameter : exposureParameters) {
                if (parameter.isSuccessfulResult()) {
                    unpackedParseResults.add(parameter.getResult());
                } else {
                    parseErrors.add(parameter.getErrorDisplayMessage());
                }
            }
        }

        final String parseErrorMessage = parseErrors.stream()
            .collect(Collectors.joining("\n"));

        if (!parseErrorMessage.isEmpty()) {
            throw CommandException.syntaxError(StringUtils.format("Could not parse endpoint adapter definitions: \n%s", parseErrorMessage),
                context);
        }

        if (verbose) {
            unpackedParseResults.stream()
                .filter(definition -> definition.isInputAdapter())
                .map(definition -> definition.toString())
                .forEach(
                    definitionString -> context.getOutputReceiver().addOutput(StringUtils.format("Input Adapter : %s", definitionString)));

            unpackedParseResults.stream()
                .filter(definition -> definition.isOutputAdapter())
                .map(definition -> definition.toString())
                .forEach(
                    definitionString -> context.getOutputReceiver().addOutput(StringUtils.format("Output Adapter: %s", definitionString)));
        }

        // If we reach this point, we are certain that there are no errors during parsing
        final EndpointAdapters.Builder endpointAdapters = new EndpointAdapters.Builder();
        for (EndpointAdapter parseResult : unpackedParseResults) {
            endpointAdapters.addEndpointAdapter(parseResult);
        }

        try {
            workflowIntegrationService.integrateWorkflowFileAsComponent(desc, componentname, endpointAdapters.build());
        } catch (IOException e) {
            throw CommandException.executionError(
                StringUtils.format("Could not integrate workflow '%s' as component '%s'", filename, componentname), context);
        }
    }

    @Reference
    public void bindWorkflowIntegrationService(WorkflowIntegrationService service) {
        this.workflowIntegrationService = service;
    }

    @Reference
    public void bindWorkflowLoaderService(PersistentWorkflowDescriptionLoaderService service) {
        this.workflowLoaderService = service;
    }

}
