/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.Variant;


/**
 * Encapsulates all the generic communication with the active-x interface. It is used as superclass for
 * most of the other classes in the ExcelAutomationpackage.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public class OleXLGeneral {

    private static final Log LOGGER = LogFactory.getLog(OleXLGeneral.class); 

    protected OleAutomation oleObject;

    protected OleControlSite controlSite;

    private int[] dispIDs;

    /**
     * General constructor.
     * 
     * @param oleContainer Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLGeneral(OleAutomation oleGeneral, OleControlSite controlSite) {
        this.oleObject = oleGeneral;
        this.controlSite = controlSite;
    }

    /**
     * Function to downsize the active-x interface for getting the dispatch ID of a dispatch
     * interface.
     * 
     * @param name Name of the interface to which the ID needs to be resolved.
     * @return Gives the corresponding ID to the dispatch interface.
     */
    protected int getIDofName(String name) {
        // Call the OleInterface to get the appropriate internal ID referring
        // to the entered name
        dispIDs = oleObject.getIDsOfNames(new String[] { name });
        logOleError(oleObject.getLastError());
        // TODO Needs a fix for the case that dispatch interface is not found and the return array
        // is null.

        // Since we are just looking for one name of the dispatch interface
        // only the first item in the array will be written. Hence we return the index 0
        return dispIDs[0];
    }

    /**
     * Invokes a method by its DispatchName.
     * 
     * @param name DispatchName that should be invoked.
     */
    protected void invokeNoReply(String name) {
        int invokeID = getIDofName(name);
        oleObject.invokeNoReply(invokeID);
        logOleError(oleObject.getLastError());
    }

    /**
     * Invokes a method by its DispatchName.
     * 
     * @param name DispatchName that should be invoked.
     * @return A variant containing the result off the invoke.
     */
    protected Variant invoke(String name) {
        int invokeID = getIDofName(name);
        Variant ret = oleObject.invoke(invokeID);
        logOleError(oleObject.getLastError());
        return ret;
    }

    /**
     * Invokes a method by its DispatchName.
     * 
     * @param name DispatchName that should be invoked.
     * @param params an array of Variants holding the values of the parameters you are passing in.
     * @return A variant containing the result off the invoke.
     */
    protected Variant invoke(String name, Variant[] params) {
        int invokeID = getIDofName(name);
        Variant ret = oleObject.invoke(invokeID, params);
        logOleError(oleObject.getLastError());
        return ret;
    }

    /**
     * Gets a property by its DispatchName.
     * 
     * @param name DispatchName that should be accessed.
     * @return Variant containing the property value.
     */
    protected Variant getProperty(String name) {
        int propertyID = getIDofName(name);
        Variant ret = oleObject.getProperty(propertyID);
        logOleError(oleObject.getLastError());
        return ret;
    }
    
    /**
     * Gets a property by its DispatchName and arguments.
     * 
     * @param name DispatchName that should be accessed.
     * @param varArgs Arguments relating to the property.
     * @return Variant containing the property value.
     */
    protected Variant getProperty(String name, Variant[] varArgs) {
        int propertyID = getIDofName(name);
        Variant ret = oleObject.getProperty(propertyID, varArgs);
        logOleError(oleObject.getLastError());
        return ret;
    }

    /**
     * Sets a property by its DispatchName.
     * 
     * @param name DispatchName of the property that should be overwritten.
     * @param value A variant that will be written to the property.
     */
    protected void setProperty(String name, Variant value) {
        int propertyID = getIDofName(name);
        oleObject.setProperty(propertyID, value);
        logOleError(oleObject.getLastError());
    }

    /**
     * Gives access to associated OleAutomation object.
     * 
     * @return OleAutomation object that is attached to this object.
     */
    protected OleAutomation getOleObject() {
        return oleObject;
    }

    /**
     * Sets the OleAutomation object.
     * 
     * @param oleObject The Object that is supposed to be associated with this object.
     */
    protected void setOleObject(OleAutomation oleObject) {
        this.oleObject = oleObject;
    }

    /**
     * Nearly all objects in the Excel COM object model has the property "Application" which can be
     * read to get access to the corresponding "Excel.Application". This is very useful in case you
     * have created Excel via "Excel.sheet" or similar.
     * 
     * @return Hands back the OleXLApplication object.
     */
    public OleXLApplication getApplication() {
        // Get the ID of the application property and read it to create an automation object out of
        // this
        int idXLApplication = getIDofName("Application");
        OleAutomation automationObject = oleObject.getProperty(idXLApplication).getAutomation();
        logOleError(oleObject.getLastError());
        
        // Now use the automation object to create the OleXLApplication object
        OleXLApplication retXLApplication = new OleXLApplication(automationObject, controlSite);

        // hand back the created object
        return retXLApplication;
    }

    /**
     * Factory method to give access to the ExcelEvents.
     * 
     * @return ExcelEvent object.
     */
    public OleXLEvents getEvents() {
        return new OleXLEvents(getApplication().oleObject, controlSite);
    }
    
    /**
     * Writes OLE error (if existing) to logger.
     * 
     * @param oleError OLEAutomation.getLastError()
     */
    protected void logOleError(final String oleError) {
        if (oleError != null && !(oleError.equalsIgnoreCase("null") || oleError.equalsIgnoreCase("No Error"))) {
            LOGGER.error("OleAutomation: " + oleError);
        }
    }
}
