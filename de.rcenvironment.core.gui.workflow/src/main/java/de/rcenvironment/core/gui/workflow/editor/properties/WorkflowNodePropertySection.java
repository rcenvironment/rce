/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.commands.CompositeCommand;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Abstract base class for implementing a property editor for a workflow node.
 * 
 * To implement a new <code>WorkflowNodePropertySection}</code>:
 * <ol>
 * <li>Derive a new section from this class.</li>
 * <li>Implement the {@link #createCompositeContents(Composite, TabbedPropertySheetPage)} method to create the GUI. Remember to use SWT
 * Forms and appropriate styles to meet the GUI standards. Tag {@link Control}s which are displaying certain configuration properties with
 * the {@link Control#setData(String, Object)} method, using {@link #CONTROL_PROPERTY_KEY} as key and the property key as value.</li>
 * <li>Create a controller class implementing {@link Controller} or deriving from {@link DefaultController}. Implement the controller
 * functionality which should be reacting on certain GUI-events in the appropriate {@link Controller} function. To integrate changes in the
 * Undo/Redo stack, implement any changes as instances of {@link WorkflowNodeCommand} and hand them to the
 * {@link #execute(WorkflowNodeCommand)} method. Within {@link WorkflowNodeCommand}s the {@link WorkflowNode} can be retrieved via
 * {@link WorkflowNodeCommand#getWorkflowNode()}. The methods {@link #setProperty(String, Serializable)},
 * {@link #setProperty(String, Serializable, Serializable)} and {@link #editProperty(String)} are convenience functions that integrate into
 * the Undo/Redo stack. Overwrite the {@link #createController()} method to return an instance of your {@link Controller} implementation.
 * </li>
 * <li>As changes to the model can be undone via the {@link CommandStack}, the GUI needs to be synchronized with changes to the underlying
 * model. This has to be realized in a custom {@link Synchronizer}, which should ideally be derived from {@link DefaultSynchronizer}.
 * Overwrite the {@link #createSynchronizer()} method to return an instance of your {@link Synchronizer} implementation.</li>
 * <li>All updates of the GUI, which reflect change events in the model, should happen through a {@link Updater} instance. If functionality
 * above those of the {@link DefaultUpdater} is required this class has to be derived and extended. Overwrite the {@link #createUpdater()}
 * method to return an instance of your {@link Updater} implementation.</li>
 * </ol>
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Doreen Seider
 * @author Markus Kunde
 * @author Kathrin Schaffert (added setProperties(...))
 */
public abstract class WorkflowNodePropertySection extends WorkflowPropertySection implements WorkflowNodeCommand.Executor {

    /** The key of {@link Control} data fields identifying the managed workflow node property. */
    public static final String CONTROL_PROPERTY_KEY = "property.control";

    /** The key of {@link Button} data fields identifying the associated enum type. */
    public static final String ENUM_TYPE_KEY = "property.enum.type";

    /** The key of {@link Button} data fields identifying the associated enum value. */
    public static final String ENUM_VALUE_KEY = "property.enum.value";

    protected final Log logger = LogFactory.getLog(this.getClass());

    protected WorkflowNode node;

    private final Map<String, Map<Enum<?>, Set<Control>>> enumGroups = new HashMap<String, Map<Enum<?>, Set<Control>>>();

    private ComponentInstanceProperties modelBindingTarget;

    private ComponentInstanceProperties lastRefreshConfiguration;

    private EditConfigurationValueCommand openEditCommand;

    private Composite composite;

    private final Controller controller = createController();

    private final Synchronizer synchronizer = createSynchronizer();

    private final SynchronizerAdapter synchronizerAdapter = new SynchronizerAdapter();

    private final Updater updater = createUpdater();

    @Override
    public void setInput(final IWorkbenchPart part, final ISelection selection) {
        final Object firstSelectionElement = ((IStructuredSelection) selection).getFirstElement();
        final WorkflowNodePart workflowNodePart = (WorkflowNodePart) firstSelectionElement;
        final WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
        if (getPart() == null || !getPart().equals(part)
            || node == null || !node.equals(workflowNode)) {
            super.setInput(part, selection);
            setWorkflowNodeBase(workflowNode);
        }
    }

    protected Composite getComposite() {
        return composite;
    }

    private void setWorkflowNodeBase(final WorkflowNode workflowNode) {
        tearDownModelBindingBase();
        this.node = workflowNode;
        initializeModelBindingBase();
        setWorkflowNode(workflowNode);
    }

    /**
     * Invoked, after a new input {@link WorkflowNode} has been set and the model binding is initialized.
     * 
     * @param workflowNode the new input {@link WorkflowNode}
     */
    protected void setWorkflowNode(final WorkflowNode workflowNode) {
        /* empty default implementation */
    }

    private void initializeModelBindingBase() {
        if (modelBindingTarget == null) {
            modelBindingTarget = getConfiguration();
            modelBindingTarget.addPropertyChangeListener(synchronizerAdapter);
            afterInitializingModelBinding();
        }
    }

    /**
     * Invoked after the model binding for the base {@link WorkflowNodePropertySection} has been initialized.
     * 
     */
    protected void afterInitializingModelBinding() {
        /* empty default implementation */
    }

    @Override
    public void dispose() {
        tearDownModelBindingBase();
        super.dispose();
    }

    private void tearDownModelBindingBase() {
        if (modelBindingTarget != null) {
            RuntimeException exception = null;
            try {
                beforeTearingDownModelBinding();
            } catch (RuntimeException e) {
                exception = e;
            }
            modelBindingTarget.removePropertyChangeListener(synchronizerAdapter);
            modelBindingTarget = null;
            if (exception != null) {
                throw new RuntimeException("Tearing down model binding failed in derived class:", exception);
            }
        }
    }

    /**
     * Invoked before the model binding for the base {@link WorkflowNodePropertySection} is teared down.
     * 
     */
    protected void beforeTearingDownModelBinding() {
        /* empty default implementation */
    }

    @Override
    /*
     * #createCompositeContent(Composite, TabbedPropertySheet) should be used to benefit from Controller, Synchronizer and Updater.
     */
    @Deprecated
    // see comment above
    public void createControls(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createControls(parent, aTabbedPropertySheetPage);
        composite = parent;
        createCompositeContent(composite, aTabbedPropertySheetPage);
        initializeController();
        initializeEnumGroups();
    }

    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        /* empty default implementation */
    }

    protected final Controller getController() {
        return controller;
    }

    protected Controller createController() {
        return new DefaultController();
    }

    protected void initializeController() {
        final Controller controller2 = getController();
        if (composite != null && controller2 != null) {
            initializeController(controller2, composite);
        }
    }

    protected void initializeController(final Controller controller2, final Composite parent) {
        for (final Control control : parent.getChildren()) {
            final String property = (String) control.getData(CONTROL_PROPERTY_KEY);
            if (control instanceof CTabFolder) {
                CTabFolder tabFolder = (CTabFolder) control;
                for (CTabItem item : tabFolder.getItems()) {
                    if (item.getControl() != null) {
                        if (item.getControl() instanceof Composite) {
                            initializeController(controller2, (Composite) item.getControl());
                        }
                    }
                }
            } else if (control instanceof Composite) {
                initializeController(controller2, (Composite) control);
            }
            final boolean activeControl = control instanceof Button;
            if (activeControl || property != null) {
                control.addFocusListener(controller2);
                control.addKeyListener(controller2);
                if (control instanceof Button) {
                    ((Button) control).addSelectionListener(controller2);
                } else if (control instanceof Text) {
                    ((Text) control).addSelectionListener(controller2);
                    ((Text) control).addModifyListener(controller2);
                } else if (control instanceof StyledText) {
                    ((StyledText) control).addSelectionListener(controller2);
                    ((StyledText) control).addModifyListener(controller2);
                } else if (control instanceof Combo) {
                    ((Combo) control).addSelectionListener(controller2);
                } else if (control instanceof CCombo) {
                    ((CCombo) control).addSelectionListener(controller2);
                    ((CCombo) control).addModifyListener(controller2);
                } else if (control instanceof Spinner) {
                    ((Spinner) control).addSelectionListener(controller2);
                    ((Spinner) control).addModifyListener(controller2);
                }
            }
        }
    }

    protected void initializeEnumGroups() {
        if (composite != null) {
            initializeEnumGroups(composite);
        }
    }

    protected void initializeEnumGroups(final Composite composite2) {
        for (final Control control : composite2.getChildren()) {
            if (control instanceof Composite) {
                initializeEnumGroups((Composite) control);
            }
            final String property = (String) control.getData(CONTROL_PROPERTY_KEY);
            @SuppressWarnings("unchecked") final Class<? extends Enum<?>> enumType =
                (Class<? extends Enum<?>>) control.getData(ENUM_TYPE_KEY);
            final Enum<?> enumValue = (Enum<?>) control.getData(ENUM_VALUE_KEY);
            if (property != null && enumType != null && enumValue != null) {
                if (!enumType.isAssignableFrom(enumValue.getClass())) {
                    throw new RuntimeException();
                }
                if (!enumGroups.containsKey(property)) {
                    enumGroups.put(property, new HashMap<Enum<?>, Set<Control>>());
                }
                final Map<Enum<?>, Set<Control>> enumGroup = enumGroups.get(property);
                if (!enumGroup.containsKey(enumValue)) {
                    enumGroup.put(enumValue, new HashSet<Control>());
                }
                enumGroup.get(enumValue).add(control);
            }
        }
    }

    protected boolean isEnumControl(final Control control) {
        final String property = (String) control.getData(CONTROL_PROPERTY_KEY);
        @SuppressWarnings("unchecked") final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) control.getData(ENUM_TYPE_KEY);
        final Enum<?> enumValue = (Enum<?>) control.getData(ENUM_VALUE_KEY);
        boolean result = control instanceof Button && (control.getStyle() & SWT.RADIO) != 0;
        result &= property != null && enumType != null && enumValue != null;
        return result;
    }

    protected final Synchronizer getSynchronizer() {
        return synchronizer;
    }

    protected Synchronizer createSynchronizer() {
        return new DefaultSynchronizer();
    }

    protected Updater getUpdater() {
        return updater;
    }

    protected Updater createUpdater() {
        return new DefaultUpdater();
    }

    protected void addPropertyChangeListener(PropertyChangeListener listener) {
        node.addPropertyChangeListener(listener);
    }

    protected void removePropertyChangeListener(PropertyChangeListener listener) {
        node.removePropertyChangeListener(listener);
    }

    protected ComponentInstanceProperties getReadableConfiguration() {
        return node;
    }

    /**
     * Returns the readable configuration.
     * 
     * @return {@link ComponentInstanceProperties}
     */
    public ComponentInstanceProperties getConfiguration() {
        if (getCommandStack() == null || node == null) {
            throw new IllegalStateException("Property input not set");
        }
        return node;
    }

    protected boolean hasInputs() {
        return WorkflowNodeUtil.hasInputs(node);
    }

    protected boolean hasOutputs() {
        return WorkflowNodeUtil.hasOutputs(node);
    }

    protected boolean hasInputs(DataType type) {
        return WorkflowNodeUtil.hasInputs(node, type);
    }

    protected boolean hasOutputs(DataType type) {
        return WorkflowNodeUtil.hasOutputs(node, type);
    }

    protected Set<EndpointDescription> getInputs() {
        return WorkflowNodeUtil.getInputs(node);
    }

    protected Set<EndpointDescription> getInputs(DataType type) {
        return WorkflowNodeUtil.getInputsByDataType(node, type);
    }

    protected Set<EndpointDescription> getOutputs() {
        return WorkflowNodeUtil.getOutputs(node);
    }

    protected Set<EndpointDescription> getOutputs(DataType type) {
        return WorkflowNodeUtil.getOutputs(node, type);
    }

    protected boolean isPropertySet(final String key) {
        return WorkflowNodeUtil.isConfigurationValueSet(node, key);
    }

    protected String getProperty(final String key) {
        if (node != null) {
            String s = WorkflowNodeUtil.getConfigurationValue(node, key);
            return s;
        } else {
            return null;
        }
    }

    /**
     * 
     * Use this method to set a configuration value, that can be undone via undo mechanism.
     * 
     * @param key configuration key
     * @param value new configuration value
     * 
     * @see #setProperty(String, String, String)
     * 
     */
    protected void setProperty(final String key, final String value) {
        final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, key);
        setProperty(key, oldValue, value);
    }

    /**
     * 
     * Use this method to set a configuration value, that can be undone via undo mechanism.
     * 
     * @param key configuration key
     * @param oldValue current configuration value
     * @param newValue new configuration value
     */
    protected void setProperty(final String key, final String oldValue, final String newValue) {
        if ((oldValue != null && !oldValue.equals(newValue))
            || (oldValue == null && oldValue != newValue)) {
            final WorkflowNodeCommand command = new SetConfigurationValueCommand(key, oldValue, newValue);
            execute(command);
        }
    }

    /**
     * 
     * Use this method to set a configuration value, that cannot be undone via undo mechanism.
     * 
     * @param key configuration key
     * @param value new configuration value
     * 
     * @see #setPropertyNotUndoable(String, String, String)
     */
    protected void setPropertyNotUndoable(final String key, final String value) {
        final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, key);
        setPropertyNotUndoable(key, oldValue, value);
    }

    /**
     * 
     * Use this method to set a configuration value, that cannot be undone via undo mechanism.
     * 
     * @param key configuration key
     * @param oldValue current configuration value
     * @param newValue new configuration value
     */
    protected void setPropertyNotUndoable(final String key, final String oldValue, final String newValue) {
        if ((oldValue != null && !oldValue.equals(newValue))
            || (oldValue == null && oldValue != newValue)) {
            final WorkflowNodeCommand command = new SetConfigurationValueCommand(key, oldValue, newValue);
            command.setCommandStack(getCommandStack());
            command.setWorkflowNode(node);
            command.initialize();
            command.execute();
        }
    }

    /**
     * Use this method to set more than one configuration value at the same time.
     */
    protected void setProperties(final String key1, final String newValue1, final String... keysAndValues) {

        if (keysAndValues.length % 2 == 1) {
            throw new InvalidParameterException("Method setProperties must be called with an even number of arguments.");
        }

        WorkflowCommand currentCommand = reduceKeyValueListToWorkflowCommand(key1, newValue1, keysAndValues);

        if (currentCommand != null) {
            currentCommand.setCommandStack(getCommandStack());
            currentCommand.initialize();
            if (currentCommand.canExecute()) {
                getCommandStack().execute(new CommandWrapper(currentCommand));
            }
        }
    }

    private WorkflowCommand reduceKeyValueListToWorkflowCommand(final String key1, final String newValue1, final String... keysAndValues) {
        WorkflowCommand currentCommand = null;

        final String oldValue1 = WorkflowNodeUtil.getConfigurationValue(node, key1);

        final WorkflowCommand initialCommand = buildSetConfigurationValueCommand(key1, oldValue1, newValue1);
        if (initialCommand != null) {
            currentCommand = initialCommand;
        }
        for (int i = 0; i < keysAndValues.length; i += 2) {
            final String key = keysAndValues[i];
            final String newValue = keysAndValues[i + 1];
            final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, key);

            final WorkflowCommand newCommand = buildSetConfigurationValueCommand(key, oldValue, newValue);
            if (currentCommand != null && newCommand != null) {
                currentCommand = new CompositeCommand(currentCommand, newCommand);
            } else if (currentCommand == null && newCommand != null) {
                currentCommand = newCommand;
            }
        }
        return currentCommand;
    }

    private WorkflowCommand buildSetConfigurationValueCommand(String key, String oldValue, String newValue) {
        if ((newValue != null && !newValue.equals(oldValue)) || (newValue == null && oldValue != null)) {
            final SetConfigurationValueCommand newCommand = new SetConfigurationValueCommand(key, oldValue, newValue);
            newCommand.setWorkflowNode(node);
            return newCommand;
        } else {
            return null;
        }
    }

    protected EditConfigurationValueCommand editProperty(final String key) {
        final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, key);
        final EditConfigurationValueCommand command = new EditConfigurationValueCommand(key, oldValue);
        execute(command);
        return command;
    }

    /**
     * If the current node is not the node where the command should be executed, this method must be called.
     * 
     * @param workflowNode to execute the command from.
     * @param command to execute.
     */
    public void execute(WorkflowNode workflowNode, WorkflowNodeCommand command) {
        if (openEditCommand != null) {
            openEditCommand.finishEditing();
            openEditCommand = null;
        }
        command.setCommandStack(getCommandStack());
        command.setWorkflowNode(workflowNode);
        command.initialize();
        if (command.canExecute()) {
            getCommandStack().execute(new NodeCommandWrapper(command));
            if (command instanceof EditConfigurationValueCommand) {
                openEditCommand = (EditConfigurationValueCommand) command;
            }
        }
    }

    @Override
    public void execute(final WorkflowNodeCommand command) {
        execute(node, command);
    }

    @Override
    public final void refresh() {
        /*
         * Caching the configuration the refresh was executed for the last time, avoids executing the refresh twice.
         */
        if (lastRefreshConfiguration == null || lastRefreshConfiguration != getConfiguration()) {
            refreshSection();
            lastRefreshConfiguration = getConfiguration();
        }
    }

    protected void refreshSection() {
        if (composite != null) {
            refreshComposite(composite);
        }
    }

    protected void refreshComposite(final Composite composite2) {
        if (composite2.isDisposed()) {
            return;
        }
        for (final Control control : composite2.getChildren()) {
            if (control.isDisposed()) {
                continue;
            }
            if (control instanceof Composite) {
                refreshComposite((Composite) control);
            }
            final String propertyKey = (String) control.getData(CONTROL_PROPERTY_KEY);
            if (propertyKey == null) {
                continue;
            }
            final String propertyValue = getProperty(propertyKey);
            getUpdater().initializeControl(control, propertyKey, propertyValue);
        }
    }

    private static boolean isBooleanButton(final Control button) {
        return button instanceof Button && (button.getStyle() & SWT.CHECK) != 0 || (button.getStyle() & SWT.TOGGLE) != 0
            || (button.getStyle() & SWT.RADIO) != 0;
    }

    /**
     * A wrapper class to wrap {@link WorkflowNodeCommand}s in GEF {@link Command}s.
     * 
     * @author Christian Weiss
     */
    private static final class NodeCommandWrapper extends WorkflowPropertySection.CommandWrapper {

        /** The backing command, invokations are forwarded to. */
        private final WorkflowNodeCommand command;

        private NodeCommandWrapper(final WorkflowNodeCommand command) {
            super(command);
            this.command = command;
        }

        @Override
        public String getLabel() {
            return command.getLabel();
        }

    }

    /**
     * {@link WorkflowNodeCommand} to change the value of a property in the backing <code>ComponentInstanceConfiguration</code>.
     * 
     * @author Christian Weiss
     */
    protected static class SetConfigurationValueCommand extends AbstractWorkflowNodeCommand {

        protected final String key;

        protected String oldValue;

        protected String newValue;

        public SetConfigurationValueCommand(final String key, final String oldValue, final String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void execute2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(key, newValue);
        }

        @Override
        public void undo2() {
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(key, oldValue);
        }
    }

    /**
     * {@link WorkflowNodeCommand} to change the value of a property in the backing <code>ComponentInstanceConfiguration</code> through
     * editing.
     * 
     * @author Christian Weiss
     */
    protected static final class EditConfigurationValueCommand extends AbstractWorkflowNodeCommand {

        private final String key;

        private final String oldValue;

        private String newValue;

        private boolean editable = true;

        private EditConfigurationValueCommand(final String key, final String oldValue) {
            this(key, oldValue, oldValue);
        }

        private EditConfigurationValueCommand(final String key, final String oldValue, final String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public boolean isEditable() {
            return editable;
        }

        public void finishEditing() {
            if (editable) {
                editable = false;
                ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
                configDesc.setConfigurationValue(key, newValue);
            }
        }

        public String getKey() {
            return key;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(final String newValue) {
            if (!editable) {
                throw new IllegalStateException();
            }
            this.newValue = newValue;
        }

        @Override
        public void execute2() {
            if (!editable) {
                ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
                // execute methods needs to set new value to be able to restore changes upon redo
                configDesc.setConfigurationValue(key, newValue);

            }
        }

        @Override
        public void undo2() {
            // perform the changes first
            if (editable) {
                finishEditing();
            }
            ConfigurationDescription configDesc = getProperties().getConfigurationDescription();
            configDesc.setConfigurationValue(key, oldValue);
        }

    }

    /**
     * Controller interface. Needs to be implemented by controllers which want to use the
     * {@link WorkflowNodePropertySection#initializeController(Controller, Composite)} method to link the controller to {@link Control}
     * components which are tagged with the {@link WorkflowNodePropertySection#CONTROL_PROPERTY_KEY}, indicating that they are displaying
     * certain properties.
     * 
     * <p>
     * <b>Remember</b>, to only commit changes to the model in the {@link Controller} and not changes to the GUI. The reactions to changes
     * in the model must be implemented in a {@link Synchronizer}.
     * </p>
     * 
     * @author Christian Weiss
     */
    protected interface Controller extends SelectionListener, FocusListener, KeyListener, ModifyListener {

    }

    /**
     * Default implementation of a {@link Controller}.
     * 
     * Implements some core functionality needed by controller components for {@link WorkflowNodePropertySection}s.
     * 
     * @author Christian Weiss
     */
    protected class DefaultController implements Controller {

        protected EditConfigurationValueCommand editCommand;

        @Override
        public void widgetDefaultSelected(final SelectionEvent event) {}

        @Override
        public void widgetSelected(final SelectionEvent event) {
            final Object source = event.getSource();
            if (source instanceof Control) {
                final Control control = (Control) source;
                widgetSelected(event, control);
                final String property = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (property != null) {
                    widgetSelected(event, control, property);
                }
                final Enum<?> enumValue = (Enum<?>) control.getData(ENUM_VALUE_KEY);
                if (enumValue != null) {
                    widgetSelected(event, control, property, enumValue);
                }
            }
        }

        protected void widgetSelected(final SelectionEvent event, final Control source) {
            /* empty default implementation */
        }

        protected void widgetSelected(final SelectionEvent event, final Control source, final String property) {
            if (source instanceof Button) {
                final Button button = (Button) source;
                if (isBooleanButton(button)) {
                    final boolean selected = button.getSelection();
                    setProperty(property, String.valueOf(selected));
                }
            } else if (source instanceof Spinner) {
                final Spinner spinner = (Spinner) source;
                final Integer spinnerValue = spinner.getSelection();
                if (getProperty(property) != null) {
                    final Integer propertyValue = Integer.valueOf(getProperty(property));
                    if (spinnerValue != null && !spinnerValue.equals(propertyValue)) {
                        setProperty(property, String.valueOf(spinnerValue));
                    }
                } else if (spinnerValue != null) {
                    setProperty(property, String.valueOf(spinnerValue));
                }
            }
        }

        protected void widgetSelected(final SelectionEvent event, final Control source, final String property, final Object value) {
            if (value instanceof Enum) {
                final Enum<?> enumValue = (Enum<?>) value;
                setProperty(property, enumValue.name());
            }
        }

        @Override
        public void focusGained(final FocusEvent event) {}

        /**
         * 
         * {@inheritDoc}
         * 
         * Functionality:
         * <ul>
         * <li>Finishes an open 'edit session' for a property encapsulated in a {@link EditConfigurationValueCommand}.</li>
         * </ul>
         * 
         * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
         */
        @Override
        public void focusLost(final FocusEvent event) {
            if (editCommand != null) {
                editCommand.finishEditing();
                editCommand = null;
            }
        }

        /**
         * {@inheritDoc}
         * 
         * Functionality:
         * <ul>
         * <li>Editing in a {@link Text} control starts or continues an open 'edit session' for a property encapsulated in a
         * {@link EditConfigurationValueCommand}.</li>
         * </ul>
         * 
         * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
         */
        @Override
        public void modifyText(final ModifyEvent event) {
            final Object source = event.getSource();
            if (source instanceof Control) {
                final Control control = (Control) source;
                final String property = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (source instanceof Text && property != null) {
                    final Text text = (Text) source;
                    final String textContent = text.getText();
                    final String propertyContent = getProperty(property);
                    if (!textContent.equals(propertyContent)) {
                        if (editCommand == null || !editCommand.isEditable()) {
                            editCommand = editProperty(property);
                        }
                        editCommand.setNewValue(textContent);
                    }
                } else if (source instanceof StyledText
                    && property != null) {
                    final StyledText text = (StyledText) source;
                    final String textContent = text.getText();
                    final String propertyContent = getProperty(property);
                    if (!textContent.equals(propertyContent)) {
                        if (editCommand == null || !editCommand.isEditable()) {
                            editCommand = editProperty(property);
                        }
                        editCommand.setNewValue(textContent);
                    }
                } else if (source instanceof CCombo
                    && property != null) {
                    final CCombo text = (CCombo) source;
                    final String textContent = text.getText();
                    final String propertyContent = getProperty(property);
                    if (!textContent.equals(propertyContent)) {
                        if (editCommand == null || !editCommand.isEditable()) {
                            editCommand = editProperty(property);
                        }
                        editCommand.setNewValue(textContent);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * Functionality:
         * <ul>
         * <li>Pressing the ENTER key in a non-SWT.MULTI {@link Text} control forces a traversal.</li>
         * </ul>
         * 
         * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
         */
        @Override
        public void keyPressed(final KeyEvent event) {
            final Object source = event.getSource();
            // TAB on enter in single line text fields
            if (source instanceof Text) {
                final Text text = (Text) source;
                if (isCarriageReturn(event)
                    && !isMultiLineText(text)) {
                    text.traverse(SWT.TRAVERSE_TAB_NEXT);
                }
            } else if (source instanceof StyledText) {
                final StyledText text = (StyledText) source;
                if (isCarriageReturn(event)
                    && !isMultiLineText(text)) {
                    text.traverse(SWT.TRAVERSE_TAB_NEXT);
                }
            } else if (source instanceof CCombo) {
                final CCombo text = (CCombo) source;
                if (isCarriageReturn(event)) {
                    text.traverse(SWT.TRAVERSE_TAB_NEXT);
                }
            }
        }

        protected boolean isCarriageReturn(final KeyEvent event) {
            return event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR;
        }

        protected boolean isMultiLineText(final Text text) {
            return (text.getStyle() & SWT.MULTI) != 0;
        }

        protected boolean isMultiLineText(final StyledText text) {
            return (text.getStyle() & SWT.MULTI) != 0;
        }

        @Override
        public void keyReleased(final KeyEvent event) {}

        /**
         * Replaces the current selection or cursor position in a {@link Text} control with the specified replacement <code>String</code>.
         * 
         * @param text the {@link Text} control with selected text or cursor position
         * @param replacement the replacement <code>String</code>
         */
        protected void replace(final Text text, final String replacement) {
            // default method of Text does not reset the selection or focus the control again
            // text.insert(replacement);
            final String textValue = text.getText();
            final Point selection = text.getSelection();
            // replace selected text part with replacement string
            final String newValue = textValue.substring(0, selection.x) + replacement + textValue.substring(selection.y);
            final String property = (String) text.getData(CONTROL_PROPERTY_KEY);
            if (!newValue.equals(textValue)) {
                if (property != null) {
                    if (editCommand == null) {
                        editCommand = editProperty(property);
                    }
                    editCommand.setNewValue(newValue);
                }
                text.setText(newValue);
                final int newX;
                final int newY;
                if (selection.x != selection.y) {
                    newX = selection.x;
                    newY = selection.x + replacement.length();
                } else {
                    newX = selection.x + replacement.length();
                    newY = newX;
                }
                text.setSelection(newX, newY);
            }
        }

        protected String getProperty(final String key) {
            /*
             * If the edit command is open and uncommitted return the value in the editor.
             */
            if (editCommand != null && editCommand.isEditable() && key.equals(editCommand.getKey())) {
                return editCommand.getNewValue();
            }
            return WorkflowNodePropertySection.this.getProperty(key);
        }

        protected EditConfigurationValueCommand editProperty(final String key) {
            return WorkflowNodePropertySection.this.editProperty(key);
        }

    }

    /**
     * Adapter to listen to events in the backing model and translate it to events in the {@link Synchronizer}.
     * 
     * @author Christian Weiss
     */
    private final class SynchronizerAdapter implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final String propertyNameValue = event.getPropertyName();
            final Matcher propertiesPatternMatcher = WorkflowNode.PROPERTIES_PATTERN.matcher(propertyNameValue);
            if (propertiesPatternMatcher.matches()) {
                final String propertyName = propertiesPatternMatcher.group(1);
                synchronizer.handlePropertyChange(propertyName, (String) event.getNewValue(), (String) event.getOldValue());
            }
        }
    }

    /**
     * Listener class responsible for keeping the GUI in sync with the model.
     * 
     * <p>
     * The <code>Synchronizer</code> gets registered at the model to listen to change events (properties & channels) and executes
     * appropriate actions to reflect those changes in the GUI.
     * </p>
     * 
     * <p>
     * The integration of a <code>Synchronizer</code> is as follows:
     * <ul>
     * <li>A {@link SynchronizerAdapter} gets registered to the backing model (a {@link ReadableComponentInstanceConfiguration}. This
     * adapter filters events and converts the non-filtered to invocations of the {@link Synchronizer} instance created via
     * {@link WorkflowNodePropertySection#createSynchronizer()}.</li>
     * <li>The {@link Synchronizer} receives those filtered events via its custom interface and reacts through updating the GUI
     * appropriately. The default implementation {@link DefaultSynchronizer} forwards updates to the {@link Updater} instance created via
     * {@link WorkflowNodePropertySection#createUpdater()}.</li>
     * </ul>
     * </p>
     * 
     * @author Christian Weiss
     */
    public interface Synchronizer {

        /**
         * React on the change of a property.
         * 
         * @param propertyName the key of the property
         * @param newValue the new value of the property
         * @param oldValue the old value of the property
         */
        void handlePropertyChange(final String propertyName, final String newValue, final String oldValue);

    }

    /**
     * Default implementation of a {@link Synchronizer}, forwarding all updates to the {@link Updater}.
     * 
     * <p>
     * It is adviced to derive from this class and call the super class implementation as the very first thing in overwritten methods.
     * </p>
     * 
     * @author Christian Weiss
     */
    protected class DefaultSynchronizer implements Synchronizer {

        @Override
        public void handlePropertyChange(final String propertyName, final String newValue, final String oldValue) {
            final Composite compositeInst = getComposite();
            if (compositeInst != null) {
                recursePropertyChange(compositeInst, propertyName, newValue, oldValue);
            }
        }

        protected void recursePropertyChange(final Composite compositeInst, final String key, final String newValue,
            final String oldValue) {
            for (final Control control : compositeInst.getChildren()) {
                if (control instanceof Composite) {
                    recursePropertyChange((Composite) control, key, newValue, oldValue);
                }
                final String linkedKey = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (linkedKey != null && linkedKey.equals(key)) {
                    handlePropertyChange(control, key, newValue, oldValue);
                }
            }
        }

        protected void handlePropertyChange(final Control control, final String key, final String newValue,
            final String oldValue) {
            getUpdater().updateControl(control, key, newValue, oldValue);
        }
    }

    /**
     * Interface for handlers updating the UI.
     * 
     * @author Christian Weiss
     */
    protected interface Updater {

        /**
         * Initializes the {@link Control} which is linked to the property.
         * 
         * @param control the linked {@link Control}
         * @param propertyName the name of the property
         * @param value the value to display
         */
        void initializeControl(final Control control, final String propertyName, final String value);

        /**
         * Updates the {@link Control} which is linked to the property.
         * 
         * @param control the linked {@link Control}
         * @param propertyName the name of the property
         * @param newValue the value to display
         * @param oldValue the old value which should not be displayed anymore
         */
        void updateControl(final Control control, final String propertyName, final String newValue, final String oldValue);

    }

    /**
     * Default {@link Updater} implementation of the handler to update the UI.
     * 
     * @author Christian Weiss
     */
    protected class DefaultUpdater implements Updater {

        /**
         * {@inheritDoc}
         * 
         * <p>
         * The default implementation delegates to {@link #updateControl(Control, String, Serializable, Serializable)} with 'null' as
         * oldValue.
         * </p>
         * 
         * @see de.rcenvironment.core.gui.workflow.editor.
         *      properties.WorkflowNodePropertySection.Updater#initializeControl(org.eclipse.swt.widgets.Control, java.lang.String,
         *      java.io.Serializable)
         */
        @Override
        public void initializeControl(final Control control, final String propertyName, final String value) {
            updateControl(control, propertyName, value, null);
        }

        @Override
        public void updateControl(final Control control, final String propertyName, final String newValue,
            final String oldValue) {
            /*
             * Text inputs are only set, if the value is a String - otherwise a formatter should be used in a custom Updater.
             */
            if (control instanceof Text && (newValue == null || newValue instanceof String)) {
                final Text textControl = (Text) control;
                final String valueOrDefault = valueOrDefault(newValue, "");
                if (!valueOrDefault.equals(textControl.getText())) {
                    textControl.setText(valueOrDefault);
                }
                /*
                 * Text inputs are only set, if the value is a String - otherwise a formatter should be used in a custom Updater.
                 */
            } else if (control instanceof StyledText && (newValue == null || newValue instanceof String)) {
                final StyledText textControl = (StyledText) control;
                final String valueOrDefault = valueOrDefault(newValue, "");
                if (!valueOrDefault.equals(textControl.getText())) {
                    textControl.setText(valueOrDefault);
                }
                /*
                 * Label outputs are set to the String value of the value.
                 */
            } else if (control instanceof Label) {
                final Label labelControl = (Label) control;
                final String valueOrDefault = stringValue(control, newValue, "");
                if (!valueOrDefault.equals(labelControl.getText())) {
                    labelControl.setText(valueOrDefault);
                }
                /*
                 * Button inputs which are are of style CHECK or TOGGLE are set to the selection-state, if the value is of type Boolean.
                 */
            } else if (control instanceof Button && isBooleanButton(control)) {
                final Button buttonControl = (Button) control;
                final String valueOrDefault = valueOrDefault(newValue, Boolean.FALSE.toString());
                if (!valueOrDefault.equals(buttonControl.getSelection())) {
                    buttonControl.setSelection(Boolean.valueOf(valueOrDefault));
                }
            } else if (control instanceof Spinner) {
                final Spinner spinnerControl = (Spinner) control;
                final String valueOrDefault = stringValue(control, newValue, "0");
                if (!valueOrDefault.equals(spinnerControl.getSelection())) {
                    spinnerControl.setSelection(Integer.valueOf(valueOrDefault));
                }
            } else if (control instanceof Button && isEnumControl(control)) {
                @SuppressWarnings("unchecked") final Class<? extends Enum<?>> enumType =
                    (Class<? extends Enum<?>>) control.getData(ENUM_TYPE_KEY);
                final Enum<?> enumValue = (Enum<?>) control.getData(ENUM_VALUE_KEY);
                final Enum<?> newEnumValue;
                newEnumValue = getEnum(enumType, newValue);
                final boolean isSelected = enumValue.equals(newEnumValue);
                ((Button) control).setSelection(isSelected);
            } else if (control instanceof CCombo && (newValue == null || newValue instanceof String)) {
                CCombo combobox = (CCombo) control;
                final String valueOrDefault = valueOrDefault(newValue, "");
                if (!valueOrDefault.equals(combobox.getText())) {
                    combobox.setText(valueOrDefault);
                }
            } else if (control instanceof Combo && (newValue == null || newValue instanceof String)) {
                Combo combobox = (Combo) control;
                final String valueOrDefault = valueOrDefault(newValue, "");
                if (!valueOrDefault.equals(combobox.getText())) {
                    combobox.setText(valueOrDefault);
                }
            }
        }

        private Enum<?> getEnum(final Class<? extends Enum<?>> enumType, final String name) {
            Enum<?> result = null;
            for (final Field field : enumType.getFields()) {
                if (field.isEnumConstant()) {
                    try {
                        final Enum<?> enumValue = (Enum<?>) field.get(enumType);
                        if (enumValue.name().equals(name)) {
                            result = enumValue;
                            break;
                        }
                    } catch (final IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return result;
        }

        private String stringValue(final Control control, final String value, final String defaultValue) {
            final String result;
            if (value != null) {
                result = value;
            } else {
                result = defaultValue;
            }
            return result;
        }

        protected String valueOrDefault(final String value, final String defaultValue) {
            final String result;
            if (value != null) {
                result = value;
            } else {
                result = defaultValue;
            }
            return result;
        }

    }

    /**
     * Layout composite to allow for adapting the composite content to use optimal space in the containing <code>ScrolledComposite</code>.
     * 
     * <p>
     * The calculation of the width of this <code>Composite</code> is based on the hints provided by {@link #computeSize(int, int, boolean)}
     * calls. Therefor such a hint is saved in a local variable ({@link #widthHint}) and used whenever {@link SWT#DEFAULT} is used instead
     * of a meaningful width hint.
     * </p>
     * 
     * @author Christian Weiss
     */
    public static final class LayoutComposite extends Composite {

        /**
         * State memorizer used to ignore the first with hint.
         * <p>
         * The first width hint has to be ignored as the <code>ControlListener</code> gets registered too late to get the first meaningful
         * width hint.
         * </p>
         */
        private boolean first = true;

        /** Buffer variable to store/remember the last meaningful width hint. */
        private Integer widthHint = 0;

        public LayoutComposite(final Composite parent) {
            this(parent, SWT.NONE | SWT.TRANSPARENT);
        }

        public LayoutComposite(final Composite parent, final int style) {
            super(parent, style);
            final FillLayout layout = new FillLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.spacing = 0;
            setLayout(layout);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            /*
             * If a width hint is provided. >> Ignore, if it is the first one. >> Store, otherwise.
             */
            if (wHint != SWT.DEFAULT) {
                if (!first) {
                    this.widthHint = wHint;
                }
                first = false;
            }
            /*
             * Use the last meaningful width hint for size calculation. This way the (meaningful) hint is used to calculate the table size
             * and not the actual width of the columns.
             */
            if (widthHint != null) {
                wHint = Math.min(widthHint, getClientArea().width);
            }
            final Point result = super.computeSize(wHint, hHint, changed);
            /*
             * Store the default (min) width of the tree, if this is the very first call using no width hint.
             */
            if (first && wHint == SWT.DEFAULT) {
                this.widthHint = result.x;
            }
            result.x = 0;
            return result;
        }

    }

}
