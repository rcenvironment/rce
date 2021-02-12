/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;


/**
 * Excel File implementation for tabbed properties view.
 *
 * @author Markus Kunde
 * @author Patrick Schaefer
 * @author Doreen Seider
 * @author Arne Bachmann
 */
public class FileSection extends ValidatingWorkflowNodePropertySection {


    private static final int FILE_TEXTFIELD_WIDTH = 300;

    private Button fileChooser;
    
    private Composite fileGroup;
    
    private Text xlsFilename;
   
    
    
    
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

        toolkit.createCLabel(client, Messages.fileSectionDescription);

        fileGroup = toolkit.createComposite(client);
        fileGroup.setLayout(new GridLayout(3, false));
        
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = FILE_TEXTFIELD_WIDTH;
        
        fileChooser = toolkit.createButton(fileGroup, Messages.fileLinkButtonLabel, SWT.PUSH);
        
        final int aKeyCode = 97;
        
        xlsFilename = toolkit.createText(fileGroup, "", SWT.SINGLE | SWT.READ_ONLY);
        xlsFilename.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == aKeyCode){
                    xlsFilename.selectAll();
                }
            }
        });
        
        xlsFilename.setData(CONTROL_PROPERTY_KEY, ExcelComponentConstants.XL_FILENAME);
        
        xlsFilename.setLayoutData(gridData);
        
        section.setClient(client);
    }
    
    /**
     * Returns the image descriptor with the given relative path.
     * 
     * @param relativePath relative path to image
     * @return ImageDescriptor of image
     */
    public static ImageDescriptor getImageDescriptor(final String relativePath) {
        ImageDescriptor id = ImageDescriptor.createFromURL(FileSection.class.getClassLoader()
            .getResource(ExcelPropertiesConstants.ICONBASEPATH + relativePath));
        return id;
    }
    
    /**
     * Open file choosing dialog for Excel file.
     * 
     */
    private void fileChoosing() {

        final IFile file = PropertyTabGuiHelper.selectFileFromActiveProject(fileGroup.getShell(), Messages.loadTitle, Messages.loadMessage);
        if (file != null) {
            setProperty(ExcelComponentConstants.XL_FILENAME, file.getFullPath().makeRelative().toString());
            refreshSection();
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
            }
        }

    }

}
