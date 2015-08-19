/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;

/**
 * Creates a "Properties" view tab for configuring endpoints (only inputs).
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * 
 */
public class OutputWriterPropertiesSection extends DefaultEndpointPropertySection {

    private Button workflowStartCheckbox;

    private Text rootText;

    private Button rootButton;

    private Composite noteComposite;

    public OutputWriterPropertiesSection() {

        OutputWriterEndpointSelectionPane outputPane =
            new OutputWriterEndpointSelectionPane(Messages.inputs, EndpointType.INPUT, this, false, "default", false);
        setColumns(1);
        setPanes(outputPane);
    }

    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayout(new FillLayout(SWT.VERTICAL));
        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        Composite root = new LayoutComposite(parent);
        Composite rootComposite = factory.createFlatFormComposite(root);
        rootComposite.setLayout(new GridLayout(1, true));
        
        final Section sectionProperties = factory.createSection(rootComposite, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText("Root Folder");
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        sectionProperties.setLayoutData(layoutData);
        final Composite sectionInstallationClient = factory.createComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, true));
        createRootSection(sectionInstallationClient);
        sectionProperties.setClient(sectionInstallationClient);
        sectionProperties.setVisible(true);
    }

    private Composite createRootSection(final Composite parent) {
        Composite rootgroup = new Composite(parent, SWT.NONE);
        rootgroup.setLayout(new GridLayout(2, false));
        rootgroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        workflowStartCheckbox = new Button(rootgroup, SWT.CHECK);
        workflowStartCheckbox.setText("Select at workflow start");

        workflowStartCheckbox.setLayoutData(new GridData(SWT.LEFT,
            SWT.TOP, true, false, 2, 1));
        workflowStartCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setEnabilityRoot(!workflowStartCheckbox.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        workflowStartCheckbox.setData(CONTROL_PROPERTY_KEY, OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);

        rootText = new Text(rootgroup, SWT.BORDER);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        rootText.setLayoutData(gridData);
        rootText.setEditable(true);
        rootText.setData(CONTROL_PROPERTY_KEY, OutputWriterComponentConstants.CONFIG_KEY_ROOT);
        rootButton = new Button(rootgroup, SWT.NONE);
        rootButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
            false, 1, 1));
        rootButton.setText("...");
        rootButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
                dialog.setText("Select Folder");
                dialog.setMessage("Select target root folder:");
                String result = dialog.open();
                if (result != null) {
                    rootText.setText(result);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        noteComposite = new Composite(rootgroup, SWT.NONE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        noteComposite.setLayoutData(gridData);
        noteComposite.setLayout(new GridLayout(2, false));

        Label warnLabel = new Label(noteComposite, SWT.READ_ONLY);
        warnLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
        Label noteLabel = new Label(noteComposite, SWT.READ_ONLY);
        warnLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setText(Messages.note);
        return rootgroup;
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        setEnabilityRoot(!workflowStartCheckbox.getSelection());
    }

    private void setEnabilityRoot(boolean enabled) {
        noteComposite.setVisible(enabled);
        rootText.setEnabled(enabled);
        rootButton.setEnabled(enabled);
    }
}
