/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.gui.utils.common.widgets.LineNumberStyledText;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Basic section for shown scripts in read only mode.
 * 
 * @author Sascha Zur
 */
public class CommandReadOnlySection extends ValidatingWorkflowNodePropertySection {

    private LineNumberStyledText commandScriptWindows;

    private LineNumberStyledText commandScriptLinux;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));
        
        final Composite composite = getWidgetFactory().createFlatFormComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));
        
        final Section scriptSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        scriptSection.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scriptSection.setText(Messages.commandScriptSection);

        Composite scriptComposite = getWidgetFactory().createFlatFormComposite(scriptSection);
        scriptComposite.setLayout(new GridLayout(2, true));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        new Label(scriptComposite, SWT.NONE).setText("Windows");
        new Label(scriptComposite, SWT.NONE).setText("Linux");

        commandScriptWindows = new LineNumberStyledText(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        commandScriptWindows.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        commandScriptWindows.setEditable(false);
        commandScriptWindows.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
                
        commandScriptLinux = new LineNumberStyledText(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        commandScriptLinux.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        commandScriptLinux.setEditable(false);
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
            commandScriptWindows.setBackgroundEnabled(false);
        }
        commandScriptLinux.setText("");
        String linuxScript = readOnlyConfig.getValue(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX);
        if (linuxScript != null) {
            commandScriptLinux.setText(linuxScript);
            commandScriptLinux.setBackgroundEnabled(false);
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }
}
