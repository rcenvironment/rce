/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * "Properties" view tab for configuring cluster configuration.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class HostSection extends ValidatingWorkflowNodePropertySection {

    protected static final int TEXT_WIDTH = 220;

    protected static final int BUTTON_WIDTH = 80;

    private boolean pinging = false;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        final Section hostSection = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        hostSection.setText(Messages.configureHost);

        Composite hostParent = factory.createFlatFormComposite(hostSection);

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        hostParent.setLayout(layout);

        final Text hostText = WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(hostParent, Messages.hostLabel,
            SshExecutorConstants.CONFIG_KEY_HOST, TEXT_WIDTH, WidgetGroupFactory.NONE).text;

        Button pingHost = factory.createButton(hostParent, "Ping host", SWT.NONE);

        GridData gridData = new GridData();
        gridData.widthHint = BUTTON_WIDTH;
        pingHost.setLayoutData(gridData);

        WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(hostParent, Messages.portLabel,
            SshExecutorConstants.CONFIG_KEY_PORT, TEXT_WIDTH, WidgetGroupFactory.ONLY_INTEGER);
        final Label pingLabel = new Label(hostParent, SWT.NONE);

        WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(hostParent, Messages.sandboxRootLabel,
            SshExecutorConstants.CONFIG_KEY_SANDBOXROOT, TEXT_WIDTH, WidgetGroupFactory.NONE);

        new Label(hostParent, SWT.NONE);
        new Label(hostParent, SWT.NONE);

        Button deleteSandboxButton = factory.createButton(hostParent, "Delete working directory after execution", SWT.CHECK);
        deleteSandboxButton.setData(CONTROL_PROPERTY_KEY, SshExecutorConstants.CONFIG_KEY_DELETESANDBOX);

        hostSection.setClient(hostParent);

        pingHost.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (!pinging) {
                    final String host = hostText.getText();
                    if (!host.isEmpty()) {
                        pingLabel.setText("Pinging host " + host + " ...");
                    }
                    hostSection.layout();
                    ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Process p1 = java.lang.Runtime.getRuntime().exec("ping " + host);
                                final int result = p1.waitFor();
                                Display.getDefault().asyncExec(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (result == 0) {
                                            pingLabel.setText("Host " + host + " is reachable");
                                        } else {
                                            pingLabel.setText("Could not reach " + host);
                                        }
                                        pinging = false;
                                        hostSection.layout();
                                    }
                                });
                            } catch (InterruptedException e) {
                                Logger.getLogger(HostSection.class.getName()).log(Level.WARNING, "", e);
                            } catch (IOException e) {
                                Logger.getLogger(HostSection.class.getName()).log(Level.WARNING, "", e);
                            }
                        }

                    });
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

    }

}
