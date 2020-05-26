/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * A dialog for defining and editing xpath as additional endpoints.
 * 
 * @author Markus Kunde
 * @author Adrian Stock
 * @author Jan Flink
 */
public class XPathEditDialog extends EndpointEditDialog {
    
    private static final String CHANNEL_XPATH = "variable.xpath";

    public XPathEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, Image icon,
        EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected Control createConfigurationArea(Composite parent) {
        Control superControl = super.createConfigurationArea(parent);

        Button selectButton = new Button(parent, SWT.NONE);
        selectButton.setText(Messages.selectButton);
        selectButton.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                createXPathDialog();
            }
        });

        return superControl;
    }

    private void createXPathDialog() {
        XPathChooserDialog dialog = new XPathChooserDialog(super.getShell(), null);
        if (dialog.getChooser() != null && dialog.open() == TitleAreaDialog.OK) {
            final VariableEntry newVar = dialog.getSelectedVariable();
            String xpath = newVar.getXpath();

            if (xpath != null && !xpath.isEmpty()) {

                Widget address = super.getWidget(CHANNEL_XPATH);
                if (address instanceof Text) {
                    ((Text) address).setText(xpath);
                }
            }

        }
    }
}
