/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * GUI in property tab for some extra tool things.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class VampZeroInitializerSection extends ValidatingWorkflowNodePropertySection {
    
    /**
     * Abstraction for swapping between eclipse and stand-alone widget creation.
     */
    private FormToolkitSwtHelper factory;
    
    /**
     * Reference to the gui model stuff.
     */
    private MainGuiController guiController;
    
    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        
        final Composite content = new LayoutComposite(parent);
        content.setLayout(new GridLayout(1, true));
        content.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        final Composite fileChoosingSection = toolkit.createFlatFormComposite(content);
       
        initVampZeroSection(toolkit, fileChoosingSection);
    }
    
    
    /**
     * Initialize file choosing section.
     * 
     * @param toolkit the toolkit to create section content
     * @param container parent
     */
    private void initVampZeroSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite container) {
        factory = new FormToolkitSwtHelper(container, "Configure parameter");
        guiController = new MainGuiController(factory, new InputTransferable() {

            @Override
            public void transfer(final String text) {
                setProperty(VampZeroInitializerComponentConstants.XMLCONTENT, text);
            }
        });
        guiController.createControls();
    }
    
    
    @Override
    protected void refreshBeforeValidation() {
        if (guiController != null) {
            final String config = getProperty(VampZeroInitializerComponentConstants.XMLCONTENT);
            if (config != null) {
                guiController.setSelectedParameters(config);
            }
        }
        factory.refresh();
    }
    
    @Override
    public void dispose() {
        if (guiController != null) {
            guiController.dispose();
        }
        if (factory != null) {
            factory.dispose();
        }
        super.dispose();
    }

}
