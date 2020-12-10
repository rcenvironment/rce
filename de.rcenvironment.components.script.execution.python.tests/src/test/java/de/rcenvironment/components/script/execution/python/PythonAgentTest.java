/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.python;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

import javax.script.ScriptException;

import org.easymock.Capture;
import org.easymock.CaptureType;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.PythonAgent;
import de.rcenvironment.core.component.execution.api.ComponentContext;

public class PythonAgentTest {
    
    private class PythonAgentUnderTest extends PythonAgent {
        
        public PythonAgentUnderTest(PythonAgentInstanceManager instanceManager, String pythonInstallationPath, int number,
            ServerSocket serverSocket, CountDownLatch initializationSignal, ComponentContext compCtx) throws ScriptException {
            super(instanceManager, pythonInstallationPath, number, serverSocket, initializationSignal, compCtx);
            // TODO Auto-generated constructor stub
        }
        private Capture<String> sentMessages = Capture.newInstance(CaptureType.ALL);
        private Capture<String> recvMessages = Capture.newInstance(CaptureType.ALL);
        @Override
        protected void sendMessage(String script) {
            this.sentMessages.setValue(script);
        }
       @Override
       protected String recvMessage() throws IOException {
//           String input = in.readLine();
          // compCtx.getLog().componentInfo("Python Instance: " + input);
//           this.recvMessages.setValue(input);
           return "";
           
       }
       @Override
       protected void writeNewLineToLog() {
           //stubbed for testing
       }
 
    }

}
