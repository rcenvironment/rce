/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.gui.properties;

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
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * GUI in property tab for tool finding.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Markus Litz
 * @author Jan Flink
 */
public class XmlLoaderComponentSection extends ValidatingWorkflowNodePropertySection {

    private static final int MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT = 300;

    private Button fileChooser;

    private Text fileContentText;

    private LayoutComposite content;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();

        content = new LayoutComposite(parent);
        content.setLayout(new FillLayout());

        initFileChoosingSection(toolkit, content);
    }

    /**
     * Initialize file choosing section.
     * 
     * @param toolkit the toolkit to create section content
     * @param container parent
     */
    private void initFileChoosingSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite container) {

        final Section section = toolkit.createSection(container, Section.TITLE_BAR);
        section.setText(Messages.fileChoosingSectionName);

        final Composite client = toolkit.createComposite(section);
        client.setLayout(new GridLayout());

        fileChooser = toolkit.createButton(client, Messages.fileLinkButtonLabel, SWT.PUSH);

        toolkit.createCLabel(client, Messages.actuallyLoadedLabel);

        GridData gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gridData.heightHint = MINIMUM_HEIGHT_OF_FILE_CONTENT_TEXT;
        fileContentText = toolkit.createText(client, "", SWT.V_SCROLL | SWT.H_SCROLL);
        fileContentText.setEditable(false);
        fileContentText.setLayoutData(gridData);

        section.setClient(client);

    }

    /**
     * Open file choosing dialog for XML file.
     * 
     */
    private void fileChoosing() {

        final IFile file = PropertyTabGuiHelper.selectFileFromProjects(content.getShell(), Messages.loadTitle, Messages.loadMessage);
        if (file != null) {
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(file.getContents(), writer);
                String theString = writer.toString();
                setProperty(XmlLoaderComponentConstants.XMLCONTENT, theString);
            } catch (IOException | CoreException e) {
                logger.error("Cannot read content from file.", e);
            }

            refreshSection();
        }
    }

    @Override
    protected void refreshBeforeValidation() {
        setXMLContent();
    }

    private void setXMLContent() {
        if (getProperty(XmlLoaderComponentConstants.XMLCONTENT) != null) {
            fileContentText.setText(getProperty(XmlLoaderComponentConstants.XMLCONTENT));
            return;
        }
        fileContentText.setText("");
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
