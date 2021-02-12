/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.xpathchooser;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.utils.common.ProjectFileEncodingUtils;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.xpathchooser.model.XSDElement;
import de.rcenvironment.core.gui.xpathchooser.model.XSDGenerator;

/**
 * Dialog-Version of the XPathChooser for use in interactive views.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Jan Flink
 * @author Adrian Stock
 */
public class XPathChooserDialog extends TitleAreaDialog {

    private static final int STANDARD_WIDTH = 800;

    private static final int STANDARD_HEIGHT = 600;

    /**
     * The logger instance.
     */
    private static final Log LOGGER = LogFactory.getLog(XPathChooserDialog.class);

    /**
     * The dialog creation instance.
     */
    private XPathChooserHelper chooser = null;

    /**
     * Contains all defined variables.
     */
    private VariableEntry selectedVariables;

    /**
     * Constructor for a dialog.
     * 
     * @param aParentShell The parent shell
     * @param root The document
     */
    public XPathChooserDialog(final Shell aParentShell, final XSDElement root) {
        super(aParentShell);
        setBlockOnOpen(true); // doesn't really work when used in RCE
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
        final XSDElement myRoot;
        try {
            if (root == null) {
                String xml = null;
                final IFile ifile = PropertyTabGuiHelper.selectFileFromProjects(aParentShell,
                    "Load XML reference file", "Select an XML document which structure you want to load as reference:", "xml");
                if (ifile != null) {
                    try {
                        xml = ProjectFileEncodingUtils.loadIfileAsString(ifile, "UTF-8");
                    } catch (final CoreException e) {
                        LOGGER.error(e);
                    } catch (final IOException e) {
                        LOGGER.error(e);
                    }
                }
                if (xml != null) { // preference found or resource loaded by user
                    myRoot = new XSDElement(null, "root");
                    myRoot.setElements(XSDGenerator.generate(new StringReader(xml)).getElements());
                    try {
                        myRoot.getElements().get(0).getAttributes().remove(0); // remove schema
                                                                               // location, if
                                                                               // exists
                    } catch (final IndexOutOfBoundsException e) {
                        /* ignore missing schema */
                        LogFactory.getLog(getClass()).debug("Catched IndexOutOfBoundException (Ignore missing schema)");
                    }
                    chooser = new XPathChooserHelper(myRoot);
                }
            } else {
                chooser = new XPathChooserHelper(root);
            }
        } catch (final XMLStreamException e) {
            int lineOfError = e.getLocation().getLineNumber();
            String errorMsg = "Could not parse provided XML document. Error occured at line " + Integer.toString(lineOfError) + ".";
            String errorLog = "Could not parse provided XML document at line " + Integer.toString(lineOfError);
            LOGGER.error(errorLog);
            MessageDialog.openError(aParentShell, "Error", errorMsg);
            chooser = null;
        }
    }

    public XPathChooserHelper getChooser() {
        return chooser;
    }
    
    /**
     * Create the dialog.
     */
    @Override
    public void create() {
        super.create();
        setTitle("XPath Variables Dialog");
        setMessage("Define an XPath by selecting a tree node.",
            IMessageProvider.NONE);
    }

    /**
     * Create the dialog, delegate to factory.
     * 
     * @param parent The area to create in
     * @return The created area
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        parent.setLayout(layout);
        final Composite c = new Composite(container, SWT.FILL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = STANDARD_HEIGHT;
        gd.widthHint = STANDARD_WIDTH;
        c.setLayoutData(gd);
        if (chooser != null) {
            chooser.createContents(c);
        }
        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        return;
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setText("XPath Variables Dialog");
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        if (chooser != null) {
            selectedVariables = chooser.getVariable(); // shallow copy, only references
        }
        setReturnCode(OK);
        close();
    }

    /**
     * When open didn't return CANCEL, this returns the used-defined variables.
     * 
     * @return The selected variables
     */
    public VariableEntry getSelectedVariable() {
        return selectedVariables;
    }

    /**
     * Initialize the view with already known variables, before opening the dialog.
     * 
     * @param variable The variable to set
     */
    public void setSelectedVariable(final VariableEntry variable) {
        selectedVariables = variable;
    }
}
