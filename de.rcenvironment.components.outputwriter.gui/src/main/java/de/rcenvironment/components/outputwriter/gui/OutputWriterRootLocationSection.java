/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodePropertySection;

/**
 * Creates a "Properties" view tab for configuring output writer root location properties.
 * 
 * @author Oliver Seebach
 * @author Kathrin Schaffert
 * 
 */
public class OutputWriterRootLocationSection extends WorkflowNodePropertySection {

    private static final String FRONT_SLASH = "/";

    private Button workflowStartCheckbox;

    private Text rootText;

    private Button rootSelectFromProjectButton;

    private Button rootSelectFromFileSystemButton;

    private Composite noteComposite;

    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {

        parent.setLayout(new FillLayout(SWT.VERTICAL | SWT.V_SCROLL));
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        Composite root = new LayoutComposite(parent);
        Composite rootComposite = toolkit.createFlatFormComposite(root);
        rootComposite.setLayout(new GridLayout(1, true));

        createRootSection(rootComposite, toolkit);

        rootComposite.layout();
    }

    private Composite createRootSection(final Composite parent, FormToolkit toolkit) {

        final Section sectionProperties = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.rootFolderSectionTitle);
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        sectionProperties.setLayoutData(layoutData);

        Composite rootgroup = toolkit.createComposite(sectionProperties);
        rootgroup.setLayout(new GridLayout(2, false));

        rootText = new Text(rootgroup, SWT.BORDER);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 2;

        rootText.setLayoutData(gridData);
        rootText.setEditable(true);
        rootText.setData(CONTROL_PROPERTY_KEY, OutputWriterComponentConstants.CONFIG_KEY_ROOT);

        workflowStartCheckbox = new Button(rootgroup, SWT.CHECK);
        workflowStartCheckbox.setText(Messages.selectAtStart);

        workflowStartCheckbox.setLayoutData(new GridData(SWT.LEFT,
            SWT.TOP, true, false, 2, 1));
        workflowStartCheckbox.setData(CONTROL_PROPERTY_KEY, OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);

        rootSelectFromProjectButton = new Button(rootgroup, SWT.NONE);
        rootSelectFromProjectButton.setText("Select from project ...");
        rootSelectFromProjectButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
            false, 1, 1));
        rootSelectFromProjectButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                IResource resource = PropertyTabGuiHelper.selectDirectoryFromActiveProjectIncludingItsRoot(
                    Display.getCurrent().getActiveShell(), "Select directory", "Select"
                        + " directory from project");
                if (resource != null) {
                    String selectedPath = resource.getFullPath().makeRelative().toPortableString();
                    rootText.setText(OutputWriterComponentConstants.PH_WORKSPACE + FRONT_SLASH + selectedPath);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        rootSelectFromFileSystemButton = new Button(rootgroup, SWT.NONE);
        rootSelectFromFileSystemButton.setText("Select from file system ...");
        rootSelectFromFileSystemButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
            false, 1, 1));

        rootSelectFromFileSystemButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
                dialog.setText("Select directory");
                dialog.setMessage("Select directory from file system");
                checkIfPathExists(dialog, rootText.getText());
                String path = dialog.open();
                if (path != null) {
                    rootText.setText(path);
                }
            }

            private void checkIfPathExists(Dialog dialog, String text) {

                // if the path in the root text field does not exist -> use default
                File f = new File(text);
                if (!f.exists()) {
                    return;
                }

                Path isThisPathExisting = Paths.get(text);

                if (Files.exists(isThisPathExisting, LinkOption.NOFOLLOW_LINKS)) {
                    if (dialog instanceof DirectoryDialog) {
                        ((DirectoryDialog) dialog).setFilterPath(isThisPathExisting.toString());
                    } else {

                        // Type FileDialog
                        if (isThisPathExisting.getParent() != null) {
                            ((FileDialog) dialog).setFilterPath(isThisPathExisting.getParent().toString());

                        } else {
                            // The path value is empty. This would open the last opened FileDialog if we not set the root path.
                            File[] paths = File.listRoots();
                            if (paths[0].getPath() != null) {
                                ((FileDialog) dialog).setFilterPath(paths[0].getPath());
                            } else {
                                return;
                            }

                        }

                    }

                    return;
                }

                if (isThisPathExisting.getParent() != null) {
                    checkIfPathExists(dialog, isThisPathExisting.getParent().toString());
                } else {
                    return;
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        noteComposite = new Composite(rootgroup, SWT.NONE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        noteComposite.setLayoutData(gridData);
        noteComposite.setLayout(new GridLayout(2, false));

        Label warnLabel = new Label(noteComposite, SWT.READ_ONLY);
        warnLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
        Label noteLabel = new Label(noteComposite, SWT.READ_ONLY);
        warnLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        noteLabel.setText(Messages.note);

        sectionProperties.setClient(rootgroup);
        sectionProperties.setVisible(true);

        return rootgroup;
    }

    private void setEnabilityRoot(boolean enabled) {
        noteComposite.setVisible(enabled);
        rootText.setEnabled(enabled);
        rootSelectFromProjectButton.setEnabled(enabled);
        rootSelectFromFileSystemButton.setEnabled(enabled);
    }

    @Override
    protected void refreshSection() {
        super.refreshSection();
        setEnabilityRoot(!workflowStartCheckbox.getSelection());
    }

    @Override
    protected RootLocationDefaultUpdater createUpdater() {
        return new RootLocationDefaultUpdater();
    }

    /**
     * 
     * Root Location {@link DefaultUpdater} implementation of the handler to update the Root folder UI.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class RootLocationDefaultUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);
            if (control instanceof Button) {
                setEnabilityRoot(!Boolean.valueOf(newValue));
            }
        }
    }

}
