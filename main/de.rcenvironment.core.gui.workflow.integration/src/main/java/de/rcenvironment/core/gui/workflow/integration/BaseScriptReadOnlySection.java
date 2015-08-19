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
public class BaseScriptReadOnlySection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT = 500;

    private Text scriptText;

    private final String scriptName;

    public BaseScriptReadOnlySection(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final Section scriptSection;
        if (scriptName.equals(ToolIntegrationConstants.KEY_PRE_SCRIPT)) {
            scriptSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
                Messages.preScriptSection);
        } else {
            scriptSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
                Messages.postScriptSection);
        }

        Composite scriptComposite = getWidgetFactory().createFlatFormComposite(scriptSection);
        scriptComposite.setLayout(new GridLayout(1, false));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scriptText = new Text(scriptComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        scriptText.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        // scriptText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        scriptText.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        ((GridData) scriptText.getLayoutData()).heightHint = MINIMUM_HEIGHT;
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
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }
}
