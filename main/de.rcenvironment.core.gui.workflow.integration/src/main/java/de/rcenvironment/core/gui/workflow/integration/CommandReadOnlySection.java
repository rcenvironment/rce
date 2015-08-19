/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Basic section for shown scripts in read only mode.
 * 
 * @author Sascha Zur
 */
public class CommandReadOnlySection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT = 500;

    private Text commandScriptWindows;

    private Text commandScriptLinux;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final Section scriptSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
            Messages.commandScriptSection);

        Composite scriptComposite = getWidgetFactory().createFlatFormComposite(scriptSection);
        scriptComposite.setLayout(new GridLayout(2, true));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        new Label(scriptComposite, SWT.NONE).setText("Windows");
        new Label(scriptComposite, SWT.NONE).setText("Linux");

        commandScriptWindows = new Text(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        commandScriptWindows.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        commandScriptWindows.setEditable(false);
        ((GridData) commandScriptWindows.getLayoutData()).heightHint = MINIMUM_HEIGHT;
        commandScriptWindows.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));

        commandScriptLinux = new Text(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        commandScriptLinux.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        commandScriptLinux.setEditable(false);
        ((GridData) commandScriptLinux.getLayoutData()).heightHint = MINIMUM_HEIGHT;
        commandScriptLinux.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));

        scriptSection.setClient(scriptComposite);

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        commandScriptWindows.setText("");
        ReadOnlyConfiguration readOnlyConfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();
        String winScript = readOnlyConfig.getValue(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS);
        if (winScript != null) {
            commandScriptWindows.setText(winScript);
        }
        commandScriptLinux.setText("");
        String linuxScript = readOnlyConfig.getValue(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX);
        if (linuxScript != null) {
            commandScriptLinux.setText(linuxScript);
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }
}
