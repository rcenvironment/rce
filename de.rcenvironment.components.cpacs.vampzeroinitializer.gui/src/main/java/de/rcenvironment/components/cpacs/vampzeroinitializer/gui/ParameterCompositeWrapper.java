/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.TableWrapData;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;

/**
 * Wrapper for Parameter composite.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class ParameterCompositeWrapper {

    private static final int NUMBER_140 = 140;

    private static final int NUMBER60 = 60;

    private static final int NUMBER100 = 100;

    private static final int NUMBER120 = 120;

    private static final int NUMBER150 = 150;

    /**
     * The logger.
     */
    private static final Log LOGGER = LogFactory.getLog(ParameterCompositeWrapper.class);

    private Composite inner;

    private TreeViewer viewer;

    private Discipline discipline;

    private List<Component> modifiedComponents;

    private org.eclipse.swt.widgets.List parameterList;

    private Text nameText;

    private Text descriptionText;

    private Text valueText;

    private Text factorText;

    /**
     * Selects the text field contents when clicked to speed up typing/usability.
     */
    private MouseListener textMarker = new MouseAdapter() {

        @Override
        public void mouseDown(final MouseEvent event) {
            ((Text) event.widget).setSelection(0, ((Text) event.widget).getText().length());
        }
    };

    public ParameterCompositeWrapper(final AbstractSwtHelper factory, final Composite parent, final List<Component> modifiedComponents) {
        inner = factory.createComposite(parent);
        inner.setLayout(factory.createDefaultLayout(2));
        this.modifiedComponents = modifiedComponents;
        parameterList = new org.eclipse.swt.widgets.List(inner, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        
        parameterList.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
        ((TableWrapData) parameterList.getLayoutData()).maxHeight = NUMBER100;
        ((TableWrapData) parameterList.getLayoutData()).maxWidth = NUMBER150;
        ((TableWrapData) parameterList.getLayoutData()).grabHorizontal = true;
        ((TableWrapData) parameterList.getLayoutData()).grabVertical = true;
        final Composite rightComposite = factory.createComposite(inner, 2);
        ((TableWrapData) rightComposite.getLayoutData()).grabHorizontal = true;
        factory.createLabel(rightComposite, "Name:");
        nameText = factory.createText(rightComposite, "");
        ((TableWrapData) nameText.getLayoutData()).maxWidth = NUMBER120;
        factory.createLabel(rightComposite, "Description:");
        descriptionText = factory.createText(rightComposite, "\n");
        ((TableWrapData) descriptionText.getLayoutData()).heightHint = NUMBER60;
        ((TableWrapData) descriptionText.getLayoutData()).grabHorizontal = true;
        ((TableWrapData) descriptionText.getLayoutData()).maxWidth = NUMBER120;
        factory.createLabel(rightComposite, "Value:");
        valueText = factory.createText(rightComposite, "0.0");
        valueText.addMouseListener(textMarker);
        factory.createLabel(rightComposite, "Factor:");
        factorText = factory.createText(rightComposite, "1.0");
        factorText.addMouseListener(textMarker);

        // some layout helper to set minimum width
        Label rightfiller1 = factory.createLabel(rightComposite, "");
        ((TableWrapData) rightfiller1.getLayoutData()).indent = NUMBER120;
        ((TableWrapData) rightfiller1.getLayoutData()).heightHint = 5;
        Label rightfiller2 = factory.createLabel(rightComposite, "");
        ((TableWrapData) rightfiller2.getLayoutData()).indent = NUMBER120;
        ((TableWrapData) rightfiller2.getLayoutData()).heightHint = 5;


        final Button revertButton = factory.createButton(rightComposite, "Revert", new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final int index = parameterList.getSelectionIndex();
                if ((index >= 0) && (index < parameterList.getItemCount())) {
                    final String selectedParameterName = parameterList.getItem(index);
                    final Parameter selectedParameter = discipline.getParameterForName(selectedParameterName);
                    if (selectedParameter != null) {
                        removeModifiedParameter(selectedParameter);
                    } else {
                        LOGGER.error("Parameter not found, but should have.");
                    }
                    viewer.refresh();
                }
            }
        }, TableWrapData.FILL);
        revertButton.setImage(factory.undoImage);
        ((TableWrapData) revertButton.getLayoutData()).grabHorizontal = true;

        final Runnable applyAction = new Runnable() {
            @Override
            public void run() {
                final int index = parameterList.getSelectionIndex();
                if ((index >= 0) && (index < parameterList.getItemCount())) {
                    final String selectedParameterName = parameterList.getItem(index);
                    final Parameter selectedParameter = discipline.getParameterForName(selectedParameterName);
                    if (selectedParameter != null) {
                        addModifiedParameter(selectedParameter, valueText.getText().trim(), factorText.getText().trim()); // add
                                                                                                                          // to
                                                                                                                          // model
                        expandTo(selectedParameter);
                    } else {
                        LOGGER.error("Parameter not found, but should have.");
                    }
                }
            }
        };
        final Button applyButton = factory.createButton(rightComposite, "Apply", new Listener() {

            @Override
            public void handleEvent(final Event event) {
                applyAction.run();
            }
        }, TableWrapData.FILL);
        applyButton.setImage(factory.okImage);
        ((TableWrapData) applyButton.getLayoutData()).grabHorizontal = true;

        final KeyListener enterHit = new KeyAdapter() {

            @Override
            public void keyPressed(final KeyEvent event) {
                if (event.character == SWT.CR) {
                    applyAction.run();
                }
            }
        };
        valueText.addKeyListener(enterHit);
        factorText.addKeyListener(enterHit);

        parameterList.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(final Event event) {
                final int index = parameterList.getSelectionIndex();
                if ((index >= 0) && (index < parameterList.getItemCount())) {
                    final String selection = parameterList.getSelection()[0];
                    final Parameter parameter = discipline.getParameterForName(selection);
                    if (parameter == null) {
                        LOGGER.error("Existing parameter not found. Fatal error!");
                    } else {
                        setName(selection);
                        setDescription(parameter.getDescription());
                        setValue(parameter.getValue());
                        setFactor(parameter.getFactor());
                        redraw();
                    }
                }
            }
        });

        // layout helper to set minimum width
        Label leftfiller = factory.createLabel(inner, "");
        ((TableWrapData) leftfiller.getLayoutData()).indent = NUMBER_140;
        ((TableWrapData) leftfiller.getLayoutData()).heightHint = 0;
        ((TableWrapData) leftfiller.getLayoutData()).maxHeight = 0;
    }

    public void setViewer(final TreeViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * Set Discipline.
     * 
     * @param discipline d
     */
    public void setDiscipline(final Discipline discipline) {
        this.discipline = discipline;
        parameterList.removeAll();
        for (final Parameter parameter : discipline.getParameters()) {
            parameterList.add(parameter.getName());
        }

    }

    /**
     * Set name.
     * 
     * @param text text
     */
    public void setName(final String text) {
        nameText.setText(text);
    }

    /**
     * Set description.
     * 
     * @param description d
     */
    public void setDescription(final String description) {
        descriptionText.setText(description);
        descriptionText.setToolTipText(description.replaceAll("\\s\\s+", "\n"));
    }

    /**
     * Set value.
     * 
     * @param value v
     */
    public void setValue(final String value) {
        valueText.setText(value);
    }

    /**
     * Set Factor.
     * 
     * @param factor f
     */
    public void setFactor(final String factor) {
        factorText.setText(factor);
    }

    /**
     * Remove tha parameter from the list.
     * 
     * @param parameter To remove
     */
    void removeModifiedParameter(final Parameter parameter) {
        final Discipline dis = getOrCreateModifiedDiscipline(parameter.getDiscipline(), false);
        if (dis == null) {
            return;
        }
        final Parameter oldParameter = dis.getParameterForName(parameter.getName());
        if (oldParameter != null) {
            dis.getParameters().remove(oldParameter);
            if (dis.getParameters().size() == 0) {
                final Component oldComponent = dis.getComponent();
                oldComponent.getDisciplines().remove(dis);
                if (oldComponent.getDisciplines().size() == 0) {
                    modifiedComponents.remove(oldComponent);
                }
            }
        }
    }

    /**
     * Create the discipline if necessary and return it.
     * 
     * @param dis discipline
     * @param cr create
     * @return
     */
    private Discipline getOrCreateModifiedDiscipline(final Discipline dis, final boolean cr) {
        final Component component = getOrCreateModifiedComponent(dis.getComponent(), cr);
        Discipline result = null;
        if (component != null) {
            final Discipline d = component.getDisciplineForName(dis.getName());
            if (d != null) {
                result = d; // found already existing discipline
            } else {
                if (!cr) {
                    return null;
                }
                final Discipline newDiscipline = (Discipline) new Discipline().setName(dis.getName());
                newDiscipline.setComponent(component);
                component.getDisciplines().add(newDiscipline);
                return newDiscipline;
            }
            
        }
        
        return result;
        
    }

    /**
     * Create component if necessary and return it.
     * 
     * @param component
     * @param create
     * @return
     */
    private Component getOrCreateModifiedComponent(final Component component, final boolean create) {
        for (final Component c : modifiedComponents) {
            if (c.getName().equals(component.getName())) {
                return c; // found already existing component
            }
        }
        if (!create) {
            return null;
        }
        final Component newComponent = (Component) new Component().setName(component.getName());
        modifiedComponents.add(newComponent);
        return newComponent;
    }

    /**
     * Create a copy of the modified parameter and add its parents if necessary.
     * 
     * @param parameter The parameter to modify
     * @param value The new value
     * @param factor The new factor
     */
    void addModifiedParameter(final Parameter parameter, final String value, final String factor) {
        final Discipline dis = getOrCreateModifiedDiscipline(parameter.getDiscipline(), true);
        final Parameter p = dis.getParameterForName(parameter.getName());
        if (p != null) {
            p.setValue(value).setFactor(factor);
            return;
        }
        final Parameter newParameter = ((Parameter) new Parameter()
            .setName(parameter.getName()))
            .setDescription(parameter.getDescription())
            .setValue(value).setFactor(factor);
        newParameter.setDiscipline(dis);
        dis.getParameters().add(newParameter);
    }

    /**
     * Expand shown tree to the given domain parameter.
     * 
     * @param parameter The paramter to show
     */
    private void expandTo(final Parameter parameter) {
        final TreeItem root = viewer.getTree().getItem(0);
        revealNode(root);
        viewer.expandAll();
        final String componentName = parameter.getDiscipline().getComponent().getName();
        for (int i = 0, n = root.getItemCount(); i < n; i++) {
            final TreeItem item = root.getItem(i);
            if (item.getText().equals(componentName)) {
                revealNode(item);
                final String disciplineName = parameter.getDiscipline().getName();
                for (int j = 0, o = item.getItemCount(); j < o; j++) {
                    final TreeItem item2 = item.getItem(j);
                    if (item2.getText().equals(disciplineName)) {
                        revealNode(item2);
                        final String parameterName = parameter.getName();
                        for (int k = 0, p = item2.getItemCount(); k < p; k++) {
                            final TreeItem item3 = item2.getItem(k);
                            if (item3.getText().equals(parameterName)) {
                                revealNode(item3);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper for SWT stuff.
     * 
     * @param node The node to show
     */
    private void revealNode(final TreeItem node) {
        assert node != null;
        node.setExpanded(true);
        viewer.getTree().update();
        viewer.getTree().showItem(node);
        viewer.refresh(true);
    }

    void redraw() {
        inner.pack(true);
        inner.getParent().getParent().redraw();
    }

}
