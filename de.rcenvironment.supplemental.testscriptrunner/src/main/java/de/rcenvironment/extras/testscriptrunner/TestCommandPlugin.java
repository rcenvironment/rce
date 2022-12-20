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
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.FileParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedFileParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
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
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription testCommands = new MainCommandDescription(ROOT_COMMAND, "test commands", "test commands", true,
            new SubCommandDescription(OPEN_VIEW_COMMAND,  "opens a GUI view and sets focus to it.", this::handleOpenViewCommand,
                new CommandModifierInfo(new AbstractCommandParameter[] { new StringParameter(null, "open view parameter",
                    "one of the following view keys is a valid input:" + viewIds.keySet().toString())
                }), true
            ),
            new SubCommandDescription(CLOSE_VIEW_COMMAND, "closes a GUI view.", this::handleCloseViewCommand,
                new CommandModifierInfo(new AbstractCommandParameter[] { new StringParameter(null, "close view parameter",
                    "one of the following view keys is a valid input:" + viewIds.keySet().toString())
                }), true
            ),
            new SubCommandDescription(CLOSE_WELCOME_SCREEN, "closes the welcome screen if present.", this::closeWelcomeScreen, true),
            new SubCommandDescription(EXPORT_WORKFLOW_RUN, "exports the run corresponding to the workflowtitle to the given directory.",
                this::exportWorkflow,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        new StringParameter(null, "absolute directory path", "absolute path to directory"),
                        new StringParameter(null, "workflow title", "title of the workflow")
                    }
                ), true
            ),
            new SubCommandDescription(COMPARE_TWO_WORKFLOW_RUNS, "compares the two given workflowruns and indicates if they are"
                + " identical or wether their are differences.", this::compareWorkflowRuns,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        new FileParameter("abs export workflow path", "absolute path to exported workflowrun"),
                        new FileParameter("abs export workflow path", "absolute path to exported workflowrun")
                    }
                ), true
            ),
            new SubCommandDescription(EXPORT_ALL_WORKFLOW_RUNS, "exports all workflow runs into the given export directory.", this::exportAllWorkflows,
                new CommandModifierInfo(new AbstractCommandParameter[] { new StringParameter(null, "abs path to export dir", "absolute path to export directory") }), true
            )
        );
        
        return new MainCommandDescription[] { testCommands };
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

        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter dirParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        
        File exportDirectory = new File(dirParameter.getResult());
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

        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter dirParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter workflowTitleParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        String dirArgument = dirParameter.getResult();
        String workflowTitle = workflowTitleParameter.getResult();

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
        
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedFileParameter wfRun1Parameter = (ParsedFileParameter) modifiers.getPositionalCommandParameter(0);
        ParsedFileParameter wfRun2Parameter = (ParsedFileParameter) modifiers.getPositionalCommandParameter(0);
        File wfRun1 = wfRun1Parameter.getResult();
        File wfRun2 = wfRun2Parameter.getResult();

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

        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter viewIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        String viewId = viewIdParameter.getResult();
        
        if (viewIds.containsKey(viewId)) {
            openView(viewId);
        } else {
            throw CommandException.syntaxError("No view associated to passed argument.", context);
        }
    }

    private void handleCloseViewCommand(CommandContext context) throws CommandException {
        
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter viewIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        String viewId = viewIdParameter.getResult();
        
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
