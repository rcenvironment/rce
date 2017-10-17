/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.AbstractWorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.executor.properties.AbstractEditScriptRunnable;
import de.rcenvironment.core.gui.workflow.executor.properties.AbstractScriptSection;
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
 * @author Sascha Zur
 */
public class XmlMergerSection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT = 300;

    private Button fileChooser;

    private Button fileEditor;

    private Composite fileGroup;

    private Composite contentGroup;

    private CLabel fileContentLabel;

    private StyledText fileContentText;

    private boolean mappingFileAsInput;

    private Button mappingFileAsInputButton;

    private Button loadMappingFileButton;

    private EditScriptRunnable esr;

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
        loadMappingFileButton = new Button(radioGroup, SWT.RADIO);
        loadMappingFileButton.setText(Messages.mappingFileLoadedButton);

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
        fileContentText = new StyledText(contentGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        fileContentText.setEditable(false);
        fileContentText.setLayoutData(gridData);
        fileContentText.setCaret(null);
        fileContentText.setData(CONTROL_PROPERTY_KEY, XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);

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
                setScriptProperties(node, theString, getMappingType(file.getName()));
            } catch (IOException | CoreException e) {
                logger.error(StringUtils.format(Messages.logReadFromFileError, Messages.cannotReadContentFromFile, e.getMessage()));
                MessageDialog.openError(getComposite().getShell(), Messages.cannotReadContentFromFile,
                    StringUtils.format(Messages.dialogMessageReadFromFileError, e.getMessage(), Messages.refreshProjectExplorer));
            }
            refreshSection();
        }
    }

    private String getMappingType(final String fileName) {
        if (fileName.endsWith(XmlMergerComponentConstants.XMLFILEEND)) {
            return XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC;
        } else {
            return XmlMergerComponentConstants.MAPPINGTYPE_XSLT;
        }
    }

    @Override
    protected Updater createUpdater() {
        return new MergerGUIUpdater();
    }

    @Override
    protected Synchronizer createSynchronizer() {
        return new MergerGUISynchronizer();
    }

    /**
     * Implementation of {@link DefaultUpdater}.
     * 
     * @author Jan Flink
     * @author Sascha Zur
     *
     */
    private class MergerGUIUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            if (!propertyName.equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)) {
                super.updateControl(control, propertyName, newValue, oldValue);
            }
            setXMLFileContent();
        }

    }

    /**
     * Implementation of {@link DefaultSynchronizer}.
     * 
     * @author Jan Flink
     * @author Sascha Zur
     *
     */
    private class MergerGUISynchronizer extends DefaultSynchronizer {

        @Override
        public void handlePropertyChange(String propertyName, String newValue, String oldValue) {
            if (!propertyName.equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)) {
                super.handlePropertyChange(propertyName, newValue, oldValue);
            } else {
                getUpdater().updateControl(null, propertyName, newValue, oldValue);
            }
        }

        @Override
        protected void handlePropertyChange(Control control, String key, String newValue, String oldValue) {
            if (!key.equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)) {
                super.handlePropertyChange(control, key, newValue, oldValue);
            } else {
                getUpdater().updateControl(control, key, newValue, oldValue);
            }
        }

    }

    private void setXMLFileContent() {
        mappingFileAsInput =
            (node.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME)
                .equals(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT));
        mappingFileAsInputButton.setSelection(mappingFileAsInput);
        loadMappingFileButton.setSelection(!mappingFileAsInput);
        fileChooser.setEnabled(!mappingFileAsInput);
        fileEditor.setEnabled(!mappingFileAsInput && !fileContentText.getText().equals(""));
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

    @Override
    protected void refreshBeforeValidation() {
        updateEditor(node);
        setXMLFileContent();
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        setXMLFileContent();
        updateEditor(node);
    }

    @Override
    protected Controller createController() {
        return new FileController();
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refresh();
    }

    private void updateEditor(WorkflowNode node) {
        if (esr != null && fileContentText != null && esr.getNode().equals(node)) {
            esr.update(fileContentText.getText());
        }
    }

    /**
     * Custom {@link DefaultController} implementation to handle the activation of the GUI controls.
     * 
     * @author Markus Kunde
     * 
     */
    private final class FileController extends DefaultController {

        @Override
        protected void widgetSelected(final SelectionEvent event, final Control source) {
            if (source == loadMappingFileButton && loadMappingFileButton.getSelection()) {
                if (XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT
                    .equals(getProperty(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME))) {
                    execute(new ChangeToLoadedMappingFileCommand(node));
                }
            } else if (source == mappingFileAsInputButton && mappingFileAsInputButton.getSelection()) {
                if (XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED
                    .equals(getProperty(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME))) {
                    execute(new ChangeToMappingFileAsInputCommand(node));
                }
            } else {
                super.widgetSelected(event, source);
                if (source == fileChooser) {
                    fileChoosing();
                } else if (source == fileEditor) {
                    esr = new EditScriptRunnable(node);
                    esr.run();
                }
            }
        }
    }

    /**
     * Adds the input for the mapping file.
     *
     */
    private class ChangeToMappingFileAsInputCommand extends AbstractWorkflowNodeCommand {

        private String oldXmlContent;

        private WorkflowNode node;

        ChangeToMappingFileAsInputCommand(WorkflowNode node) {
            this.node = node;
        }

        @Override
        protected void execute2() {
            ConfigurationDescription configDesc = node.getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
            oldXmlContent = configDesc.getConfigurationValue((XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME));
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, "");
            addMappingFileInput(node);
        }

        @Override
        protected void undo2() {
            ConfigurationDescription configDesc = node.getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, oldXmlContent);
            removeMappingFileInput(node);
        }

    }

    /**
     * Remove the input for the mapping file.
     *
     */
    private class ChangeToLoadedMappingFileCommand extends AbstractWorkflowNodeCommand {

        private WorkflowNode node;

        ChangeToLoadedMappingFileCommand(WorkflowNode node) {
            this.node = node;
        }

        @Override
        protected void execute2() {
            ConfigurationDescription configDesc = node.getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
            removeMappingFileInput(getWorkflowNode());
        }

        @Override
        protected void undo2() {
            ConfigurationDescription configDesc = node.getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
                XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_INPUT);
            addMappingFileInput(getWorkflowNode());
        }
    }

    private void addMappingFileInput(WorkflowNode workflowNode) {
        EndpointDescriptionsManager manager = workflowNode.getInputDescriptionsManager();
        Map<String, String> metaData = new HashMap<String, String>();
        manager.addDynamicEndpointDescription(XmlMergerComponentConstants.INPUT_ID_MAPPING_FILE,
            XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE, DataType.FileReference,
            metaData);
    }

    private void removeMappingFileInput(WorkflowNode workflowNode) {
        EndpointDescriptionsManager manager = workflowNode.getInputDescriptionsManager();
        manager.removeDynamicEndpointDescription(XmlMergerComponentConstants.INPUT_NAME_MAPPING_FILE);
    }

    /**
     * Implementation of {@link AbstractEditScriptRunnable}.
     * 
     * @author Jan Flink
     */
    private class EditScriptRunnable extends AbstractEditScriptRunnable {

        private final WorkflowNode node;

        EditScriptRunnable(WorkflowNode node) {
            this.node = node;
        }

        public WorkflowNode getNode() {
            return node;
        }

        @Override
        protected void setScript(String script) {
            setScriptProperties(node, script,
                node.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME));
        }

        @Override
        protected String getScript() {
            return node.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);
        }

        @Override
        protected String getScriptName() {
            String suffix;
            if (node.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME) != null
                && node.getConfigurationDescription().getConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME)
                    .equals(XmlMergerComponentConstants.MAPPINGTYPE_CLASSIC)) {
                suffix = ".xml";
            } else {
                suffix = ".xsl";
            }
            return "Mapping" + suffix;
        }

    }

    /**
     * If a mapping is edited in an editor, the workflow editor must get dirty when the mapping is saved. To do so, a command must be
     * executed, but it must contain the correct node.
     * 
     * TODO Let the {@link XmlMergerSection} extend the {@link AbstractScriptSection} to ensure quality and avoid duplicated code.
     * 
     * @param node to execute the save command to.
     * @param newValue of the mapping.
     * @author Jan Flink
     */
    private void setScriptProperties(WorkflowNode node, final String newScriptValue, final String newTypeValue) {
        final String oldScriptValue = WorkflowNodeUtil.getConfigurationValue(node, XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME);
        final String oldTypeValue = WorkflowNodeUtil.getConfigurationValue(node, XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME);
        if ((oldScriptValue != null && !oldScriptValue.equals(newScriptValue))
            || (oldScriptValue == null && oldScriptValue != newScriptValue)) {
            final WorkflowNodeCommand command =
                new SetScriptPropertiesValueCommand(oldScriptValue, newScriptValue, oldTypeValue, newTypeValue);
            execute(node, command);
        }
    }

    /**
     * Command for setting every property if a file is loaded.
     * 
     *  @author Sascha Zur
     */
    protected static class SetScriptPropertiesValueCommand extends AbstractWorkflowNodeCommand {

        private final String oldScriptValue;

        private final String newScriptValue;

        private String oldTypeValue;

        private String newTypeValue;

        public SetScriptPropertiesValueCommand(final String oldScriptValue, final String newScriptValue, final String oldTypeValue,
            final String newTypeValue) {
            this.oldScriptValue = oldScriptValue;
            this.newScriptValue = newScriptValue;
            this.oldTypeValue = oldTypeValue;
            this.newTypeValue = newTypeValue;
        }

        @Override
        public void execute2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, newScriptValue);
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, newTypeValue);
        }

        @Override
        public void undo2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(XmlMergerComponentConstants.XMLCONTENT_CONFIGNAME, oldScriptValue);
            configDesc.setConfigurationValue(XmlMergerComponentConstants.MAPPINGTYPE_CONFIGNAME, oldTypeValue);

        }

    }
}
