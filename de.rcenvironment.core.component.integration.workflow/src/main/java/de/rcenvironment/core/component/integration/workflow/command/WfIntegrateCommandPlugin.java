/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedListParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
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

    private static final String SYNTAX_DESCRIPTION = "ComponentName:OutputName:ExposedName,";
    
    private static final String DESCRIPTION_TEXT = "elements of the workflow to expose (see User Guide)";
    
    private static final CommandFlag VERBOSE_FLAG = new CommandFlag("-v", "--verbose", "enable verbose output");
    
    private static final StringParameter TOOLNAME_PARAMETER = new StringParameter(null, "toolname", "name for the tool");
    
    private static final StringParameter WORKFLOW_FILE_PARAMETER = new StringParameter(null, "workflow file",
            "workflow to be integrated");
    
    private static final StringParameter ELEMENT_PARAMETER = new StringParameter(null, "element", "element to be exposed");
    
    private static final ListCommandParameter EXPOSE_LIST_PARAMETER = new ListCommandParameter(ELEMENT_PARAMETER,
            SYNTAX_DESCRIPTION, DESCRIPTION_TEXT);
    
    private static final NamedParameter NAMED_EXPOSE_PARAMETER = new NamedSingleParameter("--expose",
            DESCRIPTION_TEXT, EXPOSE_LIST_PARAMETER);
    
    private static final NamedParameter NAMED_EXPOSE_INPUTS_PARAMETER = new NamedSingleParameter("--expose-inputs",
            DESCRIPTION_TEXT, EXPOSE_LIST_PARAMETER);
    
    private static final NamedParameter NAMED_EXPOSE_OUTPUTS_PARAMETER = new NamedSingleParameter("--expose-outputs",
            DESCRIPTION_TEXT, EXPOSE_LIST_PARAMETER);

    private WorkflowIntegrationService workflowIntegrationService;

    private PersistentWorkflowDescriptionLoaderService workflowLoaderService;

    @Override
    public MainCommandDescription[] getCommands() {
        return new MainCommandDescription[] {
            new MainCommandDescription("wf-integrate", "integrate a workflow file as a component",
                "integrate a workflow file as a component", this::performWfIntegrate,
                new CommandModifierInfo(new AbstractCommandParameter[]
                    { TOOLNAME_PARAMETER, WORKFLOW_FILE_PARAMETER },
                    new CommandFlag[] { VERBOSE_FLAG },
                    new NamedParameter[] {
                        NAMED_EXPOSE_PARAMETER,
                        NAMED_EXPOSE_INPUTS_PARAMETER,
                        NAMED_EXPOSE_OUTPUTS_PARAMETER
                    }
                )
            )
        };
    }

    private void performWfIntegrate(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        final boolean verbose = modifiers.hasCommandFlag("-v");
        final ParsedStringParameter toolnameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        final ParsedStringParameter workflowFileParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        
        final ParsedListParameter exportListParameter = (ParsedListParameter) modifiers.getCommandParameter("--expose");
        final List<String> exportListValues = exportListParameter.getResult().stream()
                .map(parameter -> (String) parameter.getResult()).collect(Collectors.toList());
        final ParsedListParameter exportInputsListParameter = (ParsedListParameter) modifiers.getCommandParameter("--expose-inputs");
        final List<String> exportInputsListValues = exportInputsListParameter.getResult()
                .stream().map(parameter -> (String) parameter.getResult()).collect(Collectors.toList());
        final ParsedListParameter exportOutputsListParameter = (ParsedListParameter) modifiers.getCommandParameter("--expose-outputs");
        final List<String> exportOutputsListValues = exportOutputsListParameter.getResult().stream()
                .map(parameter -> (String) parameter.getResult()).collect(Collectors.toList());
        
//        if (exportListValues.isEmpty() && exportInputListValues.isEmpty() && exportInputsListValues.isEmpty()
//                && exportOutputListValues.isEmpty() && exportOutputsListValues.isEmpty()) {
//            throw CommandException.syntaxError("No export defined", context);
//        }
        
        final String componentname = toolnameParameter.getResult();
        final Optional<String> idValidationError = ComponentIdRules.validateComponentIdRules(componentname);
        if (idValidationError.isPresent()) {
            throw CommandException.syntaxError("Invalid component ID", context);
        }

        final WorkflowDescription desc;
        try {
            desc = workflowLoaderService.loadWorkflowDescriptionFromFile(new File(workflowFileParameter.getResult()), null);
        } catch (WorkflowFileException e1) {
            throw CommandException.executionError(StringUtils.format("Workflow file at '%s' could not be parsed",
                    workflowFileParameter.getResult()), context);
        }

        final WfIntegrateCommandParser parser = new WfIntegrateCommandParser();
        final Collection<EndpointAdapter> unpackedParseResults = new LinkedList<>();
        final Collection<String> parseErrors = new LinkedList<>();
        
        parseListParameter(exportListValues, "--expose", parser, desc, unpackedParseResults, parseErrors);
        parseListParameter(exportInputsListValues, "--expose-inputs", parser, desc, unpackedParseResults, parseErrors);
        parseListParameter(exportOutputsListValues, "--expose-outputs", parser, desc, unpackedParseResults, parseErrors);
        
        final String parseErrorMessage = parseErrors.stream()
            .collect(Collectors.joining("\n"));

        if (!parseErrorMessage.isEmpty()) {
            throw CommandException.syntaxError(StringUtils.format("Could not parse endpoint adapter definitions: \n%s", parseErrorMessage),
                context);
        }

        if (verbose) {
            unpackedParseResults.stream()
                .filter(EndpointAdapter::isInputAdapter)
                .map(EndpointAdapter::toString)
                .forEach(
                    definitionString -> context.getOutputReceiver().addOutput(StringUtils.format("Input Adapter : %s", definitionString)));

            unpackedParseResults.stream()
                .filter(EndpointAdapter::isOutputAdapter)
                .map(EndpointAdapter::toString)
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
                StringUtils.format("Could not integrate workflow '%s' as component '%s'",
                        workflowFileParameter.getResult(), componentname), context);
        }
    }
    
    private void parseListParameter(List<String> values, String type, WfIntegrateCommandParser parser, WorkflowDescription desc, Collection<EndpointAdapter> unpackedParseResults, Collection<String> parseErrors) {
        for (int i = 0; i < values.size(); i++) {
            final Collection<ParseResult<EndpointAdapter>> exposureParameters =
                    parser.parseEndpointAdapterDefinition(type, values.get(i), desc);
            for (ParseResult<EndpointAdapter> parameter : exposureParameters) {
                if (parameter.isSuccessfulResult()) {
                    unpackedParseResults.add(parameter.getResult());
                } else {
                    parseErrors.add(parameter.getErrorDisplayMessage());
                }
            }
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
