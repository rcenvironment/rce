/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * History point of Excel component.
 *
 * @author Markus Kunde
 */
@Deprecated
public class ExcelHistoryObject implements Serializable {

    private static final long serialVersionUID = 7807032311798848955L;
    
    private List<ChannelValue> historyPoints;
    
    public void setHistoryPoints(final List<ChannelValue> historyPoints) {
        this.historyPoints = new ArrayList<ChannelValue>(historyPoints);
    }
    
    public List<ChannelValue> getHistoryPoints() {
        return historyPoints;
    }
}
