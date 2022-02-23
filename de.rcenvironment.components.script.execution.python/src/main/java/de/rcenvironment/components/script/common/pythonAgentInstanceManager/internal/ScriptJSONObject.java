/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal;

/*
 * 
 * Class to store Information about script for communication with Python instance via JSON notation.
 * 
 * 
 * @author Niklas Foerst
 *
 */

public class ScriptJSONObject {
    private String pythonCommand = null;
    private String script = null;
    
    public String getPythonCommand() {
        return this.pythonCommand; 
    }
    
    public void   setPythonComamnd(String pythonCommandParam){
        this.pythonCommand = pythonCommandParam;
    }

    public String  getScript() { 
        return this.script; 
    }
    
    public void setScript(String script) { 
        this.script = script; 
    }

}


