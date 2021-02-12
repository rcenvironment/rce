/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dialog for removing an integrated tool configuration.
 * 
 * @author Sascha Zur
 * @author Adrian Stock
 */
public class RemoveToolIntegrationDialog extends Dialog {

    private static final Log LOGGER = LogFactory.getLog(ChooseConfigurationPage.class);

    private static final int LIST_HEIGHT = 200;

    private static final int LIST_WIDTH = 300;

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private final Set<String> integratedConfigs;

    private String[] selectedTools = null;

    private List toolList;

    private Button keepOnDiskButton;

    private boolean keepOnDisk;

    private final Map<String, String> integrationMapping;

    private Map<String, Map<String, Object>> allConfigurations;

    public RemoveToolIntegrationDialog(Shell parent, Set<String> integratedConfigurations, Collection<ToolIntegrationContext> contexts) {
        super(parent);
        integratedConfigs = integratedConfigurations;
        integrationMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        allConfigurations = new TreeMap<String, Map<String, Object>>();
        readExistingConfigurations(contexts);

        for (String config : integratedConfigs) {
            for (ToolIntegrationContext c : contexts) {
                if (config.contains(c.getPrefixForComponentId())) {
                    integrationMapping.put(
                        // TODO use the new tool id mapper introduced in 9.0.0 here?
                        createDisplayedToolName(config.substring(c.getPrefixForComponentId().length())),
                        config);
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

    private void readExistingConfigurations(Collection<ToolIntegrationContext> contexts) {

        for (ToolIntegrationContext context : contexts) {
            String configFolder = context.getRootPathToToolIntegrationDirectory();
            File toolIntegrationFile = new File(configFolder, context.getNameOfToolIntegrationDirectory());
            if (toolIntegrationFile.exists() && toolIntegrationFile.isDirectory() && toolIntegrationFile.listFiles() != null
                && toolIntegrationFile.listFiles().length > 0) {
                for (File toolFolder : toolIntegrationFile.listFiles()) {
                    if (toolFolder != null && toolFolder.isDirectory() && !toolFolder.getName().equals("null")
                        && toolFolder.listFiles() != null && toolFolder.listFiles().length > 0) {
                        File[] files = toolFolder.listFiles();
                        for (File f : files) {
                            if (f.getName().equals(context.getConfigurationFilename())) {
                                try {
                                    @SuppressWarnings("unchecked") Map<String, Object> configurationMap =
                                        mapper.readValue(f, new HashMap<String, Object>().getClass());
                                    allConfigurations.put((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                                        configurationMap);
                                } catch (IOException e) {
                                    LOGGER.error("Could not read configuration file: ", e);
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the string that will be displayed in the edit / remove page.
     * 
     * @param toolName of the tool
     * 
     * @return a string that will be displayed in the gui.
     */
    public String createDisplayedToolName(String toolName) {
        String type = ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_TYPE;
        if (allConfigurations.get(toolName).get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
            type = (String) allConfigurations.get(toolName).get(ToolIntegrationConstants.INTEGRATION_TYPE);
        }
        @SuppressWarnings({ "rawtypes", "unchecked" }) ArrayList<LinkedHashMap> launchSettings =
            (ArrayList<LinkedHashMap>) allConfigurations.get(toolName).get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
        @SuppressWarnings("unchecked") String version =
            ((LinkedHashMap<Object, String>) (launchSettings.get(0))).get(ToolIntegrationConstants.KEY_VERSION);
        Object[] toolInformation = { toolName, version, type };
        String displayedToolName = StringUtils.format("%s (%s; Type: %s)", toolInformation);
        return displayedToolName;
    }
}
