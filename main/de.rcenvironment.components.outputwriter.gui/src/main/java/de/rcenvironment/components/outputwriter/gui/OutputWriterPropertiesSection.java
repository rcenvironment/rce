/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;

/**
 * Creates a "Properties" view tab for configuring endpoints (only inputs).
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * @author Brigitte Boden
 * 
 */
public class OutputWriterPropertiesSection extends DefaultEndpointPropertySection {

    private Button workflowStartCheckbox;

    private Text rootText;

    private Button rootButton;

    private Composite noteComposite;

    private OutputLocationPane outputLocationPane;

    public OutputWriterPropertiesSection() {

        OutputWriterEndpointSelectionPane outputPane =
            new OutputWriterEndpointSelectionPane(Messages.inputs, EndpointType.INPUT, "default", this);
        setColumns(1);
        setPanes(outputPane);

        outputLocationPane = new OutputLocationPane(this);

        outputPane.setOutputLocationPane(outputLocationPane);
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        final ComponentInstanceProperties configuration = getConfiguration();
        outputLocationPane.setConfiguration(configuration);
        outputLocationPane.refresh();
    }

    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayout(new FillLayout(SWT.VERTICAL | SWT.V_SCROLL));
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        GridData layoutData;
        TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        Composite content = new LayoutComposite(parent);
        Composite outputComposite = toolkit.createFlatFormComposite(content);

        outputComposite.setLayout(new GridLayout(1, true));
        outputLocationPane.createControl(outputComposite, Messages.outputLocationPaneTitle, toolkit);
        layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        outputLocationPane.getControl().setLayoutData(layoutData);

        Composite root = new LayoutComposite(parent);
        Composite rootComposite = toolkit.createFlatFormComposite(root);
        rootComposite.setLayout(new GridLayout(1, true));
        createRootSection(rootComposite, toolkit);

        rootComposite.layout();
        outputComposite.layout();
    }

    private Composite createRootSection(final Composite parent, FormToolkit toolkit) {

        final Section sectionProperties = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.rootFolderSectionTitle);
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        sectionProperties.setLayoutData(layoutData);

        Composite rootgroup = toolkit.createComposite(sectionProperties);
        rootgroup.setLayout(new GridLayout(2, false));
        workflowStartCheckbox = new Button(rootgroup, SWT.CHECK);
        workflowStartCheckbox.setText(Messages.selectAtStart);

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
                dialog.setText(Messages.selectRootFolder);
                dialog.setMessage(Messages.selectRootFolder);
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

        sectionProperties.setClient(rootgroup);
        sectionProperties.setVisible(true);

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
