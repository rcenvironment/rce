/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.export.matching.BooleanTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.DateTimeTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.DirectoryReferenceTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.FileReferenceTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.FloatTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.IntegerTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.MatchResult;
import de.rcenvironment.core.datamanagement.export.matching.Matcher;
import de.rcenvironment.core.datamanagement.export.matching.MatrixTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.NotAValueMatcher;
import de.rcenvironment.core.datamanagement.export.matching.ShortTextTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.SmallTableTDMatcher;
import de.rcenvironment.core.datamanagement.export.matching.VectorTDMatcher;
import de.rcenvironment.core.datamanagement.export.objects.PlainWorkflowRun;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * A {@link CommandPlugin} that provides test commands. These are used to fulfill BDD test steps.
 * 
 * @author Tobias Brieden (based on code by)
 * @author Marlon Schroeter
 */
@Component
public class TestCommandPlugin implements CommandPlugin {

    private static final String ROOT_COMMAND = "tc"; // short for test command. Input for another root command is appreciated.

    private static final String OPEN_VIEW_COMMAND = "open_view";

    private static final String CLOSE_VIEW_COMMAND = "close_view";

    private static final String CLOSE_WELCOME_SCREEN = "close_welcome";

    private static final String EXPORT_ALL_WORKFLOW_RUNS = "export_all_wf_runs";

    private static final String EXPORT_WORKFLOW_RUN = "export_wf_run";

    private static final String COMPARE_TWO_WORKFLOW_RUNS = "compare_wf_runs";

    private Map<String, String> viewIds = new HashMap<>();
    
    private MetaDataService metaDataService;

    private Map<DataType, Matcher> matchers = new HashMap<>();

    // we do not use the JsonUtils.getDefaultObjectMapper() method, as it returns the mapper from an older Jackson version
    private ObjectMapper mapper = new ObjectMapper();

