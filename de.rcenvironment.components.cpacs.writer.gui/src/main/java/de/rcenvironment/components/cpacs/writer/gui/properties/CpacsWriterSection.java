/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.cpacs.writer.common.CpacsWriterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Advanced property tab for ToolWrapper instances.
 * 
 * @author Markus Kunde
 */
public class CpacsWriterSection extends ValidatingWorkflowNodePropertySection {

    private static final int FOLDER_TEXTFIELD_WIDTH = 300;

    private Button fileChooser;

    private Composite fileGroup;

    private Text filePath;

    private Button checkIncremental;

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

        Label localFolderTitle = new Label(client, STANDARD_LABEL_WIDTH);
        localFolderTitle.setText(Messages.localFolderTitle);

        fileGroup = toolkit.createComposite(client);
        fileGroup.setLayout(new GridLayout(2, false));

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = FOLDER_TEXTFIELD_WIDTH;

        filePath = toolkit.createText(fileGroup, "", SWT.SINGLE);
        filePath.setData(CONTROL_PROPERTY_KEY, CpacsWriterComponentConstants.LOCAL_STORE_FOLDER);
        filePath.setLayoutData(gridData);

        fileChooser = toolkit.createButton(fileGroup, Messages.fileLinkButtonLabel, SWT.PUSH);

        checkIncremental = toolkit.createButton(fileGroup, Messages.overwrite, SWT.CHECK | SWT.FLAT);
        checkIncremental.setData(CONTROL_PROPERTY_KEY, CpacsWriterComponentConstants.SAVE_MODE);

        section.setClient(client);
    }

    /**
     * Open file choosing dialog for Mapping file.
     * 
     */
    private void fileChoosing() {
        final String path = getProperty(CpacsWriterComponentConstants.LOCAL_STORE_FOLDER);

        final DirectoryDialog dialog = new DirectoryDialog(fileGroup.getShell());
        dialog.setFilterPath(path);
        dialog.setMessage(Messages.loadMessage);
        dialog.setText(Messages.loadTitle);
        String newPath = dialog.open();

        if (newPath != null) {
            newPath = newPath.trim();
            while (newPath.endsWith("/") || newPath.endsWith("\\")) {
                newPath = newPath.substring(0, newPath.length() - 1).trim();
            }
            if (!newPath.trim().isEmpty()) {
                setProperty(CpacsWriterComponentConstants.LOCAL_STORE_FOLDER, newPath);
                refreshSection();
            }
        }
    }

    @Override
    protected void refreshBeforeValidation() {
        fileGroup.pack(true);
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
            }
        }

    }

}
