/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.gui.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;
import de.rcenvironment.cpacs.gui.xpathchooser.XPathChooserPropertyViewPane;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;



/**
 * A "Properties" view tab for configuring dynamic endpoints and directory channels. Allows new channels to be added via
 * XPathChooser.
 * 
 * @author Markus Kunde
 */
public class ToolWrapperDirectoryPropertyViewPane extends XPathChooserPropertyViewPane {

    /** Checkbox for adding directory channel. */
    private Button buttonDirectory;
    
    public ToolWrapperDirectoryPropertyViewPane(String genericEndpointTitle, EndpointType direction, Executor executor, String id) {
        super(genericEndpointTitle, direction, executor, id);
    }

    @Override
    public Control createControl(Composite parent, String title, FormToolkit toolkit) {
        Control superControl = super.createControl(parent, title, toolkit);
        // empty label to get desired layout - feel free to improve
        new Label(client, SWT.READ_ONLY);
        buttonDirectory = toolkit.createButton(client, Messages.directoryButtonLabel, SWT.FLAT);
        buttonDirectory.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        
        SelectionAdapter buttonListener = new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.widget == buttonDirectory) {
                    final WorkflowNodeCommand command =
                        new AddDynamicEndpointCommand(endpointType, ChameleonCommonConstants.ID_DIRECTORY_PANE,
                            ChameleonCommonConstants.DIRECTORY_CHANNELNAME,
                            endpointManager.getDynamicEndpointDefinition(ChameleonCommonConstants.ID_DIRECTORY_PANE)
                                .getDefaultDataType(), new HashMap<String, String>(), allPanes);
                    execute(command);
                }
            }
        };
        buttonDirectory.addSelectionListener(buttonListener);

        return superControl;
    }
    
    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        if (name.equals(ChameleonCommonConstants.DIRECTORY_CHANNELNAME)) {
            EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
            Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

            EndpointEditDialog dialog =
                new EndpointEditDialog(Display.getDefault().getActiveShell(),
                    String.format(de.rcenvironment.core.gui.workflow.editor.properties.Messages.editMessage, endpointType), configuration,
                    endpointType,
                    ChameleonCommonConstants.ID_DIRECTORY_PANE, false, endpoint.getDeclarativeEndpointDescription()
                        .getMetaDataDefinition(), newMetaData);

            onEditClicked(name, dialog, newMetaData);
        } else {
            super.onEditClicked();
        }
    }
    
    
}
