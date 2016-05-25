/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.xpathchooser;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
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
 */
public class XPathChooserDialog extends TitleAreaDialog {

    /**
     * The logger instance.
     */
    private static final Log LOGGER = LogFactory.getLog(XPathChooserDialog.class);

    /**
     * The dialog creation instance.
     */
    private XPathChooserHelper chooser = null;

    /**
     * The parent shell.
     */
    private Shell parentShell;

    /**
     * Contains all defined variables.
     */
    private Set<VariableEntry> selectedVariables = new HashSet<VariableEntry>();

    /**
     * The allowed number of selections in the list.
     */
    private int maxEntries = 1;

    /**
     * Constructor for a dialog.
     * 
     * @param aParentShell The parent shell
     * @param root The document
     */
    public XPathChooserDialog(final Shell aParentShell, final XSDElement root) {
        super(aParentShell);
        setBlockOnOpen(true); // doesn't really work when used in RCE
        parentShell = aParentShell;
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
        final XSDElement myRoot;
        try {
            if (root == null) {
                String cpacs = null;
                final IFile ifile = PropertyTabGuiHelper.selectFileFromProjects(aParentShell,
                    "Load CPACS file", "Select a CPACS xml document which structure you want to load");
                if (ifile != null) {
                    try {
                        cpacs = ProjectFileEncodingUtils.loadIfileAsString(ifile, "UTF-8");
                    } catch (final CoreException e) {
                        LOGGER.error(e);
                    } catch (final IOException e) {
                        LOGGER.error(e);
                    }
                }
                if (cpacs != null) { // preference found or resource loaded by user
                    myRoot = new XSDElement(null, "root");
                    myRoot.setElements(XSDGenerator.generate(new StringReader(cpacs)).getElements());
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
            LOGGER.error("Could not parse provided XML document");
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
        setMessage("Define input and output variables by dragging tree nodes into the table below.",
            IMessageProvider.NONE);
        final Image titleImage = ImageManager.getInstance().getSharedImage(StandardImages.TREE_LARGE);
        setTitleImage(titleImage);
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
        // layout.horizontalAlignment = GridData.FILL;
        parent.setLayout(layout);
        final Composite c = new Composite(container, SWT.None);
        final GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = GridData.CENTER;
        gd.verticalAlignment = GridData.BEGINNING;
        c.setLayoutData(gd);
        if (chooser != null) {
            chooser.createContents(c);
            if (selectedVariables.size() > 0) {
                chooser.setSelectedVariables(selectedVariables);
            }
            if (maxEntries < Short.MAX_VALUE) {
                chooser.setMaximumNumberOfEntries(maxEntries);
            }
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
        final Image headImage = ImageManager.getInstance().getSharedImage(StandardImages.TREE_SMALL);
        shell.setImage(headImage);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        LOGGER.debug("okPressed");
        if (chooser != null) {
            selectedVariables = chooser.getVariables(); // shallow copy, only references
        }
        setReturnCode(OK);
        close();
    }

    /**
     * When open didn't return CANCEL, this returns the used-defined variables.
     * 
     * @return The selected variables
     */
    public Set<VariableEntry> getSelectedVariables() {
        return selectedVariables;
    }

    /**
     * Initialize the view with already known variables, before opening the dialog.
     * 
     * @param variables The variables to add
     */
    public void setSelectedVariables(final Collection<VariableEntry> variables) {
        selectedVariables.clear();
        selectedVariables.addAll(variables);
    }

    /**
     * Restrict the number of entries allowed to choose. This is useful for e.g. single-selections.
     * 
     * @param num The allowed number of selections in the list
     */
    public void setMaximumNumberOfEntries(final int num) {
        maxEntries = num;
    }
}
