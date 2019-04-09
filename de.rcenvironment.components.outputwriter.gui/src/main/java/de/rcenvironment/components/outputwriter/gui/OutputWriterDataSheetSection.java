/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertySection;

/**
 * Creates a "Properties" view tab for configuring output writer data sheet properties.
 * 
 * @author Oliver Seebach
 * 
 */
public class OutputWriterDataSheetSection extends WorkflowNodePropertySection {

    private OutputLocationPane outputLocationPane;

    public OutputWriterDataSheetSection() {
        outputLocationPane = new OutputLocationPane(this);
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

        outputComposite.layout();
    }

    @Override
    protected void refreshSection() {
        super.refreshSection();
        final ComponentInstanceProperties configuration = getConfiguration();
        outputLocationPane.setConfiguration(configuration);
    }
    
    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refreshSection();
    }
    
}
