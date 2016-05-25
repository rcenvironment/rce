/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.AbstractWorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * GUI in property tab for tool finding.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Markus Litz
 * @author Miriam Lenk
 * @author Jan Flink
 * @author Brigitte Boden
 */
public class XmlMergerSection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT = 300;

    private Button fileChooser;

    private Button fileEditor;

    private Composite fileGroup;

    private Composite contentGroup;

    private CLabel fileContentLabel;

    private Text fileContentText;

    private boolean mappingFileAsInput;

    private Button mappingFileAsInputButton;

    private Button loadMappingFileButton;

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!getComposite().isDisposed()) {
                    refreshSection();
                }
            }
        });
    }

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

        Composite radioGroup = toolkit.createComposite(client);
        radioGroup.setLayout(new GridLayout(2, false));

        mappingFileAsInputButton = new Button(radioGroup, SWT.RADIO);
        mappingFileAsInputButton.setText(Messages.mappingFileAsInputButton);
        mappingFileAsInputButton.setSelection(mappingFileAsInput);
        loadMappingFileButton = new Button(radioGroup, SWT.RADIO);
        loadMappingFileButton.setText(Messages.mappingFileLoadedButton);
        mappingFileAsInputButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                mappingFileAsInput = mappingFileAsInputButton.getSelection();

                if (getProperty(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)
                    .equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT) && !mappingFileAsInput) {
                    execute(new ChangeToLoadedMappingFileCommand());
                } else if (getProperty(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)
                    .equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED) && mappingFileAsInput) {
                    execute(new ChangeToMappingFileAsInputCommand());
                }
            }
        });

        fileGroup = toolkit.createComposite(client);
        fileGroup.setLayout(new GridLayout(2, false));
        fileGroup.setEnabled(!mappingFileAsInput);

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.grabExcessHorizontalSpace = false;

        fileChooser = toolkit.createButton(fileGroup, Messages.fileLinkButtonLabel, SWT.PUSH);

        fileEditor = toolkit.createButton(fileGroup, Messages.fileEditorButtonLabel, SWT.PUSH);

        layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        contentGroup = toolkit.createComposite(client);
        contentGroup.setLayoutData(layoutData);
        contentGroup.setLayout(new GridLayout(1, false));

        fileContentLabel = toolkit.createCLabel(contentGroup, Messages.actuallyLoadedLabel);

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
            } catch (IOException | CoreException e) {
                logger.error(StringUtils.format(Messages.logReadFromFileError, Messages.cannotReadContentFromFile, e.getMessage()));
                MessageDialog.openError(getComposite().getShell(), Messages.cannotReadContentFromFile,
                    StringUtils.format(Messages.dialogMessageReadFromFileError, e.getMessage(), Messages.refreshProjectExplorer));
            }

            refreshSection();
        }
    }

    /**
     * Open file Editor for Mapping file.
     * 
     */
    private void fileEditing() {
        Runnable editMappingFileRunnable = new EditMappingFileRunnable(node);
        editMappingFileRunnable.run();
        setXMLContent();
        
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
        mappingFileAsInput =
            (getProperty(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)
                .equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT));
        setXMLContent();
        mappingFileAsInputButton.setSelection(mappingFileAsInput);
        loadMappingFileButton.setSelection(!mappingFileAsInput);
        fileChooser.setEnabled(!mappingFileAsInput);
        fileEditor.setEnabled(!mappingFileAsInput && getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME) != null);
        fileContentLabel.setEnabled(!mappingFileAsInput);
        fileContentText.setEnabled(!mappingFileAsInput);
        if (mappingFileAsInput) {
            fileContentLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
            // Setting the focus to the selected radio button. This is necessary when the button has been selected automatically (e.g. after
            // a "redo" operation), because otherwise a "save" operation will change the button selection again, causing the bug in
            // https://mantis.sc.dlr.de/view.php?id=13578
            if (loadMappingFileButton.isFocusControl()) {
                mappingFileAsInputButton.setFocus();
            }
        } else {
            fileContentLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));
            if (mappingFileAsInputButton.isFocusControl()) {
                loadMappingFileButton.setFocus();
            }
        }
        fileGroup.pack(true);
    }

    private void setXMLContent() {
        if (!fileContentText.isDisposed()) {
            if (getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME) != null) {
                fileContentText.setText(getProperty(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME));
                return;
            }
            fileContentText.setText("");
        }
    }

    @Override
    protected Controller createController() {
        return new FileController();
    }

    /**
     * Custom {@link DefaultController} implementation to handle the activation of the GUI controls.
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

    /**
     * Adds the input for the mapping file.
     * 
     */
    private class ChangeToMappingFileAsInputCommand extends AbstractWorkflowNodeCommand {

        private String oldXmlContent;

        @Override
        protected void execute2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
            oldXmlContent = configDesc.getConfigurationValue((XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME));
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, null);
            addMappingFileInput(getWorkflowNode());
        }

        @Override
        protected void undo2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, oldXmlContent);
            removeMappingFileInput(getWorkflowNode());
        }

    }

    /**
     * Remove the input for the mapping file.
     * 
     */
    private class ChangeToLoadedMappingFileCommand extends AbstractWorkflowNodeCommand {

        @Override
        protected void execute2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
            removeMappingFileInput(getWorkflowNode());
        }

        @Override
        protected void undo2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
            addMappingFileInput(getWorkflowNode());
        }
    }

    private void addMappingFileInput(WorkflowNode workflowNode) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        Map<String, String> metaData = new HashMap<String, String>();
        manager.addDynamicEndpointDescription(XmlMergerComponentConstants.INPUT_ID_MAPPING_FILE,
            XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE, DataType.FileReference,
            metaData);
    }

    private void removeMappingFileInput(WorkflowNode workflowNode) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        manager.removeDynamicEndpointDescription(XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE);
    }

}
