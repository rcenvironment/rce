/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
 */
public class ScriptSection extends AbstractScriptSection {

    private static final Log LOGGER = LogFactory.getLog(ScriptSection.class);

    private CCombo languages;

    public ScriptSection() {
        super(Messages.scriptname);
    }
    
    @Override
    protected void createCompositeContentAtVeryTop(Composite composite, TabbedPropertySheetWidgetFactory factory) {
        Composite scriptParent = factory.createFlatFormComposite(composite);
        scriptParent.setLayout(new RowLayout());
        new Label(scriptParent, SWT.NONE).setText(Messages.chooseLanguage);
        languages = new CCombo(scriptParent, SWT.BORDER | SWT.READ_ONLY);
        languages.setData(CONTROL_PROPERTY_KEY, ScriptComponentConstants.SCRIPT_LANGUAGE);
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
                setProperty(SshExecutorConstants.CONFIG_KEY_SCRIPT, returnValue);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }

    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refresh();
    }

}
