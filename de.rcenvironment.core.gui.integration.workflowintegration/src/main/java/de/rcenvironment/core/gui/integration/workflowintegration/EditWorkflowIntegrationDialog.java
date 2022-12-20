/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.gui.integration.common.IntegrationHelper;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.EditWorkflowIntegrationHandler;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Dialog for selecting an integrated workflow to edit.
 * 
 * @author Kathrin Schaffert
 */
public class EditWorkflowIntegrationDialog extends TitleAreaDialog {

    private static final int LIST_HEIGHT = 200;

    private static final int LIST_WIDTH = 300;

    private String[] activeIds;

    private String[] inactiveIds;

    private List workflowList;

    private List workflowActivationList;

    private IntegrationHelper integrationHelper = new IntegrationHelper();

    private ToolIntegrationContextRegistry integrationContextRegistry;

    public EditWorkflowIntegrationDialog(Shell parent) {
        super(parent);

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        integrationContextRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        readExistingConfigurations();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Edit an integrated Workflow");
    }

    @Override
    public void create() {
        super.create();
        setTitle("Choose Workflow Configuration");
        setMessage("Edit an active workflow configuration or activate an inactive one.");
        getButton(OK).setEnabled(false);

    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g = new GridData(GridData.FILL_BOTH);
        g.grabExcessHorizontalSpace = true;
        container.setLayoutData(g);

        new Label(container, SWT.NONE).setText("Choose workflow configuration to edit: ");

        ListViewer viewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setInput(activeIds);

        workflowList = viewer.getList();
        GridData workflowListData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        workflowListData.widthHint = LIST_WIDTH;
        workflowListData.heightHint = LIST_HEIGHT;
        workflowList.setLayoutData(workflowListData);
        workflowList.addSelectionListener(new EditWorkflowSelectionListener());
        workflowList.addMouseListener(new EditWorkflowMouseListener());

        new Label(container, SWT.NONE).setText("Choose inactive workflow configuration to edit: ");
        
        ListViewer inactiveComponentsViewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
        inactiveComponentsViewer.setContentProvider(new ArrayContentProvider());
        inactiveComponentsViewer.setInput(inactiveIds);

        workflowActivationList = inactiveComponentsViewer.getList();
        GridData activateListData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        activateListData.widthHint = LIST_WIDTH;
        activateListData.heightHint = LIST_HEIGHT;
        workflowActivationList.setLayoutData(activateListData);
        workflowActivationList.addSelectionListener(new EditWorkflowSelectionListener());
        workflowActivationList.addMouseListener(new EditWorkflowMouseListener());

        return container;
    }

    private class EditWorkflowSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {

            Object src = arg0.getSource();
            if (src instanceof List) {
                List list = (List) src;
                int index = list.getSelectionIndex();
                workflowList.deselectAll();
                workflowActivationList.deselectAll();
                list.setSelection(index);
                getButton(OK).setEnabled(true);
            }

        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    private class EditWorkflowMouseListener implements MouseListener {

        @Override
        // no implementation needed
        public void mouseUp(MouseEvent arg0) {}

        @Override
        public void mouseDown(MouseEvent arg0) {
            // no implementation needed
        }

        @Override
        public void mouseDoubleClick(MouseEvent arg0) {
            okPressed();
        }
    }

    @Override
    protected void okPressed() {
        String workflowName;
        if (workflowActivationList.isEnabled() && workflowActivationList.getSelectionCount() == 1) {
            int i = workflowActivationList.getSelectionIndex();
            workflowName = workflowActivationList.getItem(i);
        } else if (workflowList.getSelectionCount() == 1) {
            int i = workflowList.getSelectionIndex();
            workflowName = workflowList.getItem(i);
        } else {
            return;
        }

        EditWorkflowIntegrationHandler handler = new EditWorkflowIntegrationHandler(workflowName);
        try {
            handler.execute(null);
        } catch (ExecutionException e) {
            LogFactory.getLog(EditWorkflowIntegrationDialog.class).error("Opening Edit Workflow failed", e);
        }

        super.okPressed();
    }

    @SuppressWarnings("unchecked")
    private void readExistingConfigurations() {

        Set<String> activeIdSet = new HashSet<>();
        Set<String> inactiveIdSet = new HashSet<>();

        IntegrationContext context = integrationContextRegistry.getToolIntegrationContextByType(IntegrationContextType.WORKFLOW.toString());
        String integrationContextName = context.getNameOfToolIntegrationDirectory();
        String rootPath = context.getRootPathToToolIntegrationDirectory();
        File workflowIntegrationDir = new File(rootPath, integrationContextName);

        if (!workflowIntegrationDir.isDirectory()) {
            return;
        }

        for (File workflowDir : workflowIntegrationDir.listFiles()) {
            if (!workflowDir.isDirectory()) {
                continue;
            }
            final Optional<File> configFile = integrationHelper.tryFindConfigurationFile(context, workflowDir);
            if (!configFile.isPresent()) {
                continue;
            }
            Map<String, Object> map;
            try {
                map = JsonUtils.getDefaultObjectMapper().readValue(configFile.get(), HashMap.class);
            } catch (IOException e) {
                throw new RuntimeException(StringUtils.format("Error reading the configuration file \"%s\"", configFile.get().toString()),
                    e);
            }
            if (!map.containsKey(IntegrationConstants.KEY_COMPONENT_NAME)) {
                continue;
            }
            if (map.get(IntegrationConstants.IS_ACTIVE) == null || map.get(IntegrationConstants.IS_ACTIVE).equals(Boolean.TRUE)) {
                activeIdSet.add((String) map.get(IntegrationConstants.KEY_COMPONENT_NAME));
            } else {
                inactiveIdSet.add((String) map.get(IntegrationConstants.KEY_COMPONENT_NAME));
            }
        }

        this.activeIds = activeIdSet.toArray(new String[0]);
        this.inactiveIds = inactiveIdSet.toArray(new String[0]);
        Arrays.sort(activeIds, String.CASE_INSENSITIVE_ORDER);
        Arrays.sort(inactiveIds, String.CASE_INSENSITIVE_ORDER);
    }

}
