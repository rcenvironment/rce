/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.gui.properties;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.ExcelServiceAccess;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * "Properties" view tab for defining macros to run.
 * 
 * @author Patrick Schaefer
 * @author Markus Kunde
 */
public class MacrosSection extends ValidatingWorkflowNodePropertySection {

    private Object lock = new Object();

    private Composite macroGroup;

    private CCombo comboMacroPre;

    private CCombo comboMacroRun;

    private CCombo comboMacroPost;

    private Button discoverMacrosButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();

        final Composite content = new LayoutComposite(parent);
        content.setLayout(new GridLayout(1, true));

        final Composite macrosChoosingSection = toolkit.createFlatFormComposite(content);
        initMacrosChoosingSection(toolkit, macrosChoosingSection);
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        discoverMacros();
    }

    /**
     * Initialize macro choosing section.
     * 
     * @param toolkit   the toolkit to create section content
     * @param container parent
     */
    private void initMacrosChoosingSection(final TabbedPropertySheetWidgetFactory toolkit, final Composite container) {
        GridData layoutData;
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        container.setLayoutData(layoutData);
        container.setLayout(new FillLayout());
        final Section section = toolkit.createSection(container, Section.TITLE_BAR | Section.EXPANDED);
        section.setText(Messages.macrosChoosingSectionName);
        final Composite client = toolkit.createComposite(section);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        client.setLayoutData(layoutData);
        client.setLayout(new GridLayout(1, false));

        CLabel lblDescription = toolkit.createCLabel(client, Messages.macrosSectionDescription);

        macroGroup = toolkit.createComposite(client);
        macroGroup.setLayout(new GridLayout(2, true));

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;

        toolkit.createCLabel(macroGroup, Messages.preMacro);
        comboMacroPre = toolkit.createCCombo(macroGroup);
        comboMacroPre.setEditable(true);
        comboMacroPre.setData(CONTROL_PROPERTY_KEY, ExcelComponentConstants.PRE_MACRO);
        toolkit.createCLabel(macroGroup, Messages.runMacro);
        comboMacroRun = toolkit.createCCombo(macroGroup);
        comboMacroRun.setEditable(true);
        comboMacroRun.setData(CONTROL_PROPERTY_KEY, ExcelComponentConstants.RUN_MACRO);
        toolkit.createCLabel(macroGroup, Messages.postMacro);
        comboMacroPost = toolkit.createCCombo(macroGroup);
        comboMacroPost.setEditable(true);
        comboMacroPost.setData(CONTROL_PROPERTY_KEY, ExcelComponentConstants.POST_MACRO);

        toolkit.createCLabel(macroGroup, "");
        discoverMacrosButton = toolkit.createButton(macroGroup, Messages.macrosDiscoverButtonLabel, SWT.PUSH);
        discoverMacrosButton.setImage(ImageManager.getInstance().getSharedImage(StandardImages.REFRESH_16));

        lblDescription.setLayoutData(gridData);
        comboMacroPre.setLayoutData(gridData);
        comboMacroRun.setLayoutData(gridData);
        comboMacroPost.setLayoutData(gridData);

        section.setClient(client);

    }

    /**
     * Discover all macros available in Excel file and fill Combo-lists with them.
     * 
     */
    private void discoverMacros() {
        ConcurrencyUtils.getAsyncTaskService().execute("Browses the given excel file for macros", () -> {

            ExcelService excelService = ExcelServiceAccess.get();
            File xlFile = ExcelUtils.getAbsoluteFile(getProperty(ExcelComponentConstants.XL_FILENAME));
            if (xlFile != null) {
                final String[] macrosAvailable;
                synchronized (lock) {
                    macrosAvailable = excelService.getMacros(xlFile);
                }
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    @TaskDescription("Sets the items of the macro combo boxes")
                    public void run() {
                        synchronized (lock) {
                            if (!comboMacroPre.isDisposed() && !comboMacroRun.isDisposed() && !comboMacroPost.isDisposed()) {
                                comboMacroPre.setItems(macrosAvailable);
                                comboMacroRun.setItems(macrosAvailable);
                                comboMacroPost.setItems(macrosAvailable);
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void refreshBeforeValidation() {
        macroGroup.pack(true);
    }

    @Override
    protected Controller createController() {
        return new MacrosController();
    }

    /**
     * Custom {@link DefaultController} implementation to handle the activation of the GUI controls.
     * 
     * @author Markus Kunde
     */
    private final class MacrosController extends DefaultController {

        @Override
        protected void widgetSelected(final SelectionEvent event, final Control source) {
            super.widgetSelected(event, source);
            if (source == discoverMacrosButton) {
                discoverMacros();
            }
        }

    }

}
