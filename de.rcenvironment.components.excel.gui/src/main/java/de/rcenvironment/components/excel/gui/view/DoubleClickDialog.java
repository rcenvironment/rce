/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.excel.common.ChannelValue;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;


/**
 * Dialog for detail view on channel values.
 *
 * @author Markus Kunde
 */
public class DoubleClickDialog extends Dialog {
    
    
    /**
     * Dimension of x.
     */
    private static final int POINTSIZE_Y = 300;

    /**
     * Dimenstion of y.
     */
    private static final int POINTSIZE_X = 400;
    
    /**
     * ChannelValue.
     */
    private ChannelValue cval = null;
    
    /**
     * Table (Excel-like representation).
     */
    private Table table = null;
    
    /**
     * Composite.
     */
    private Composite composite = null;

    /**
     * Constructor.
     * @param parentShell parent shell
     * @param cvalue Channelvalue to show in dialog
     */
    protected DoubleClickDialog(Shell parentShell, final ChannelValue cvalue) {
        super(parentShell);
        cval = cvalue;
        this.setShellStyle(getShellStyle() | (SWT.RESIZE | SWT.SHELL_TRIM));
    }
    
    @Override
    public boolean close() {
        boolean ret = super.close();
        
        //Not nice, but workbook-object will not released.
        ExcelUtils.destroyGarbage();
        
        return ret;
    }
    
    @Override
    protected Control createDialogArea(Composite parentComposite) {
        composite = (Composite) super.createDialogArea(parentComposite);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.horizontalSpacing = 0;

        composite.setLayout(gridLayout);
        
        //Label infos                         
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        Label infos = new Label(composite, SWT.NONE);
        String messagePart = null;
        if (cval.isInputValue()) {
            messagePart = Messages.inputChannelNameType;
        } else {
            messagePart = Messages.outputChannelNameType;
        }

        
        infos.setText(Messages.directionColumnName 
                   + ": " + messagePart + "\n" 
                   + Messages.channelColumnName
                   + ": " + cval.getChannelName());
        infos.setLayoutData(gridData);
        
        
        //Label icon
        gridData = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1);
        Label header = new Label(composite, SWT.NONE);
        header.setText(Messages.doubleClickHeader);
        Image image = ImageManager.getInstance().getSharedImage(StandardImages.EXCEL_LARGE);
        header.setImage(image);
        header.setLayoutData(gridData);
   
        
        //Label data content                           
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        table.setLayoutData(gridData);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        
        //Load data in background
        final String oldText = this.composite.getShell().getText();
        this.composite.getShell().setText(Messages.loadInBackground);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                fillTableWithContent();
                composite.getShell().setText(oldText);
            }
        };
        super.getParentShell().getDisplay().asyncExec(r);
      
        table.computeSize(POINTSIZE_Y, POINTSIZE_X);
        
        return composite;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(POINTSIZE_X, POINTSIZE_Y);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.doubleClickShellName);
    }
    
    /**
     * Fill the table with content.
     */
    private void fillTableWithContent() {       
        SmallTableTD vals = cval.getValues();
        
        //Create columns
        for (int loopIndex = 0; loopIndex < vals.getColumnCount(); loopIndex++) {
            TableColumn column = new TableColumn(table, SWT.NULL);
            column.setResizable(true);
        }
        
        //Set table content
        for (int row = 0; row < vals.getRowCount(); row++) {
            TableItem item = new TableItem(table, SWT.NULL);
            for (int col = 0; col < vals.getColumnCount(); col++) {
                String valueString =
                    ExcelUtils.smallTableToString(vals.getSubTable(row, col, row + 1, col + 1),
                        ExcelComponentConstants.STRINGLINESEPARATOR, ExcelComponentConstants.TABLELINESEPARATOR);
                item.setText(col, valueString);
            }
        }
        for (int loopIndex = 0; loopIndex < vals.getColumnCount(); loopIndex++) {
            table.getColumn(loopIndex).pack();
        }

        //Not nice, but workbook-object will not released.
        ExcelUtils.destroyGarbage();
    }

}
