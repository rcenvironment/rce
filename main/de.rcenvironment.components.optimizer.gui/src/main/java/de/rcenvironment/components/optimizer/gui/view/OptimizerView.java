/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.view;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.components.optimizer.common.OptimizerResultService;
import de.rcenvironment.components.optimizer.gui.properties.Messages;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.gui.workflow.view.ComponentRuntimeView;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Runtime view of the {@link ParametricStudyComponent}.
 * 
 * @author Sascha Zur
 */
public class OptimizerView extends ViewPart implements ComponentRuntimeView, ISelectionProvider {

    private ComponentExecutionInformation componentExecutionInformation;

    private TabFolder tabFolder;

    private ChartDataComposite dataComposite;

    private ChartConfigurationComposite configurationComposite;

    private boolean initialized = false;

    private final List<ISelectionChangedListener> selectionChangedListeners = new LinkedList<ISelectionChangedListener>();

    private ISelectionProvider selectionProvider;

    private OptimizerDatastore study;

    @Override
    public void createPartControl(Composite parent) {
        // create the tab folder
        tabFolder = new TabFolder(parent, SWT.BOTTOM);
        // update the selection provider upon tab changes
        tabFolder.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                final TabItem tabItem = (TabItem) event.item;
                final Widget widget = tabItem.getControl();
                ISelectionProvider provider = null;
                if (widget instanceof ISelectionProvider) {
                    provider = (ISelectionProvider) widget;
                }
                setSelectionProvider(provider);
            }

        });

        dataComposite = new ChartDataComposite(tabFolder, SWT.NONE, componentExecutionInformation);
        dataComposite.createControls();

        // create the chart tab item
        final TabItem chartTabItem = new TabItem(tabFolder, SWT.NONE, 0);
        chartTabItem.setText(Messages.chartTabText);

        final TabItem dataTabItem = new TabItem(tabFolder, SWT.NONE, 1);
        dataTabItem.setText(Messages.dataTabText);
        //

        configurationComposite = new ChartConfigurationComposite(tabFolder,
            SWT.NONE);
        configurationComposite.createControls();
        chartTabItem.setControl(configurationComposite);
        // create the data tab item

        dataTabItem.setControl(dataComposite);

        setSelectionProvider(dataComposite);

        getSite().setSelectionProvider(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        // why doesn't it get disposed automatically???
        tabFolder.dispose();
        // dataComposite.dispose();
        // configurationComposite.dispose();
    }

    @Override
    public void setFocus() {
        if (!initialized) {
            initialized = true;
            refresh();
        }
    }

    private void refresh() {
        if (componentExecutionInformation == null) {
            return;
        }
    }

    @Override
    public void initializeData(final ComponentExecutionInformation compInstanceDescr) {
        this.componentExecutionInformation = compInstanceDescr;
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        OptimizerResultService optimizerResultService = serviceRegistryAccess.getService(OptimizerResultService.class);
        study = OptimizerDatastore.connect(componentExecutionInformation.getExecutionIdentifier(),
            componentExecutionInformation.getNodeId(), optimizerResultService);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        synchronized (selectionChangedListeners) {
            selectionChangedListeners.add(listener);
        }
    }

    @Override
    public void removeSelectionChangedListener(
        ISelectionChangedListener listener) {
        synchronized (selectionChangedListeners) {
            selectionChangedListeners.remove(listener);
        }
    }

    @Override
    public ISelection getSelection() {
        ISelection selection = null;
        if (selectionProvider != null) {
            selection = selectionProvider.getSelection();
        }
        return selection;
    }

    @Override
    public void setSelection(ISelection selection) {
        throw new UnsupportedOperationException();
    }

    private void setSelectionProvider(final ISelectionProvider selectionProvider) {
        synchronized (selectionChangedListeners) {
            // remove listeners from old provider
            for (final ISelectionChangedListener listener : selectionChangedListeners) {
                this.selectionProvider.removeSelectionChangedListener(listener);
            }
            this.selectionProvider = selectionProvider;
            // add listeners from new provider
            for (final ISelectionChangedListener listener : selectionChangedListeners) {
                this.selectionProvider.addSelectionChangedListener(listener);
            }
        }
    }

    @Override
    public void initializeView() {
        if (study != null) {
            dataComposite.setStudyDatastore(study);
            configurationComposite.setStudyDatastore(study);
        }
    }

}
