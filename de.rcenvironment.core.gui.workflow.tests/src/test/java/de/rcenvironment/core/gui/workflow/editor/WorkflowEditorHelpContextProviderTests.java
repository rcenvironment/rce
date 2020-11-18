/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;

public class WorkflowEditorHelpContextProviderTests {
    
    private WorkflowEditorHelpContextProviderTestHarness testHarness;
    
    @Before
    public void constructTestHarness() {
        this.testHarness = new WorkflowEditorHelpContextProviderTestHarness();
    }

    @Test
    public void testGetContextForNull() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIs(null);

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.rce.gui.workflow.editor");
    }

    @Test
    public void testGetContextForConnectionPart() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIs(new ConnectionPart());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.connectionEditorContext");
    }

    @Test
    public void testGetContextForWorkflowLabelPart() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIs(new WorkflowLabelPart());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.workflowLabelContext");
    }

    @Test
    public void testGetContextForWorkflowPart() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIs(new WorkflowPart());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.workflowEditorContext");
    }

    @Test
    public void testGetContextForWorkflowExecutionInformationPart() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIs(new WorkflowExecutionInformationPart());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.runtimeWorkflowEditorContext");
    }

    @Test
    public void testGetContextForWorkflowNodePartWithMatchingIntegrationContext() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIsWorkflowNodePartWithComponentIdentifier(someComponentIdentifier())
            .registryHasTIContextMatchingPrefix(someComponentIdentifier());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.integration.*");
    }

    @Test
    public void testGetContextForWorkflowNodePartWithRemoteAccessComponent() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIsWorkflowNodePartWithComponentIdentifier(someRemoteAccessComponentIdentifier())
            .registryHasNoTIContextMatchingPrefix(someRemoteAccessComponentIdentifier());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.remoteaccess.*");
    }

    @Test
    public void testGetContextForWorkflowNodePartWithBuiltinComponent() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIsWorkflowNodePartWithComponentIdentifier(joinerComponentIdentifierWithVersion())
            .registryHasNoTIContextMatchingPrefix(joinerComponentIdentifierWithVersion());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas(joinerComponentIdentifier());
    }

    @Test
    public void testGetContextForWorkflowNodePartWithWorkflowComponent() {
        testHarness.given()
            .providerIsConstructedWithArbitraryViewer()
            .selectedElementIsWorkflowNodePartWithComponentIdentifier(workflowComponentIdentifierWithVersion())
            .registryHasNoTIContextMatchingPrefix(workflowComponentIdentifierWithVersion());

        testHarness.when()
            .gettingContext();
        
        testHarness.then()
            .queriedContextIdWas("de.rcenvironment.workflow");
    }

    protected static String someComponentIdentifier() {
        return "someIdentifier";
    }
    
    protected static String someRemoteAccessComponentIdentifier() {
        return "de.rcenvironment.remoteaccess.component";
    }
    
    protected static String joinerComponentIdentifierWithVersion() {
        return "de.rcenvironment.joiner/4.2";
    }

    protected static String joinerComponentIdentifier() {
        return "de.rcenvironment.joiner";
    }

    protected static String workflowComponentIdentifierWithVersion() {
        return "de.rcenvironment.integration.workflow.Fibonacci/0.0";
    }

}
