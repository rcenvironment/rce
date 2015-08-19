/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.gui.properties;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.cpacs.gui.xpathchooser.Messages;
import de.rcenvironment.cpacs.gui.xpathchooser.XPathChooserPropertyView;
import de.rcenvironment.cpacs.utils.common.components.ChameleonCommonConstants;


/**
 * Extending input/output property tab with directory checkboxes.
 *
 * @author Markus Kunde
 */
public class ToolWrapperDirectoryChannelsSection extends XPathChooserPropertyView {

    public ToolWrapperDirectoryChannelsSection() {
        super();
        ToolWrapperDirectoryPropertyViewPane inputPane =
            new ToolWrapperDirectoryPropertyViewPane(Messages.inputPaneName, EndpointType.INPUT, this,
                ChameleonCommonConstants.ID_INPUT_PANE);
        ToolWrapperDirectoryPropertyViewPane outputPane =
            new ToolWrapperDirectoryPropertyViewPane(Messages.outputPaneName, EndpointType.OUTPUT, this,
                ChameleonCommonConstants.ID_OUTPUT_PANE);
        setColumns(2);
        
        setPanes(inputPane, outputPane);

        inputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
        outputPane.setAllPanes(new EndpointSelectionPane[] { inputPane, outputPane });
    }
}
