/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.HashSet;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link ParallelComponentCaller}.
 * 
 * @author Doreen Seider
 */
public class ParallelComponentCallerTest {

    private static final String METHOD_NAME = "method-to-call";

    private static final String COMP_EXE_ID_1 = "comp-exe-id-1";

    private static final String COMP_EXE_ID_2 = "comp-exe-id-2";
    
    private static final Set<String> COMP_EXE_IDS = new HashSet<>();
    
    /**
     * Set up test scenario.
     */
    @BeforeClass
    public static void setup() {
        COMP_EXE_IDS.add(COMP_EXE_ID_1);
        COMP_EXE_IDS.add(COMP_EXE_ID_2);        
    }

    /**
     * Tests if abstracted methods are called as expected.
     * @throws RemoteOperationException on unexpected errors
     * @throws ExecutionControllerException on unexpected errors
     */
    @Test
    public void testCallingComponentSucceeding() throws ExecutionControllerException, RemoteOperationException {
        
        Callbacks callbacksMock = EasyMock.createStrictMock(Callbacks.class);
        Capture<String> capturedCompExeId1 = Capture.newInstance();
        callbacksMock.callSingleComponent(EasyMock.capture(capturedCompExeId1));
        Capture<String> capturedCompExeId2 = Capture.newInstance();
        callbacksMock.callSingleComponent(EasyMock.capture(capturedCompExeId2));
        EasyMock.expect(callbacksMock.getMethodToCallAsString()).andStubReturn(METHOD_NAME);
        EasyMock.replay(callbacksMock);
        
        TestableParallelComponentCaller caller = new TestableParallelComponentCaller(COMP_EXE_IDS, createWorkflowExecutionContextMock());
        caller.setCallbacks(callbacksMock);
        Throwable t = caller.callParallelAndWait();

        EasyMock.verify(callbacksMock);

        Assert.assertNull(t);
        assertCallToCallSingleComponent(capturedCompExeId1, capturedCompExeId2);
    }
    
    /**
     * Tests if abstracted methods are called as expected.
     * @throws RemoteOperationException on unexpected errors
     * @throws ExecutionControllerException on unexpected errors
     */
    @Test
    public void testCallingThrowingRuntimeException() throws ExecutionControllerException, RemoteOperationException {
        testCallingComponentThrowingAnException(new ExecutionControllerException("ece-exe-message"));
        testCallingComponentThrowingAnException(new RuntimeException());
        testCallingComponentThrowingAnException(new RemoteOperationException("roe-exe-message"));
    }
    
    private void testCallingComponentThrowingAnException(Exception e) throws ExecutionControllerException, RemoteOperationException {
        
        Callbacks callbacksMock = EasyMock.createStrictMock(Callbacks.class);
        Capture<String> capturedCompExeId1 = Capture.newInstance();
        callbacksMock.callSingleComponent(EasyMock.capture(capturedCompExeId1));
        Capture<String> capturedCompExeId2 = Capture.newInstance();
        callbacksMock.callSingleComponent(EasyMock.capture(capturedCompExeId2));
        EasyMock.expectLastCall().andThrow(e);
        EasyMock.expect(callbacksMock.getMethodToCallAsString()).andStubReturn(METHOD_NAME);
        EasyMock.replay(callbacksMock);
        
        TestableParallelComponentCaller caller = new TestableParallelComponentCaller(COMP_EXE_IDS, createWorkflowExecutionContextMock());
        caller.setCallbacks(callbacksMock);
        String methodName = caller.getMethodToCallAsString();
        Throwable t = caller.callParallelAndWait();
        
        EasyMock.verify(callbacksMock);
        
        Assert.assertEquals(METHOD_NAME, methodName);
        Assert.assertTrue(t.getMessage().contains("Failed to"));
        Assert.assertEquals(e, t.getCause());
        assertCallToCallSingleComponent(capturedCompExeId1, capturedCompExeId2);
    }
    
    private void assertCallToCallSingleComponent(Capture<String> capturedCompExeId1, Capture<String> capturedCompExeId2) {
        Assert.assertEquals(1, capturedCompExeId1.getValues().size());
        Assert.assertEquals(1, capturedCompExeId2.getValues().size());
        Assert.assertNotSame(capturedCompExeId1.getValues().get(0), capturedCompExeId2.getValues().get(0));
        Assert.assertTrue(COMP_EXE_IDS.contains(capturedCompExeId1.getValues().get(0)));
        Assert.assertTrue(COMP_EXE_IDS.contains(capturedCompExeId2.getValues().get(0)));
    }
    
    private WorkflowExecutionContext createWorkflowExecutionContextMock() {
        WorkflowExecutionContext wfExeCtxMock = EasyMock.createStrictMock(WorkflowExecutionContext.class);
        EasyMock.expect(wfExeCtxMock.getInstanceName()).andReturn("wf");
        EasyMock.expect(wfExeCtxMock.getExecutionIdentifier()).andReturn("exe id");
        EasyMock.replay(wfExeCtxMock);
        return wfExeCtxMock;
    }
    
    /**
     * Stub implementation of {@link ParallelComponentCaller}.
     * 
     * @author Doreen Seider
     */
    private class TestableParallelComponentCaller extends ParallelComponentCaller {

        private Callbacks callbacks;
        
        protected TestableParallelComponentCaller(Set<String> componentsToConsider, WorkflowExecutionContext wfExeCtx) {
            super(componentsToConsider, wfExeCtx);
        }

        @Override
        protected void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
            callbacks.callSingleComponent(compExeId);
        }

        @Override
        protected String getMethodToCallAsString() {
            return callbacks.getMethodToCallAsString();
        }
        
        private void setCallbacks(Callbacks callbacks) {
            this.callbacks = callbacks;
        }
        
    }
    
    /**
     * Interface used to recognize abstract method calls.
     * 
     * @author Doreen Seider
     */
    private interface Callbacks {
        
        void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException;
        
        String getMethodToCallAsString();
        
    }

}
