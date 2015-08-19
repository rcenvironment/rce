/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * 
 * Condition section where user can set desired condition.
 * 
 * @author David Scholz
 */
public class SwitchConditionSection extends ValidatingWorkflowNodePropertySection {

    private static final int KEY_CODE_A = 97;

    private StyledText conditionTextfield;

    private Button insertChannelButton;

    private Button insertOpButton;

    private Combo channelCombo;

    private Combo opCombo;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayout(new GridLayout(1, false));
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        Section parentSection = aTabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        parentSection.setLayout(new GridLayout());
        parentSection.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        parentSection.setText(Messages.conditionFieldString);
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        final Composite mainComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(parent);
        mainComposite.setLayout(new GridLayout(2, true));
        mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        conditionTextfield = new StyledText(mainComposite, SWT.WRAP | SWT.BORDER);
        conditionTextfield.setLayout(new GridLayout());
        conditionTextfield.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        conditionTextfield.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CONDITION_KEY);
        conditionTextfield.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        conditionTextfield.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == KEY_CODE_A) {
                    conditionTextfield.selectAll();
                }
            }

        });

        final Composite comboSectionComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(mainComposite);
        comboSectionComposite.setLayout(new GridLayout());
        comboSectionComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        final Composite propertiesComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(comboSectionComposite);
        propertiesComposite.setLayout(new GridLayout(3, false));
        propertiesComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_VERTICAL));
        Label opLabel = new Label(propertiesComposite, SWT.NONE);
        opLabel.setText(Messages.operatorsLabelString);
        opLabel.setBackground(propertiesComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        opCombo = new Combo(propertiesComposite, SWT.READ_ONLY);
        opCombo.setLayout(new GridLayout());
        opCombo.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        setComboOperators();
        opCombo.pack();
        insertOpButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(propertiesComposite, Messages.insertButtonString, SWT.PUSH);
        insertOpButton.addListener(SWT.Selection, getButtonListener());

        Label channelLabel = new Label(propertiesComposite, SWT.NONE);
        channelLabel.setText(Messages.channelLabelString);
        channelLabel.setBackground(propertiesComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        channelCombo = new Combo(propertiesComposite, SWT.READ_ONLY);
        channelCombo.setLayout(new GridLayout());
        channelCombo.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        channelCombo.pack();
        insertChannelButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(propertiesComposite, Messages.insertButtonString, SWT.PUSH);
        insertChannelButton.addListener(SWT.Selection, getButtonListener());
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        channelCombo.removeAll();

        for (EndpointDescription channelName : getConfiguration().getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            channelCombo.add(channelName.getName());
        }

        for (EndpointDescription dataName : getConfiguration().getInputDescriptionsManager().getStaticEndpointDescriptions()) {
            for (DataType datatype : SwitchComponentConstants.CONDITION_INPUT_DATA_TYPES) {
                if (datatype.equals(dataName.getDataType())) {
                    channelCombo.add(dataName.getName());
                }
            }
        }
        channelCombo.select(0); // default combo selection
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        refreshBeforeValidation();
    }

    @Override
    protected void refreshBeforeValidation() {
        aboutToBeShown();
    }

    private void setComboOperators() {
        opCombo.removeAll();
        opCombo.setItems(SwitchComponentConstants.OPERATORS);
        opCombo.select(0);
    }

    private Listener getButtonListener() {

        return new Listener() {

            @Override
            public void handleEvent(Event arg0) {

                String s = null;

                if (arg0.widget.equals(insertOpButton)) {
                    s = opCombo.getText() + " ";
                    conditionTextfield.insert(s);
                } else if (arg0.widget.equals(insertChannelButton)) {
                    s = channelCombo.getText() + " ";
                    conditionTextfield.insert(s);
                }

                if (arg0.widget.equals(insertOpButton) || arg0.widget.equals(insertChannelButton)) {
                    conditionTextfield.setFocus();
                    conditionTextfield.setCaretOffset(conditionTextfield.getCaretOffset() + s.length());
                }
            }
        };
    }

}