    private ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);

    public TestCommandPlugin() {
        fillViewIdMap();
        fillMatcherMap();
    }
    
    /**
     * bind method for OSGi.
     * @param service metadataservice
     */
    @Reference
    public void bindMetaDataService(MetaDataService service) {
        this.metaDataService = service;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(ROOT_COMMAND);

        String subCommand = context.consumeNextToken();
        switch (subCommand) {
        case OPEN_VIEW_COMMAND:
            handleOpenViewCommand(context);
            break;
        case CLOSE_VIEW_COMMAND:
            handleCloseViewCommand(context);
            break;
        case CLOSE_WELCOME_SCREEN:
            closeWelcomeScreen(context);
            break;
        case EXPORT_ALL_WORKFLOW_RUNS:
            exportAllWorkflows(context);
            break;
        case EXPORT_WORKFLOW_RUN:
            exportWorkflow(context);
            break;
        case COMPARE_TWO_WORKFLOW_RUNS:
            compareWorkflowRuns(context);
            break;
        default:
            throw CommandException.unknownCommand(context);
        }
    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, OPEN_VIEW_COMMAND),
                "<view>",
                true,
                "opens a GUI view and sets focus to it.",
                "<view> : one of the following view keys is a valid input:",
                viewIds.keySet().toString()));

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, CLOSE_VIEW_COMMAND),
                "(<view>|all)",
                true,
                "closes a GUI view.",
                "<view> : one of the following view keys is a valid input:",
                "\t" + viewIds.keySet().toString()));

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, CLOSE_WELCOME_SCREEN),
                "",
                true,
                "closes the welcome screen if present."));

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, EXPORT_WORKFLOW_RUN),
                "<absolute path to directory> <workflowtitle>",
                true,
                "exports the run corresponding to the workflowtitle to the given directory."));

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, COMPARE_TWO_WORKFLOW_RUNS),
                "<absolute path to exported workflowrun> <absolute path to exported workflowrun>",
                true,
                "compares the two given workflowruns and indicates if they are identical or wether their are differences."));

        contributions
            .add(new CommandDescription(
                StringUtils.format(StepDefinitionConstants.COMMAND_DESCRIPTION_FORMAT, ROOT_COMMAND, EXPORT_ALL_WORKFLOW_RUNS),
                "<absolute path to export directory>",
                true,
                "exports all workflow runs into the given export directory."));

        return contributions;
    }

    private void closeWelcomeScreen(CommandContext context) {
        new UIJob("Closing welcome screen") {

            @Override
            public IStatus runInUIThread(IProgressMonitor progressMonitor) {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IViewReference[] viewReferences = page.getViewReferences();

                for (IViewReference viewRef : viewReferences) {
                    if (viewRef.getId().equals("org.eclipse.ui.internal.introview")) {
                        closeView("org.eclipse.ui.internal.introview");
                    }
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void exportAllWorkflows(CommandContext context) throws CommandException {
        String dirArgument = context.consumeNextToken();
        if (dirArgument == null) {
            throw CommandException.syntaxError("Not enough arguments.", context);
        }
        noMoreTokens(context);

        File exportDirectory = new File(dirArgument);
        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs();
        }

        try {
            for (WorkflowRunDescription wfRunDesc : metaDataService.getWorkflowRunDescriptions()) {
                WorkflowRun workflowRun =
                    metaDataService.getWorkflowRun(wfRunDesc.getWorkflowRunID(), wfRunDesc.getStorageLogicalNodeId());

                // convert the workflow run
                TypedDatumSerializer serializer = serviceRegistryAccess.getService(TypedDatumService.class).getSerializer();
                PlainWorkflowRun plainWorkflowRun = new PlainWorkflowRun(workflowRun, serializer);

                File targetFile = new File(exportDirectory, wfRunDesc.getWorkflowTitle().replaceAll(":", "_") + ".json");

                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(targetFile, plainWorkflowRun);
            }
        } catch (IOException | CommunicationException e) {
            throw CommandException.executionError(StringUtils.format("Failed to export all workflow runs: %s", e), context);
        }
        
        context.println("Successfully exported all workflow runs.");
    }

    /**
     * exporting workflow to json file. Used for testing purposes.
     * 
     * @throws CommandException
     */
    private void exportWorkflow(CommandContext context) throws CommandException {
        String dirArgument = context.consumeNextToken();
        String workflowTitle = context.consumeNextToken();

        if (dirArgument == null || workflowTitle == null) {
            throw CommandException.syntaxError("Not enough arguments.", context);
        }

        noMoreTokens(context);

        File targetDirectory = new File(dirArgument);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        File targetFile = new File(targetDirectory, workflowTitle.replaceAll(":", "_") + ".json");
        
        try {
            // retrieve the workflow run from the data management
            boolean foundCorrespondingWfRunId = false;
            for (WorkflowRunDescription wfRunDesc : metaDataService.getWorkflowRunDescriptions()) {
                if (workflowTitle.equals(wfRunDesc.getWorkflowTitle())) {
                    foundCorrespondingWfRunId = true;
                    WorkflowRun workflowRun =
                        metaDataService.getWorkflowRun(wfRunDesc.getWorkflowRunID(), wfRunDesc.getStorageLogicalNodeId());

                    // convert the workflow run
                    TypedDatumSerializer serializer = serviceRegistryAccess.getService(TypedDatumService.class).getSerializer();
                    PlainWorkflowRun plainWorkflowRun = new PlainWorkflowRun(workflowRun, serializer);

                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, plainWorkflowRun);

                    context.println(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_EXPORT);

                    break;
                }
            }

            if (!foundCorrespondingWfRunId) {
                throw CommandException
                    .executionError(StringUtils.format("Failed to find workflow corresponding to wf title \"%s\"", workflowTitle), context);
            }

        } catch (IOException | CommunicationException e) {
            throw CommandException.executionError(StringUtils.format("Failed to export workflow run: %s", e), context);
        }
    }

    private void compareWorkflowRuns(CommandContext context) throws CommandException {

        File wfRun1 = new File(context.consumeNextToken());
        File wfRun2 = new File(context.consumeNextToken());

        noMoreTokens(context);

        try {
            PlainWorkflowRun plainWorkflowRun1 = mapper.readValue(wfRun1, PlainWorkflowRun.class);
            PlainWorkflowRun plainWorkflowRun2 = mapper.readValue(wfRun2, PlainWorkflowRun.class);

            MatchResult result = plainWorkflowRun1.matches(matchers, plainWorkflowRun2);

            if (result.hasMatched()) {
                context.println(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_COMPARISON_IDENTICAL);
            } else {
                context.println(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_COMPARISON_DIFFERENT);
                context.println(result);
            }
        } catch (IOException e) {
            throw CommandException.executionError("Could not parse passed files.", context);
        }
    }

    private void handleOpenViewCommand(CommandContext context) throws CommandException {
        String viewId = context.consumeNextToken();

        noMoreTokens(context);

        if (viewIds.containsKey(viewId)) {
            openView(viewId);
        } else {
            throw CommandException.syntaxError("No view associated to passed argument.", context);
        }
    }

    private void handleCloseViewCommand(CommandContext context) throws CommandException {
        String viewId = context.consumeNextToken();

        noMoreTokens(context);

        if (viewIds.containsKey(viewId)) {
            closeView(viewIds.get(viewId));
        } else {
            throw CommandException.syntaxError("No view associated with passed argument.", context);
        }
    }

    private void openView(String viewKey) {
        new UIJob(StringUtils.format("Opening view %s", viewKey)) {

            @Override
            public IStatus runInUIThread(IProgressMonitor progressMonitor) {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewIds.get(viewKey));
                } catch (PartInitException e) {
                    return Status.CANCEL_STATUS;
                    // TODO handle error and display that opening the view was not possible
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void closeView(String viewId) {
        new UIJob(StringUtils.format("Closing view %s", viewId)) {

            @Override
            public IStatus runInUIThread(IProgressMonitor progressMonitor) {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IViewReference[] viewReferences = page.getViewReferences();

                for (IViewReference viewRef : viewReferences) {
                    if (viewRef.getId().equals(viewId)) {
                        page.hideView(viewRef.getView(true));

                        return Status.OK_STATUS;
                    }
                }
                return Status.CANCEL_STATUS;
            }
        }.schedule();
    }

    // TODO causes exception after having closed the last view. Not clear why.
    @SuppressWarnings("unused")
    private void closeAllViews() {
        new UIJob("Closing all views") {

            @Override
            public IStatus runInUIThread(IProgressMonitor progressMonitor) {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IViewReference[] viewReferences = page.getViewReferences();

                for (IViewReference viewRef : viewReferences) {
                    if (viewIds.containsValue(viewRef.getId())) {
                        closeView(viewRef.getId());
                    }
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void noMoreTokens(CommandContext context) throws CommandException {
        if (context.hasRemainingTokens()) {
            throw CommandException.syntaxError("Too many arguments.", context);
        }
    }

    private void fillViewIdMap() {
        viewIds.put("Component_Publishing", "de.rcenvironment.core.gui.authorization.ComponentPublishingView");
        viewIds.put("Cluster_Job_Monitor", "de.rcenvironment.core.gui.cluster.view.ClusterJobMonitorView");
        viewIds.put("Command_Console", "de.rcenvironment.core.gui.command.CommandConsoleViewer");
        viewIds.put("CPACS_Writer", "de.rcenvironment.components.cpacs.writer.gui.runtime.CpacsGeomView");
        viewIds.put("Data_Management_Browser", "de.rcenvironment.rce.gui.datamanagement.browser.DataManagementBrowser");
        viewIds.put("Excel", "de.rcenvironment.components.excel.gui.view.ExcelView");
        viewIds.put("Log", "de.rcenvironment.core.gui.log.LogView");
        viewIds.put("Network", "de.rcenvironment.core.gui.communication.views.NetworkView");
        viewIds.put("Optimizer", "de.rcenvironment.components.optimizer.gui.view.OptimizerView");
        viewIds.put("Parametric_Study", "de.rcenvironment.components.parametricstudy.gui.view.ParametricStudyView");
        viewIds.put("Properties", "org.eclipse.ui.views.PropertySheet");
        viewIds.put("TIGL_Viewer", "de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer");
        viewIds.put("Timeline", "de.rcenvironment.gui.Timeline");
        viewIds.put("Workflow_List", "de.rcenvironment.gui.workflowList");
        viewIds.put("Workflow_Console", "de.rcenvironment.gui.WorkflowComponentConsole");
    }

    private void fillMatcherMap() {
        // TODO add matcher for all supported data types in RCE
        matchers.put(DataType.Boolean, new BooleanTDMatcher());
        matchers.put(DataType.Integer, new IntegerTDMatcher());
        matchers.put(DataType.Float, new FloatTDMatcher());
        matchers.put(DataType.NotAValue, new NotAValueMatcher());
        matchers.put(DataType.ShortText, new ShortTextTDMatcher());
        matchers.put(DataType.DateTime, new DateTimeTDMatcher());
        matchers.put(DataType.DirectoryReference, new DirectoryReferenceTDMatcher());
        matchers.put(DataType.FileReference, new FileReferenceTDMatcher());
        matchers.put(DataType.Matrix, new MatrixTDMatcher());
        matchers.put(DataType.Vector, new VectorTDMatcher());
        matchers.put(DataType.SmallTable, new SmallTableTDMatcher());
    }
}
