/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;

/**
 * An extended "Properties" view tab for configuring dynamic endpoints (ie inputs and outputs) and using initial Variables.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Markus Kunde
 */
public class EndpointPropertySection extends ValidatingWorkflowNodePropertySection {

    private static final int OFFSET_ENPOINTPANES = 20;

    private static final int OFFSET_PANES = 50;

    protected Composite endpointsComposite;

    protected Composite parentComposite;

    private EndpointSelectionPane[] panes;

    private int columns;

    public EndpointPropertySection() {}

    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {

        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        this.parentComposite = parent;

        TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        Composite content = new LayoutComposite(parent);

        endpointsComposite = toolkit.createFlatFormComposite(content);

        endpointsComposite.setLayout(new GridLayout(columns, true));
        GridData layoutData;

        for (EndpointSelectionPane pane : panes) {
            pane.createControl(endpointsComposite, pane.genericEndpointTitle, toolkit);
            layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
            pane.getControl().setLayoutData(layoutData);
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        final ComponentInstanceProperties configuration = getConfiguration();
        for (EndpointSelectionPane pane : panes) {
            pane.setConfiguration(configuration);
        }
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refreshSection();
        if (endpointsComposite != null) {
            // In some cases the offset is not necessary and produces an exception (e.g. Inputprovider with linux mint)
            // Not setting it resolves the problem.l
            if (panes.length > 1) {
                endpointsComposite.setSize(endpointsComposite.getSize().x - OFFSET_ENPOINTPANES, endpointsComposite.getSize().y);
                endpointsComposite.pack();
            } else {
                endpointsComposite.setSize(parentComposite.getSize().x, endpointsComposite.getSize().y);
            }
            endpointsComposite.layout();
        }
    }

    public EndpointSelectionPane[] getPanes() {
        return panes;
    }

    public void setPanes(EndpointSelectionPane... panes) {
        this.panes = panes;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }
}
