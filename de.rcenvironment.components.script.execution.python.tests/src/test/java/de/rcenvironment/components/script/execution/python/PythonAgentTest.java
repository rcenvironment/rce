package de.rcenvironment.components.script.execution.python;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.script.ScriptException;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.PythonAgent;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.PythonAgentInstanceManagerImpl;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.ScriptJSONObject;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

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
