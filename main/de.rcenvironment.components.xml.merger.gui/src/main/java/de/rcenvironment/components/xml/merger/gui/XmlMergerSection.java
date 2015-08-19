/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileEncodingUtils;

/**
 * GUI in property tab for tool finding.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Markus Litz
 * @author Miriam Lenk
 * @author Jan Flink
 */
public class XmlMergerSection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT = 300;

    private Button fileChooser;
    
    private Button fileEditor;
    
    private Composite fileGroup;

    private Composite contentGroup;

    private Text fileContentText;
 
    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        
        final Composite content = new LayoutComposite(parent);
        content.setLayout(new GridLayout(1, true));

        final Composite fileChoosingSection = toolkit.createFlatFormComposite(content);
        initFileChoosingSection(toolkit, fileChoosingSection);
    }
    
    
    /**
     * Initialize file choosing section.
     * 
     * @param toolkit the toolkit to create section content
     * @param container parent
     */
    private void initFileChoosingSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite container) {
        GridData layoutData;
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        container.setLayoutData(layoutData);
        container.setLayout(new FillLayout());
        final Section section = toolkit.createSection(container, Section.TITLE_BAR | Section.EXPANDED);
        section.setText(Messages.fileChoosingSectionName);
        final Composite client = toolkit.createComposite(section);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        client.setLayoutData(layoutData);
        client.setLayout(new GridLayout(1, false));

        fileGroup = toolkit.createComposite(client);
        fileGroup.setLayout(new GridLayout(2, false));
        
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.grabExcessHorizontalSpace = false;
        
        fileChooser = toolkit.createButton(fileGroup, Messages.fileLinkButtonLabel, SWT.PUSH);
        
        fileEditor = toolkit.createButton(fileGroup, Messages.fileEditorButtonLabel, SWT.PUSH);
        
        layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        contentGroup = toolkit.createComposite(client);
        contentGroup.setLayoutData(layoutData);
        contentGroup.setLayout(new GridLayout(1, false));

        toolkit.createCLabel(contentGroup, Messages.actuallyLoadedLabel);

        gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gridData.heightHint = MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT;
        fileContentText = toolkit.createText(contentGroup, "", SWT.V_SCROLL | SWT.H_SCROLL);
        fileContentText.setEditable(false);
        fileContentText.setLayoutData(gridData);

        section.setClient(client);
    }
    
    /**
     * Open file choosing dialog for Mapping file.
     * 
     */
    private void fileChoosing() {

        final IFile file = PropertyTabGuiHelper.selectFileFromProjects(fileGroup.getShell(), Messages.loadTitle, Messages.loadMessage);
        if (file != null) {
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(file.getContents(), writer);
                String theString = writer.toString();
                setProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, theString);
                setMappingType(file.getName());
            } catch (IOException e) {
                logger.error("Cannot read content from file.");
            } catch (CoreException e) {
                logger.error("Cannot read content from file.");
            }
            
            refreshSection();
        }
    }
    
    /**
     * Open file Editor for Mapping file.
     * 
     */
    private void fileEditing() {
        try {
            final File tempFile =
                TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("mapping.xsl");
            String content = getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);

            FileEncodingUtils.saveUnicodeStringToFile(content, tempFile);
            
            EditorsHelper.openExternalFileInEditor(tempFile, new Runnable[] {
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String newValue = FileEncodingUtils.loadUnicodeStringFromFile(tempFile);
                            setProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, newValue);
                            setXMLContent();
                            if (getProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME) == null
                                || (getProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME) 
                                    instanceof String
                                && getProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME)
                                    .isEmpty())) {
                                // Just guessing it is XSLT
                                setProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME,
                                    XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
                            }
                        } catch (final IOException e) {
                            logger.error("Could not read temporary edited file", e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Could not create temporary file", e);
        } catch (PartInitException e) {
            logger.error(e);
        }
    }
    
    private void setMappingType(final String fileName) {
        if (fileName.endsWith(XmlMergerComponentConstants.XMLFILEEND)) {
            setProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC);
        } else {
            setProperty(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, XmlMergerComponentConstants.MAPPINGTYPE_XSLT);
        }
    }
    
    @Override
    protected void refreshBeforeValidation() {
        fileEditor.setEnabled(getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME) != null);
        setXMLContent();
        fileGroup.pack(true);
    }

    private void setXMLContent() {
        if (getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME) != null) {
            fileContentText.setText(getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME));
            return;
        }
        fileContentText.setText("");
    }

    
    @Override
    protected Controller createController() {
        return new FileController();
    }

    /**
     * Custom {@link DefaultController} implementation to handle the activation of the GUI
     * controls.
     * 
     * @author Markus Kunde
     */
    private final class FileController extends DefaultController {

        @Override
        protected void widgetSelected(final SelectionEvent event, final Control source) {
            super.widgetSelected(event, source);
            if (source == fileChooser) {
                fileChoosing();
            } else if (source == fileEditor) {
                fileEditing();
            }
        }

    }
}
