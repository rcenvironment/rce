/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.utils.incubator.xml.XMLException;

/**
 * GUI-Controller handling the main Vampzero application. This controller doesn't control resource handles, it's all delegated to the
 * factory and the caller.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class MainGuiController {

    private static final int SIZE_200 = 200;

    /**
     * The logger.
     */
    private static final Log LOGGER = LogFactory.getLog(MainGuiController.class);

    /**
     * Contains all parameters that were modified by the user.
     */
    private List<Component> modifiedComponents = new ArrayList<Component>();

    /**
     * Gui creation factory.
     */
    private FormToolkitSwtHelper factory;

    /**
     * The tree viewer.
     */
    private TreeViewer viewer;

    /**
     * Listener to call when "store cpacs" is clicked.
     */
    private InputTransferable listener;

    /**
     * Path to a gui.xml on the local file system.
     */
    private String xmlPath = null;

    /**
     * All widgets to dispose after closing the app.
     */
    private List<Control> disposables = new LinkedList<Control>();

    /**
     * All components in the GUI definition file.
     */
    private List<Component> components;
    
    private GuiInputParser guiInputParser = new GuiInputParser();
    
    private ToolspecificOutputWriter toolspecificOutputWriter = new ToolspecificOutputWriter();

    /**
     * Constructor.
     * 
     * @param factory The factory to use when creating this gui.
     * @param performListener The listener to activate when hitting save
     */
    public MainGuiController(final FormToolkitSwtHelper factory, final InputTransferable performListener) {
        this.factory = factory;
        this.listener = performListener;
    }

    /**
     * Constructor.
     * 
     * @param factory The factory to use when creating this gui.
     * @param performListener The listener to activate when hitting save
     * @param guiXmlpath Local file system path to load the GUI description from
     */
    public MainGuiController(final FormToolkitSwtHelper factory, final InputTransferable performListener, final String guiXmlPath) {
        this.factory = factory;
        this.listener = performListener;
        this.xmlPath = guiXmlPath;
    }

    /**
     * Remove all unmanaged widgets, in Eclipse most are managed by the FormFactory.
     */
    public void dispose() {
        while (disposables.size() > 0) {
            if (disposables instanceof LinkedList) {
                ((LinkedList<Control>) disposables).getLast().dispose();
                ((LinkedList<Control>) disposables).removeLast();
            }
        }
    }

    /**
     * Gui creation helper. Use either the provided file path or load a standard GUI config from the bundle.
     * 
     * @return The main controls
     */
    public Composite createControls() {
        List<Component> componentsTemp = null;
        if ((xmlPath != null) && (new File(xmlPath).canRead())) {
            FileInputStream fis;
            boolean killSwitch = false;
            try {
                fis = new FileInputStream(xmlPath);
                componentsTemp = guiInputParser.parse(fis);
                fis.close();
            } catch (XPathExpressionException | IOException | XMLException e) {
                LOGGER.error(e);
                killSwitch = true;
            }
            if (killSwitch) {
                return null;
            }
        } else {
            try {
                componentsTemp = guiInputParser.parse(getClass().getClassLoader().getResourceAsStream("resources/gui.xml"));
            } catch (final XPathExpressionException | XMLException e) {
                LOGGER.error(e.getCause().getMessage());
                componentsTemp = new ArrayList<Component>();
            }
        }
        components = componentsTemp;

        final Composite mainComposite = factory.createMainComposite();
        disposables.add(mainComposite);
        final TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = 5;
        layout.horizontalSpacing = 4;
        layout.verticalSpacing = 4;
        mainComposite.setLayout(layout);

        // drop down boxes and main mask
        final Composite comboComposite = factory.createComposite(mainComposite, 3, 3);
        disposables.add(comboComposite);

        final Combo componentCombo = factory.createCombo(comboComposite, null, null);
        final Combo disciplineCombo = factory.createCombo(comboComposite, null, null);
        final Button loadButton = factory.createButton(comboComposite, "Load configuration", new Listener() {

            @Override
            public void handleEvent(final Event event) {
                IFile file = PropertyTabGuiHelper.selectFileFromProjects(comboComposite.getShell(), "Foo", "FooBar");
                if (file != null) {
                    StringWriter writer = new StringWriter();
                    try {
                        IOUtils.copy(file.getContents(), writer);
                        String theString = writer.toString();
                        setSelectedParameters(theString);
                    } catch (IOException e) {
                        LOGGER.error("Cannot read content from file.");
                    } catch (CoreException e) {
                        LOGGER.error("Cannot read content from file.");
                    }
                }
            }
        });
        disposables.add(componentCombo);
        disposables.add(disciplineCombo);
        disposables.add(loadButton);

        final Label separator = factory.createSeparator(mainComposite, true);
        ((TableWrapData) separator.getLayoutData()).valign = TableWrapData.FILL;
        ((TableWrapData) separator.getLayoutData()).rowspan = 2;
        disposables.add(separator);

        final Composite rightComposite = factory.createComposite(mainComposite);
        disposables.add(rightComposite);
        final TableWrapData td = new TableWrapData();
        td.align = TableWrapData.FILL;
        td.valign = TableWrapData.FILL;
        td.grabHorizontal = true;
        td.grabVertical = true;
        td.rowspan = 2;
        rightComposite.setLayoutData(td);
        rightComposite.setLayout(new GridLayout(1, false));
        final Button createButton = new Button(rightComposite, SWT.PUSH | SWT.FLAT);
        disposables.add(createButton);
        createButton.setText("Create CPACS");
        createButton.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                listener.transfer(toolspecificOutputWriter.createOutput(modifiedComponents));
            }
        });
        createButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        createButton.setImage(factory.saveImage);
        viewer = new TreeViewer(rightComposite, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);

        disposables.add(viewer.getTree());
        viewer.setContentProvider(new ParameterTreeContentProvider());
        viewer.setLabelProvider(new ParameterTreeLabelProvider(viewer.getTree().getDisplay()));
        ColumnViewerToolTipSupport.enableFor(viewer);
        final Set<List<Component>> set = new HashSet<List<Component>>();
        set.add(modifiedComponents); // can't use newSet(collection...) because then not the list
                                     // but its contents are added,which are none
        viewer.setInput(set); // create root "set" with the list of all components in it
        final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.minimumHeight = SIZE_200;
        gd.minimumWidth = SIZE_200;

        final Composite contentComposite = factory.createComposite(mainComposite, 1, 1);
        disposables.add(contentComposite);

        contentComposite.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.FILL, 1, 1));
        contentComposite.setLayout(new GridLayout(1, false));

        // initiall combo fillings
        for (final Component component : components) {
            componentCombo.add(component.getName());
        }
        for (final Discipline discipline : components.get(0).getDisciplines()) {
            disciplineCombo.add(discipline.getName());
        }

        final ParameterCompositeWrapper parameterCompositeWrapper =
            new ParameterCompositeWrapper(factory, contentComposite, modifiedComponents);
        parameterCompositeWrapper.setDiscipline(components.get(0).getDisciplines().get(0));
        parameterCompositeWrapper.setViewer(viewer);
        viewer.getTree().setLayoutData(gd);
        viewer.getTree().addListener(SWT.Selection,
            getTreeViewListener(componentCombo, disciplineCombo, parameterCompositeWrapper, contentComposite));
        viewer.getTree().addKeyListener(getKeyAdapter());
        componentCombo.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final String newSelection = componentCombo.getText();
                final Component component = getComponentForName(components, newSelection);
                fillDisciplines(disciplineCombo, component);
                parameterCompositeWrapper.setDiscipline(component.getDisciplines().get(0));
                parameterCompositeWrapper.redraw();
                disciplineCombo.select(0);
            }
        });
        disciplineCombo.addListener(SWT.Selection, getDisciplineComboListener(disciplineCombo, parameterCompositeWrapper, componentCombo));
        componentCombo.select(0); // initial selection is not empty
        disciplineCombo.select(0);
        return mainComposite;
    }

    private Listener getDisciplineComboListener(final Combo disciplineCombo, final ParameterCompositeWrapper parameterCompositeWrapper,
        final Combo componentCombo) {
        return new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final String newSelection = disciplineCombo.getText();
                final Component component = getComponentForName(components, componentCombo.getText());
                parameterCompositeWrapper.setDiscipline(component.getDisciplineForName(newSelection));
                parameterCompositeWrapper.redraw();
            }
        };
    }

    private Listener getTreeViewListener(final Combo componentCombo, final Combo disciplineCombo,
        final ParameterCompositeWrapper parameterCompositeWrapper, final Composite contentComposite) {
        return new Listener() {

            @Override
            public void handleEvent(final Event event) {
                if (viewer.getTree().getSelectionCount() != 1) {
                    return; // to preven empty selection (if possible at all)
                }
                final TreeItem[] items = viewer.getTree().getSelection();
                TreeItem item = items[0];
                String path = item.getText();
                while (item.getParentItem() != null) {
                    item = item.getParentItem();
                    path = item.getText() + "/\\/" + path;
                }
                final String[] parameterBreakDown = path.split("/\\\\/"); // component/discipline/parameter
                if (parameterBreakDown.length < 2) { // no need to change comp/disc/param ( first is empty)
                    return;
                }
                if (!componentCombo.getText().equals(parameterBreakDown[1])) { // we need to switch the component
                    final Component component = getComponentForName(components, parameterBreakDown[1]);
                    componentCombo.select(componentCombo.indexOf(component.getName()));
                    fillDisciplines(disciplineCombo, component);
                    if (parameterBreakDown.length > 2) { // a discipline has been clicked
                        parameterCompositeWrapper.setDiscipline(component.getDisciplineForName(parameterBreakDown[2]));
                        parameterCompositeWrapper.redraw();
                        disciplineCombo.select(disciplineCombo.indexOf(parameterBreakDown[2]));
                    } else { // no discipline is set: take first one
                        parameterCompositeWrapper.setDiscipline(component.getDisciplines().get(0));
                        parameterCompositeWrapper.redraw();
                        disciplineCombo.select(0);
                    }
                } else if (parameterBreakDown.length > 2) {
                    if (!disciplineCombo.getText().equals(parameterBreakDown[2])) {
                        parameterCompositeWrapper.setDiscipline(getComponentForName(components, parameterBreakDown[1])
                            .getDisciplineForName(parameterBreakDown[2]));
                        parameterCompositeWrapper.redraw();
                        disciplineCombo.select(disciplineCombo.indexOf(parameterBreakDown[2]));
                    }
                }
                if (parameterBreakDown.length > 3) { // a parameter name has been clicked
                    for (final Component component : modifiedComponents) {
                        if (component.getName().equals(parameterBreakDown[1])) {
                            final Parameter param =
                                component.getDisciplineForName(parameterBreakDown[2]).getParameterForName(parameterBreakDown[3]);
                            parameterCompositeWrapper.setName(param.getName());
                            parameterCompositeWrapper.setDescription(param.getDescription());
                            parameterCompositeWrapper.setValue(param.getValue());
                            parameterCompositeWrapper.setFactor(param.getFactor());
                        }
                    }
                    final org.eclipse.swt.widgets.List paramList =
                        (org.eclipse.swt.widgets.List) ((Composite) ((Composite) contentComposite).getChildren()[0]).getChildren()[0];
                    for (int i = 0, n = paramList.getItemCount(); i < n; i++) { // find the correct
                                                                                // parameter in the
                                                                                // list
                        if (paramList.getItem(i).equals(parameterBreakDown[3])) {
                            paramList.setSelection(i);
                            break;
                        }
                    }
                }
            }
        };
    }

    private KeyListener getKeyAdapter() {
        return new KeyAdapter() {

            @Override
            public void keyReleased(final KeyEvent event) {
                if (event.character == SWT.DEL) {
                    if (viewer.getTree().getSelectionCount() != 1) {
                        return; // to preven empty selection (if possible at all)
                    }
                    final TreeItem[] items = viewer.getTree().getSelection();
                    TreeItem item = items[0];
                    String path = item.getText();
                    while (item.getParentItem() != null) {
                        item = item.getParentItem();
                        path = item.getText() + "/\\/" + path;
                    }
                    final String[] parameterBreakDown = path.split("/\\\\/"); // component/discipline/parameter
                    if (parameterBreakDown.length < 4) { // only remove params
                        return;
                    }
                    final Discipline discipline = getComponentForName(modifiedComponents, parameterBreakDown[1])
                        .getDisciplineForName(parameterBreakDown[2]);
                    discipline.getParameters().remove(discipline.getParameterForName(parameterBreakDown[3]));
                    if (discipline.getParameters().size() == 0) {
                        discipline.getComponent().getDisciplines().remove(discipline);
                        if (discipline.getComponent().getDisciplines().size() == 0) {
                            modifiedComponents.remove(discipline.getComponent());
                        }
                    }
                    viewer.refresh();
                }
            }
        };
    }

    /**
     * Helper to update the drop-down box.
     * 
     * @param disciplineCombo The combo
     * @param component The parent component
     */
    private void fillDisciplines(final Combo disciplineCombo, final Component component) {
        disciplineCombo.removeAll();
        for (final Discipline discipline : component.getDisciplines()) {
            disciplineCombo.add(discipline.getName());
        }
    }

    /**
     * Call this with an existing created configuration to set the GUI contents accordingly. The routine doesn't allow non-existing entries
     * of the initially loaded GUI definition file.
     * 
     * @param configuration The configuration to parse as one XML string.
     */
    public void setSelectedParameters(final String configuration) {
        try {
            final List<Component> setComponents = guiInputParser.parse(configuration);
            modifiedComponents.clear();
            for (final Component setComponent : setComponents) {
                final Component compDefinition = getComponentForName(components, setComponent.getName());
                if (compDefinition == null) {
                    LOGGER.warn("Ignoring component setting " + setComponent.getName());
                    continue; // skip loaded setting, because it's not allowed
                }
                if (getComponentForName(modifiedComponents, setComponent.getName()) != null) {
                    LOGGER.warn("Ignoring duplicate component setting " + setComponent.getName());
                    continue;
                }
                final Component newComponent = new Component();
                newComponent.setName(setComponent.getName());
                modifiedComponents.add(newComponent);
                for (final Discipline setDiscipline : setComponent.getDisciplines()) {
                    final Discipline discDefinition = compDefinition.getDisciplineForName(setDiscipline.getName());
                    if (discDefinition == null) {
                        LOGGER.warn("Ignoring discipline setting " + setDiscipline.getName());
                        continue;
                    }
                    if (newComponent.getDisciplineForName(setDiscipline.getName()) != null) {
                        LOGGER.warn("Ignoring duplicate discipline setting " + setDiscipline.getName());
                        continue;
                    }
                    final Discipline newDiscipline = new Discipline();
                    newDiscipline.setName(setDiscipline.getName());
                    newComponent.getDisciplines().add(newDiscipline);
                    for (final Parameter setParameter : setDiscipline.getParameters()) {
                        final Parameter paramDefinition = discDefinition.getParameterForName(setParameter.getName());
                        if (paramDefinition == null) {
                            LOGGER.warn("Ignoring parameter setting " + setParameter.getName());
                            continue;
                        }
                        if (newDiscipline.getParameterForName(setParameter.getName()) != null) {
                            LOGGER.warn("Ignoring duplicate parameter setting " + setParameter.getName());
                            continue;
                        }
                        final Parameter newParameter = new Parameter(setParameter);
                        newParameter.setName(setParameter.getName());
                        newDiscipline.getParameters().add(newParameter);
                    }
                }

            }
            viewer.refresh();
            viewer.expandAll();
        } catch (final XPathExpressionException | XMLException e) {
            LOGGER.error("Error parsing earlier vampzero configuration", e);
        }
    }

    /**
     * Helper symmetrical to the component and discipline method helpers.
     * 
     * @param comp The list of all loaded components
     * @param name The name to look for
     * @return The component with the given name or null if not found
     */
    private Component getComponentForName(final List<Component> comp, final String name) {
        assert comp != null;
        assert comp.size() > 0;
        assert name != null;
        for (final Component component : comp) {
            if (component.getName().equals(name)) {
                return component;
            }
        }
        return null; // nothing found throws exception, shouldn't ever happen
    }

}
