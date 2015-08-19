/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.properties;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.rce.components.excel.commons.ExcelAddress;

/**
 * A dialog for defining and editing cells as additional endpoints.
 * 
 * @author Patrick Schaefer
 * @author Markus Kunde
 */
public class VariablesEditDialog extends EndpointEditDialog {

    private File xlFile;
    
    private CellSelectionDialog selectionDialog;

    
    public VariablesEditDialog(Shell parentShell, String title, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, Image icon,
        EndpointMetaDataDefinition metaData, Map<String, String> metadataValues, final File xlFile) {
        super(parentShell, title, configuration, direction, id, isStatic, metaData, metadataValues);
        
        this.xlFile = xlFile;
    }

    public File getFile() {
        return xlFile;
    }

    protected void notifyAboutSelection() {
        if (selectionDialog.getAddress() != null && !selectionDialog.getAddress().isEmpty()) {
            ExcelAddress addr = new ExcelAddress(xlFile, selectionDialog.getAddress());

            Widget address = super.getWidget(ExcelComponentConstants.METADATA_ADDRESS);
            if (address instanceof Text) {
                ((Text) address).setText(addr.getFullAddress());
            }
        }
        selectionDialog.close(); 
    }
    
    @Override
    protected Control createConfigurationArea(Composite parent) {
        Control superControl = super.createConfigurationArea(parent);
        
        Button selectButton = new Button(parent, SWT.NONE);
        selectButton.setText(Messages.selectButton);
        selectButton.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                createExcelDialog();
            }
        });
        
        return superControl;
    }
    
    @Override
    protected boolean validateMetaDataInputs() {
        boolean validation = false;
        
        if (super.validateMetaDataInputs()) {
            try {
                Widget address = super.getWidget(ExcelComponentConstants.METADATA_ADDRESS);
                if (address instanceof Text) {
                    String rawAddress = ((Text) address).getText();
                    ExcelAddress addr = new ExcelAddress(xlFile, rawAddress);
                    validation = true;
                }
            } catch (ExcelException e) {
                validation = false;
            }
        }
        
        return validation;   
    }
    
    /**
     * Creates the Excel-cell choosing dialog.
     */
    private void createExcelDialog() {
        final Shell excelDialog = new Shell(SWT.ON_TOP);
        selectionDialog =
            new CellSelectionDialog(excelDialog, SWT.ON_TOP | SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM, this);

        excelDialog.setLayout(new FillLayout());

        excelDialog.setText(xlFile.getAbsolutePath());

        excelDialog.pack();
        excelDialog.setSize(0, 0);
        Rectangle parentSize = excelDialog.getBounds();
        Rectangle mySize = excelDialog.getBounds();

        int locationX;
        int locationY;
        locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
        locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;

        excelDialog.setLocation(new Point(locationX, locationY));
        excelDialog.open();        
        selectionDialog.open(super.getMetadataValues().get(ExcelComponentConstants.METADATA_ADDRESS));
    }

    
}
