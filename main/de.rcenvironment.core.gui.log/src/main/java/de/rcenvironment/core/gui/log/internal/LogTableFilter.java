/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.log.SerializableLogEntry;

/**
 * Listener for GUI-elements such as check box, drop down box, and text field to realize changes. In case of changes it pushes to refresh
 * displaying the data and organizes filtering table data.
 * 
 * @author Enrico Tappert
 */
public class LogTableFilter extends ViewerFilter implements SelectionListener, KeyListener {

    /** Constant. */
    private static final String Filter = ".*";

    private boolean myErrorSetup;

    private boolean myInfoSetup;

    private boolean myWarnSetup;

    private LogView myLoggingView;

    private String mySearchTerm;

    private TableViewer myTableViewer;

    private Pattern myPattern;

    public LogTableFilter(LogView loggingView, TableViewer tableViewer) {
        myLoggingView = loggingView;
        myTableViewer = tableViewer;

        updateTableView();

        myTableViewer.getTable().setFocus();
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        boolean returnValue = false;

        if (element instanceof SerializableLogEntry) {
            SerializableLogEntry logEntry = (SerializableLogEntry) element;

            if (isLevelSelected(logEntry.getLevel())) {
                returnValue = isSelectedBySearchTerm(logEntry.getMessage());
            }
        }

        return returnValue;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        updateTableView();
        myTableViewer.getTable().setFocus();
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        // do nothing
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        updateTableView();
    }

    /**
     * 
     * Collect view settings, push to refresh displaying.
     * 
     */
    private void updateTableView() {
        myErrorSetup = myLoggingView.getErrorSelection();
        myInfoSetup = myLoggingView.getInfoSelection();
        myWarnSetup = myLoggingView.getWarnSelection();

        setSearchTerm(myLoggingView.getSearchText());

        LogModel.getInstance().setSelectedLogSource(myLoggingView.getPlatform());

        myTableViewer.refresh();
    }

    private boolean isLevelSelected(int level) {
        boolean returnValue = false;

        if ((LogService.LOG_ERROR == level) && myErrorSetup) {
            returnValue = true;
        } else if ((LogService.LOG_INFO == level) && myInfoSetup) {
            returnValue = true;
        } else if ((LogService.LOG_WARNING == level) && myWarnSetup) {
            returnValue = true;
        }

        return returnValue;
    }

    private boolean isSelectedBySearchTerm(String message) {
        boolean returnValue = false;

        if (null == mySearchTerm || 0 == mySearchTerm.length()) {
            // "nothing" matches them all
            returnValue = true;
        } else if (messageMatchesSearchTerm(message)) {
            returnValue = true;
        }
        return returnValue;
    }

    private void setSearchTerm(String searchTerm) {
        // regular expression enables matching within a text
        // search is case insensitive

        // Escape all regex expressions except for the *
        char[] regexsymbols = { '(', ')', '{', '}', '[', ']', '^', '?', '.', '\\', '$', '|', '+' };
        mySearchTerm = searchTerm.toLowerCase();
        StringBuffer searchTermBuffer = new StringBuffer(mySearchTerm);

        int offset = 0;
        for (int i = 0; i < mySearchTerm.length(); i++) {
            for (int j = 0; j < regexsymbols.length; j++) {
                // Insert '\' before regexsymbol
                if (mySearchTerm.charAt(i) == regexsymbols[j]) {
                    searchTermBuffer.insert(i + offset, "\\");
                    offset++;
                }
            }
        }

        mySearchTerm = searchTermBuffer.toString();
        while (mySearchTerm.contains(" ")) {
            if (mySearchTerm.indexOf(" ") != mySearchTerm.length() - 1) {
                mySearchTerm = mySearchTerm.replaceFirst(" ", ".");
            } else {
                mySearchTerm = mySearchTerm.replaceFirst(" ", "");
            }
        }

        mySearchTerm = mySearchTerm.replaceAll("\\*", ".*");
        mySearchTerm = Filter + mySearchTerm + Filter;

        try {
            myPattern = Pattern.compile(mySearchTerm);
        } catch (PatternSyntaxException e) {
            myPattern = Pattern.compile(""); // should not happen
        }

    }

    private boolean messageMatchesSearchTerm(String message) {
        // search is case insensitive - see also method 'setSearchTerm'
        return myPattern.matcher(message.toLowerCase()).matches();
    }
}
