/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
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
 */
public class ScriptSection extends AbstractScriptSection {

    private static final Log LOGGER = LogFactory.getLog(ScriptSection.class);

    private CCombo languages;

    public ScriptSection() {
        super(AbstractScriptSection.LOCAL_FILE | AbstractScriptSection.NEW_SCRIPT_FILE | AbstractScriptSection.NO_SCRIPT_FILENAME,
            Messages.scriptname);
    }

    @Override
    protected void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        /*
         * Inspecting the build-up of super-class section. Parent composite has a section which has a layout composite. The label-text will
         * be inserted at first position.
         */
        Composite parentComposite = null;
        Control[] parentChildControls = parent.getChildren();
        if (parentChildControls[0] instanceof Section) {
            Control[] sectionChildControls = ((Section) parentChildControls[0]).getChildren();
            for (int i = 0; i < sectionChildControls.length; i++) {
                if (sectionChildControls[i] instanceof Composite) {
                    parentComposite = (Composite) sectionChildControls[i];
                    break;
                }
            }
        }

        if (parentComposite != null) {
            TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
            Composite scriptParent = factory.createFlatFormComposite(parentComposite);
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
            scriptParent.moveAbove(parentComposite.getChildren()[0]);
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
