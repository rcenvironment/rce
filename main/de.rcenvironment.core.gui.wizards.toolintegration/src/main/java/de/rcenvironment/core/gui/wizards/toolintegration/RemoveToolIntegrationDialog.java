/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;

/**
 * Dialog for removing an integrated tool configuration.
 * 
 * @author Sascha Zur
 */
public class RemoveToolIntegrationDialog extends Dialog {

    private static final String SEPARATOR = ".";

    private static final int LIST_HEIGHT = 200;

    private static final int LIST_WIDTH = 300;

    private final Set<String> integratedConfigs;

    private String[] selectedTools = null;

    private List toolList;

    private Button keepOnDiskButton;

    private boolean keepOnDisk;

    private final Map<String, String> integrationMapping;

    public RemoveToolIntegrationDialog(Shell parent, Set<String> integratedConfigurations, Collection<ToolIntegrationContext> contexts) {
        super(parent);
        integratedConfigs = integratedConfigurations;
        integrationMapping = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        for (String config : integratedConfigs) {
            String type ="";
            for (ToolIntegrationContext c : contexts) {
                if (config.contains(c.getPrefixForComponentId())) {
                    type = c.getContextType();
                    integrationMapping.put(config.substring(c.getPrefixForComponentId().length()) 
                    		+ " (Type: " + type.substring(type.lastIndexOf(SEPARATOR) + 1) + ")", config);
                    break;
                }
            }
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Deactivate Tool");
        shell.setImage(null);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g = new GridData(GridData.FILL_BOTH);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);
        new Label(container, SWT.NONE).setText("Choose tool configuration to deactivate: ");
        toolList = new List(container, SWT.MULTI | SWT.BORDER);
        GridData toolListData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        toolListData.widthHint = LIST_WIDTH;
        toolListData.heightHint = LIST_HEIGHT;
        toolList.setLayoutData(toolListData);
        for (String key : integrationMapping.keySet()) {
            toolList.add(key);
        }
        toolList.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (toolList.getSelectionCount() > 0) {
                    selectedTools = toolList.getSelection();
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        keepOnDiskButton = new Button(container, SWT.CHECK);
        keepOnDiskButton.setText("Keep tool configuration on disk");
        keepOnDiskButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                keepOnDisk = keepOnDiskButton.getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        keepOnDiskButton.setSelection(true);
        keepOnDisk = true;
        return container;
    }

    public boolean getKeepOnDisk() {
        return keepOnDisk;
    }

    /**
     * Gets an array of the tool ids to remove.
     * 
     * @return tool ids.
     */
    public String[] getSelectedTools() {
        if (selectedTools != null && selectedTools.length > 0) {
            String[] selectedIDs = new String[selectedTools.length];
            for (int i = 0; i < selectedTools.length; i++) {
                selectedIDs[i] = integrationMapping.get(selectedTools[i]);
            }
            return selectedIDs;
        }
        return new String[0];
    }

    public void setSelectedTools(String[] selectedTools) {
        this.selectedTools = selectedTools;
    }
}
