/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactoryRegistry;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.workflow.executor.properties.AbstractScriptSection;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * "Properties" view tab for loading and editing script files.
 * 
 * @author Markus Litz
 * @author Arne Bachmann
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Hendrik Abbenhaus
 * @author Kathrin Schaffert
 */
public class ScriptSection extends AbstractScriptSection {

    private static final String JYTHON = "Jython";

    private static final String PYTHON = "Python";

    private static final Log LOGGER = LogFactory.getLog(ScriptSection.class);

    private Combo languages;

    public ScriptSection() {
        super(Messages.scriptname);
    }
    
    @Override
    protected void createCompositeContentAtVeryTop(Composite composite, TabbedPropertySheetWidgetFactory factory) {
        Composite scriptParent = factory.createFlatFormComposite(composite);
        scriptParent.setLayout(new RowLayout());
        new Label(scriptParent, SWT.NONE).setText(Messages.chooseLanguage);
        languages = new Combo(scriptParent, SWT.BORDER | SWT.READ_ONLY);
        languages.setData(CONTROL_PROPERTY_KEY, ScriptComponentConstants.SCRIPT_LANGUAGE);
        languages.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = languages.getSelectionIndex();
                if (index == 1) {
                    setProperty(ScriptComponentConstants.SCRIPT_LANGUAGE, PYTHON);
                } else {
                    setProperty(ScriptComponentConstants.SCRIPT_LANGUAGE, JYTHON);
                }
            }

        });
        languages.addListener(SWT.MouseWheel, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                // deactivate MouseWheel interaction for Script language dropdown menu
                arg0.doit = false;
            }
        });

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        ScriptExecutorFactoryRegistry scriptExecutorRegistry = serviceRegistryAccess.getService(ScriptExecutorFactoryRegistry.class);
        List<ScriptLanguage> languagesForCombo =
            scriptExecutorRegistry.getCurrentRegisteredExecutorLanguages();
        for (ScriptLanguage sl : languagesForCombo) {
            languages.add(sl.getName());
        }
    }


    @Override
    public void refreshSection() {
        super.refreshSection();
        refreshScriptSection();
    }

    /**
     * 
     */
    public void refreshScriptSection() {
        if (getProperty(ScriptComponentConstants.SCRIPT_LANGUAGE) == null
            || ((String) getProperty(ScriptComponentConstants.SCRIPT_LANGUAGE)).isEmpty()
            || languages.getText().equals("\"\"")) {
            if (languages.getItemCount() > 0) {
                languages.select(0);
            }
        }

        // replace current script by content of defaultScript.py when:
        // (a) the current text is either null or empty or
        // (b) it ends like the pre-configured script (see configuration.json) and has the some length.
        if (getProperty(SshExecutorConstants.CONFIG_KEY_SCRIPT) == null
            || getProperty(SshExecutorConstants.CONFIG_KEY_SCRIPT).equals(
                ScriptComponentConstants.DEFAULT_SCRIPT_WITHOUT_COMMENTS_AND_IMPORTS)) {
            try {
                final InputStream is;
                if (getClass().getResourceAsStream("/resources/defaultScript.py") == null) {
                    is = new FileInputStream("./resources/defaultScript.py");
                } else {
                    is = getClass().getResourceAsStream("/resources/defaultScript.py");
                }
                final String returnValue = IOUtils.toString(is);
                IOUtils.closeQuietly(is);
                setPropertyNotUndoable(SshExecutorConstants.CONFIG_KEY_SCRIPT, returnValue);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }

    }
}
