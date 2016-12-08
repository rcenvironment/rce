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
public class BaseScriptReadOnlySection extends ValidatingWorkflowNodePropertySection {

    private LineNumberStyledText scriptText;

    private final String scriptName;

    public BaseScriptReadOnlySection(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));
        
        final Composite composite = getWidgetFactory().createFlatFormComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));
        
        final Section scriptSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        scriptSection.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        
        if (scriptName.equals(ToolIntegrationConstants.KEY_PRE_SCRIPT)) {
            scriptSection.setText(Messages.preScriptSection);
        } else {
            scriptSection.setText(Messages.postScriptSection);
        }

        Composite scriptComposite = getWidgetFactory().createFlatFormComposite(scriptSection);
        scriptComposite.setLayout(new GridLayout(1, false));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scriptText = new LineNumberStyledText(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        scriptText.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scriptText.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        scriptSection.setClient(scriptComposite);
        scriptText.setEditable(false);

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        scriptText.setText("");
        ReadOnlyConfiguration readOnlyConfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();
        String script = readOnlyConfig.getValue(scriptName);
        if (script != null) {
            scriptText.setText(script);
        }
        scriptText.setBackgroundEnabled(false);
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }
}
