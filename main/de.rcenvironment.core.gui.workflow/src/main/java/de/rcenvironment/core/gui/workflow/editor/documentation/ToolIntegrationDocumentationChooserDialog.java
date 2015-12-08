/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.documentation;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Dialog for choosing a documentation version if there are multiple.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationDocumentationChooserDialog extends Dialog {

    private Map<String, String> componentInstallationsWithDocumentation;

    private String selectedHash = null;

    private Table table;

    private String toolIdentifier;

    /**
     * Create the dialog.
     * 
     * @param parentShell
     * @param componentInstallationsWithDocumentation
     * @param toolIdentifier
     */
    public ToolIntegrationDocumentationChooserDialog(Shell parentShell,
        Map<String, String> componentInstallationsWithDocumentation, String toolIdentifier) {
        super(parentShell);
        this.componentInstallationsWithDocumentation = componentInstallationsWithDocumentation;
        this.toolIdentifier = toolIdentifier;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        setShellStyle(SWT.MIN | SWT.RESIZE | SWT.TITLE);
        newShell.setText(
            StringUtils.format("Select Documentation For Component %s (Version %s)",
                toolIdentifier.substring(toolIdentifier.lastIndexOf('.') + 1, toolIdentifier.lastIndexOf("/")),
                toolIdentifier.substring(toolIdentifier.lastIndexOf("/") + 1)));
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        ServiceRegistryPublisherAccess serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        Label informationLabel = new Label(container, SWT.NONE);
        informationLabel
            .setText("There is more than one version of the components documentation available.\r\nPlease choose the desired version.");

        DistributedComponentKnowledgeService componentKnowledgeService =
            serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
        componentKnowledgeService.getCurrentComponentKnowledge().getAllInstallations();

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnInstancename = new TableColumn(table, SWT.NONE);
        tblclmnInstancename.setText("Instance name");

        TableColumn tblclmnLocal = new TableColumn(table, SWT.NONE);
        tblclmnLocal.setText("Source");

        TableColumn tblclmnHashValue = new TableColumn(table, SWT.NONE);
        tblclmnHashValue.setText("Hash value");

        String[] items = new String[componentInstallationsWithDocumentation.size()];
        componentInstallationsWithDocumentation.keySet().toArray(items);

        for (String key : componentInstallationsWithDocumentation.keySet()) {
            TableItem item = new TableItem(table, SWT.NULL);

            String nodeId = componentInstallationsWithDocumentation.get(key);
            boolean cached = false;
            if (nodeId.endsWith(ToolIntegrationConstants.DOCUMENTATION_CACHED_SUFFIX)) {
                nodeId = nodeId.substring(0, nodeId.length() - 3);
                cached = true;
            }
            NodeIdentifier identifier = NodeIdentifierFactory.fromNodeId(nodeId);
            item.setText(0, identifier.getAssociatedDisplayName());
            if (cached) {
                item.setText(1, "Cached");
            } else {
                boolean localNode = identifier.equals(serviceRegistryAccess.getService(PlatformService.class).getLocalNodeId());
                if (localNode) {
                    item.setText(1, "Local");
                } else {
                    item.setText(1, "Remote");
                }
            }
            item.setText(2, key);
        }

        table.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                selectedHash = table.getSelection()[0].getText(2);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        selectedHash = items[0];

        serviceRegistryAccess.dispose();
        tblclmnInstancename.pack();
        tblclmnLocal.pack();
        tblclmnHashValue.pack();
        return container;
    }

    protected void verify() {
        getButton(OK).setEnabled(selectedHash != null);
    }

    public String getSelectedHash() {
        return selectedHash;
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

}
