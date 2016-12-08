/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.gui.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.components.excel.common.ExcelAddress;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.components.excel.common.SimpleExcelService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager).
 *
 * @author Patrick Schaefer
 * @author Markus Kunde
 */
public class VariablesSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane[] allPanes;

    private Button buttonAutoDiscover;

    /**
     * @param genericEndpointTitle the display text describing individual endpoints (like "Input" or "Output"); used in dialog texts
     */
    public VariablesSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage,
        WorkflowNodeCommand.Executor executor) {
        super(title, direction, dynEndpointIdToManage, new String[] {}, new String[] {}, executor);
    }

    public EndpointSelectionPane[] getAllPanes() {
        return allPanes;
    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.allPanes = allPanes;
    }

    @Override
    public Control createControl(final Composite parent, String title, FormToolkit toolkit) {
        Control superControl = super.createControl(parent, title, toolkit);
        // empty label to get desired layout - feel free to improve
        new Label(client, SWT.READ_ONLY);
        buttonAutoDiscover = toolkit.createButton(client,
            Messages.autoDiscover, SWT.FLAT);
        buttonAutoDiscover.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        SelectionAdapter excelButtonListener = new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == buttonAutoDiscover) {
                    // TODO execute in separate thread

                    final List<WorkflowNodeCommand> commands = new ArrayList<>();

                    Job job = new Job(endpointType + " Autodiscover") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                monitor.beginTask("Starting", IProgressMonitor.UNKNOWN);

                                ExcelService excelService = new SimpleExcelService();

                                String excelFile =
                                    getConfiguration().getConfigurationDescription()
                                        .getConfigurationValue(ExcelComponentConstants.XL_FILENAME);
                                final File xlFile = ExcelUtils.getAbsoluteFile(excelFile);

                                String regex = null;
                                if (endpointType == EndpointType.INPUT) {
                                    regex = ExcelComponentConstants.DISCOVER_INPUT_REGEX;
                                } else {
                                    regex = ExcelComponentConstants.DISCOVER_OUTPUT_REGEX;
                                }

                                boolean successful = false;

                                if (excelService.isValidExcelFile(xlFile)) {
                                    List<String> visitedAdresses = new ArrayList<>();
                                    for (ExcelAddress addr : excelService.getUserDefinedCellNames(xlFile)) {
                                        if (addr.isUserDefindNameOfScheme(regex)) {
                                            Map<String, String> metaData = new HashMap<String, String>();
                                            metaData.put(ExcelComponentConstants.METADATA_ADDRESS, addr.getFullAddress());

                                            boolean exists = false;
                                            for (EndpointDescription endpoint : endpointManager.getEndpointDescriptions()) {
                                                if (endpoint.getName().equals(addr.getUserDefinedName())) {
                                                    exists = true;
                                                    break;
                                                }
                                            }

                                            if (!exists && !visitedAdresses.contains(addr.getFullAddress())) {
                                                visitedAdresses.add(addr.getFullAddress());
                                                final WorkflowNodeCommand command =
                                                    new AddDynamicEndpointCommand(endpointType, dynEndpointIdToManage,
                                                        addr.getUserDefinedName(), endpointManager
                                                            .getDynamicEndpointDefinition(ExcelPropertiesConstants.ID_INPUT_PANE)
                                                            .getDefaultDataType(),
                                                        metaData, allPanes);
                                                commands.add(command);
                                                successful = true;
                                            }
                                        }
                                    }
                                    visitedAdresses.clear();
                                    if (successful) {
                                        return Status.OK_STATUS;
                                    } else {
                                        return Status.CANCEL_STATUS;
                                    }
                                }
                                monitor.worked(1);
                                return null;
                            } finally {
                                monitor.done();
                            }
                        };
                    };
                    job.addJobChangeListener(new IJobChangeListener() {

                        @Override
                        public void done(IJobChangeEvent event) {
                            if (event.getResult() == Status.OK_STATUS) {
                                for (final WorkflowNodeCommand command : commands) {
                                    Display.getDefault().asyncExec(new Runnable() {

                                        @Override
                                        public void run() {
                                            execute(command);
                                        }
                                    });
                                }
                            } else if (event.getResult() == Status.CANCEL_STATUS) {
                                Display.getDefault().asyncExec(new Runnable() {

                                    @Override
                                    public void run() {
                                        MessageBox dialog = new MessageBox(parent.getShell(), SWT.ICON_WARNING);
                                        dialog.setText("Warning!");
                                        dialog.setMessage("No " + endpointType + " could be added!");
                                        dialog.open();
                                    }
                                });
                            }
                            commands.clear();
                        }

                        @Override
                        public void awake(IJobChangeEvent arg0) {}

                        @Override
                        public void aboutToRun(IJobChangeEvent arg0) {}

                        @Override
                        public void sleeping(IJobChangeEvent arg0) {}

                        @Override
                        public void scheduled(IJobChangeEvent arg0) {}

                        @Override
                        public void running(IJobChangeEvent arg0) {}
                    });

                    job.setUser(true);
                    job.schedule();

                }
            }
        };

        buttonAutoDiscover.addSelectionListener(excelButtonListener);
        buttonAdd.addPaintListener(new EnablingPaintListener(buttonAdd));
        buttonAutoDiscover.addPaintListener(new EnablingPaintListener(buttonAutoDiscover));

        return superControl;
    }

    @Override
    protected void onAddClicked() {

        String excelFile =
            getConfiguration().getConfigurationDescription()
                .getConfigurationValue(ExcelComponentConstants.XL_FILENAME);
        final File xlFile = ExcelUtils.getAbsoluteFile(excelFile);

        ExcelService excelService = new SimpleExcelService();
        if (excelFile != null && !excelFile.isEmpty() && excelService.isValidExcelFile(xlFile)) {
            EndpointEditDialog dialog =
                new VariablesEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD, configuration,
                    endpointType, dynEndpointIdToManage, false,
                    icon, endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                        .getMetaDataDefinition(),
                    new HashMap<String, String>(), xlFile);
            super.onAddClicked(dialog);
        } else {
            MessageBox box = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_WARNING);
            box.setMessage("Please select an Excel file in the \"File\" section!");
            box.setText("Warning");
            box.open();
        }

    }

    @Override
    protected void onEditClicked() {

        String excelFile =
            getConfiguration().getConfigurationDescription()
                .getConfigurationValue(ExcelComponentConstants.XL_FILENAME);
        final File xlFile = ExcelUtils.getAbsoluteFile(excelFile);

        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointEditDialog dialog =
            new VariablesEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.EDIT, configuration,
                endpointType, dynEndpointIdToManage, isStaticEndpoint,
                icon, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData, xlFile);

        super.onEditClicked(name, dialog, newMetaData);
    }

    /**
     * Paint listener that checks whether the buttons should be enabled whenever they are rendered.
     * 
     * @author Oliver Seebach
     */
    class EnablingPaintListener implements PaintListener {

        private Button buttonToEnable = null;

        EnablingPaintListener(Button button) {
            buttonToEnable = button;
        }

        @Override
        public void paintControl(PaintEvent event) {
            String excelFile = getConfiguration().getConfigurationDescription()
                .getConfigurationValue(ExcelComponentConstants.XL_FILENAME);
            ExcelService excelService = new SimpleExcelService();
            File xlFile = ExcelUtils.getAbsoluteFile(excelFile);
            if (excelFile == null || excelFile.isEmpty() || !excelService.isValidExcelFile(xlFile)) {
                buttonToEnable.setEnabled(false);
            } else {
                buttonToEnable.setEnabled(true);
            }
        }
    }
}
