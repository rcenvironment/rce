/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.wizards.toolintegration;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;

/**
 * File chooser to choose a file path relative to the tool directory.
 * 
 * @author Jan Flink
 */
public class RelativeXMLFilePathChooserButtonListener implements SelectionListener {

    private Text linkedTextfield;

    private Shell shell;

    private Map<String, Object> configurationMap;

    private String title;

    public RelativeXMLFilePathChooserButtonListener(String title, Text linkedTextfield, Shell shell, Map<String, Object> configurationMap) {
        this.title = title;
        this.linkedTextfield = linkedTextfield;
        this.shell = shell;
        this.configurationMap = configurationMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void widgetSelected(SelectionEvent arg0) {
        List<Map<String, String>> launchConfigs =
            (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
        // In case of CPACS tool integration only one launch configuration is allowed
        String toolDirectory = launchConfigs.iterator().next().get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY);

        String selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
            new String[] { "*.xml;*.xsl", "*.xml", "*.xsl" }, title, toolDirectory);
        if (selectedPath != null) {
            File selectedFile = new File(selectedPath.replace(toolDirectory + File.separator, ""));
            if (!selectedFile.isAbsolute()) {
                linkedTextfield.setText(selectedFile.getPath());
            } else {
                MessageDialog.openError(shell, Messages.fileNotRelativeTitle, Messages.fileNotRelativeText);
                linkedTextfield.setText("");
            }
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }

    /**
     * Updates the configuration map.
     * 
     * @param newConfigurationMap The new configuration map.
     */
    public void updateConfiguration(Map<String, Object> newConfigurationMap) {
        this.configurationMap = newConfigurationMap;
    }

}
