/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.activeX.ActiveXInvocationProxy;
import com.jacob.com.Dispatch;
import com.jacob.com.DispatchProxy;
import com.jacob.com.Variant;

import de.rcenvironment.components.excel.common.ExcelAddress;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelServiceGUIEvents;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;


/**
 * Simple Dialog for selection of Excel cell address.
 *
 * @author Markus Kunde
 */
public class CellSelectionDialog extends Dialog {

    private static final int MAX_HEIGHT = 100;
    private static final int MAX_WIDTH = 430;
    protected Shell shlExcelAddress;
    private Text textCellAddress;
    private VariablesEditDialog varDialog;
    private String initAddress;
    
    private ExcelServiceGUIEvents excelEvents;
    
    private DispatchProxy dispatchProxy;

    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public CellSelectionDialog(Shell parent, int style, VariablesEditDialog dialog) {
        super(parent, style);
        setText("SWT Dialog");
        varDialog = dialog;
        
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        excelEvents = serviceRegistryAccess.getService(ExcelServiceGUIEvents.class);
    }

    /**
     * Open the dialog.
     * @param initialAddress initial Address of Excel cell
     */
    public void open(final String initialAddress) {
        createContents();
        shlExcelAddress.open();
        shlExcelAddress.layout();
        initAddress = initialAddress;
        setAddress(initAddress);
        ActiveXComponent objExcel = 
            excelEvents.openExcelApplicationRegisterListener(varDialog.getFile(), initAddress, new ExcelApplicationEventsProxy());
        dispatchProxy = new DispatchProxy(objExcel);
    }
    
    /**
     * Closes the dialog.
     * 
     */
    public void close() {
        ActiveXComponent objExcel = new ActiveXComponent(dispatchProxy.toDispatch());
        if (objExcel != null) {
            excelEvents.quitExcel(objExcel, false);
        }        
        getParent().dispose();
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shlExcelAddress = new Shell(getParent(), getStyle());
        shlExcelAddress.setSize(MAX_WIDTH, MAX_HEIGHT);
        shlExcelAddress.setText("Excel Address Selection Dialog");
        shlExcelAddress.setLayout(new FormLayout());
        
        textCellAddress = new Text(shlExcelAddress, SWT.BORDER);
        FormData fdtextCellAddress = new FormData();
        fdtextCellAddress.top = new FormAttachment(0, 10);
        fdtextCellAddress.left = new FormAttachment(0);
        fdtextCellAddress.right = new FormAttachment(MAX_HEIGHT);
        textCellAddress.setLayoutData(fdtextCellAddress);
        
        final Button btnOk = new Button(shlExcelAddress, SWT.NONE);
        FormData fdbtnOk = new FormData();
        fdbtnOk.top = new FormAttachment(textCellAddress, 6);
        btnOk.setLayoutData(fdbtnOk);
        btnOk.setText("OK");
        
        final Button btnCancel = new Button(shlExcelAddress, SWT.NONE);
        fdbtnOk.right = new FormAttachment(btnCancel);
        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
        FormData fdbtnCancel = new FormData();
        fdbtnCancel.top = new FormAttachment(textCellAddress, 6);
        fdbtnCancel.right = new FormAttachment(textCellAddress, 0, SWT.RIGHT);
        btnCancel.setLayoutData(fdbtnCancel);
        btnCancel.setText("Cancel");
        
        Listener listener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.widget != btnOk) {
                    setAddress(initAddress);  
                }
                varDialog.notifyAboutSelection();
            }
        };
        
        btnOk.addListener(SWT.Selection, listener);
        btnCancel.addListener(SWT.Selection, listener);
    }
        
    /**
     * Sets Excels cell address.
     * 
     * @param address in Excel
     */
    public void setAddress(final String address) {
        textCellAddress.setText(address);
    }
    
    /**
     * Returns Excels cell address.
     * 
     * @return address in Excel
     */
    public String getAddress() {
        return textCellAddress.getText();
    }
    
    
    /**
     * Listener for gui events.
     *
     * @author Markus Kunde
     */
    class ExcelApplicationEventsProxy extends ActiveXInvocationProxy {
        /**
         * {@inheritDoc}
         *
         * @see com.jacob.activeX.ActiveXInvocationProxy#invoke(java.lang.String, com.jacob.com.Variant[])
         */
        @Override
        public Variant invoke(String methodName, Variant[] targetParameter) {       
            if (methodName.equalsIgnoreCase("SheetSelectionChange")) {
                sheetSelectionChange(targetParameter);
            }
            return null;
        }

        
        private void sheetSelectionChange(Variant[] arguments) {
            if (arguments.length == 2) {
                Dispatch range = arguments[0].getDispatch();
                String rangeAddress = Dispatch.get(range, "Address").getString();
                Dispatch worksheet = Dispatch.get(range, "Worksheet").toDispatch();
                String worksheetName = Dispatch.get(worksheet, "Name").getString();

                ExcelAddress addr = 
                    new ExcelAddress(varDialog.getFile(), worksheetName + ExcelComponentConstants.DIVIDER_TABLECELLADDRESS + rangeAddress);
                
                if (addr != null) {
                    setAddress(addr.getFullAddress());
                }
            } else {
                throw new RuntimeException("Return target-arguments of 'SheetSelectionChange(Sh, Target)' does not have length of 2.");
            }
        }
    }
}
