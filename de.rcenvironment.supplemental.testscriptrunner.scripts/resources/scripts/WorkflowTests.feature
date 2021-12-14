Feature: WorkflowTests

@Workflow01
@NoGUITestSuite
Scenario: Execute a distributed workflow on a single node and expect fallback to local component execution

  Given running instance "NodeA" using the default build
  When executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeA"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeA"


@Workflow02
@DefaultTestSuite
@NoGUITestSuite
Scenario: Execute a distributed workflow, testing the local fallback case first, and then proper remote component execution

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  
  # set local access to the Joiner component on NodeB (to make sure no permissions from previous tests exist) 
  When  executing command "components set-auth rce/Joiner local" on "NodeB"
  Then  the output should contain "Set access authorization"

  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # all Joiner components should fall back to NodeA as there is no such component available on NodeB
  When executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeA"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeA"

  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"

  # wait to make sure component authorization has propagated to NodeA before starting the workflow
  When  waiting for 1 second

  # now the workflow should execute Joiner 1 and 3 on NodeB as stored in the workflow file; 
  # this also verifies that the previous workflow run did not erase the node settings from the .wf file during fallback 
  When executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeB" using node id "00000000000000000000000000000002"


@Workflow03
@WfRobustness01
@NoGUITestSuite
Scenario: Network disruptions during distributed workflow with local controller

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # NodeA is the instance that initiated the connection, so it must be the one to cycle the connection
  When  scheduling a reconnect of "NodeA" after 5 seconds
  And   scheduling a reconnect of "NodeA" after 10 seconds
  And   scheduling a reconnect of "NodeA" after 15 seconds
  And   scheduling a reconnect of "NodeA" after 20 seconds

  And   executing workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "NodeA"
  Then  the workflow controller should have been "NodeA"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   the workflow should have reached the FINISHED state


@Workflow04
@WfRobustness02
@NoGUITestSuite
Scenario: Network disruptions during distributed workflow with remote controller

  Given instances "NodeA, NodeB [WfHost;Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # NodeA is the instance that initiated the connection, so it must be the one to cycle the connection
  When  scheduling a reconnect of "NodeA" after 5 seconds
  And   scheduling a reconnect of "NodeA" after 10 seconds
  And   scheduling a reconnect of "NodeA" after 15 seconds
  And   scheduling a reconnect of "NodeA" after 20 seconds

  # Initiate the workflow on NodeA, but the controller is set to id 2 (NodeB) inside the workflow file
  And   executing workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_on_node2.wf" on "NodeA"
  Then  the workflow controller should have been "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  # Check debug.log as a workaround as long as remote workflow log capture is not robust against network disruption (issue #0016137)
  And   the log output of "NodeB" should contain the pattern "Workflow .* is now FINISHED"


@Workflow25
Scenario: Remote node restart repeatedly during distributed workflow with local controller

  Given instances "Uplink, NodeB [Id=00000000000000000000000000000002], NodeC [Id=00000000000000000000000000000003]" using the default build
  #And   configured network connections "NodeA->NodeB [autoStart]"
  And   configured network connections "NodeB-[upl]->Uplink [autoRetry], NodeC-[upl]->Uplink [autoRetry]" 
  When  starting instances "Uplink, NodeB" concurrently
  # set public access to the Joiner component on NodeB 
  And  executing command "components set-auth rce/Joiner public" on "NodeB"
  And  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds
  
  # even though the network connection may have been established, wait for NodeB to fully announce components etc.
  And  waiting for 2 seconds
  # schedule restart of NodeB while the workflow should be running
  #And   scheduling a restart of "NodeB" after 2 seconds
  And   starting workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "Uplink"

  And   waiting for 15 seconds

  And   starting instance "NodeC" 
  And   waiting for 55 seconds

  Then  the workflow controller should have been "Uplink"
  And   workflow component "Local 1" should have been run on "Uplink"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "Uplink"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  # The workflow should have failed within a reasonable time (instead of waiting indefinitely)
  And   the workflow should have reached the FAILED state
  # Also, NodeA should have received and logged an exception stating that the remote node restart was specifically detected
  And   the log output of "NodeA" should contain the pattern "(?:has been restarted|the remote node was restarte|The destination instance for this request was restarted)"

  # Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
  When  waiting for 15 seconds
  
@Workflow05
@WfRobustness03
@NoGUITestSuite
Scenario: Remote node start during distributed workflow with local controller

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds
  
  # even though the network connection may have been established, wait for NodeB to fully announce components etc.
  When  waiting for 2 seconds
  # schedule restart of NodeB while the workflow should be running
  And   scheduling a restart of "NodeB" after 2 seconds
  And   starting workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "NodeA"

  And   waiting for 15 seconds
  Then  the workflow controller should have been "NodeA"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  # The workflow should have failed within a reasonable time (instead of waiting indefinitely)
  And   the workflow should have reached the FAILED state
  # Also, NodeA should have received and logged an exception stating that the remote node restart was specifically detected
  And   the log output of "NodeA" should contain the pattern "(?:has been restarted|the remote node was restarted|The destination instance for this request was restarted)"

  # Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
  When  waiting for 15 seconds
  
  
@Workflow06
@DefaultTestSuite
@NoGUITestSuite
Scenario: Executing "wf self-test"

  # note: this test frequently fails when testing 8.1.0 or 8.1.1 as the SSH timeout was accidentally set too low in these releases  

  Given the running instance "NodeA" using the default build

  When  executing the command "wf self-test" on "NodeA"
  Then  the output should contain "Verification SUCCEEDED"
  
  
@Workflow07
@SSHTestSuite
@NoGUITestSuite
Scenario: Running Workflow with remote tool, published via uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    When starting instances "Client1, Client2, Uplink1" in the given order 
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And executing workflow "UplinkRemoteWorkflow.wf" on "Client2"
    
    Then the workflow controller should have been "Client2"
    And workflow component "Optimizer" should have been run on "Client2"
    And workflow component "TestTool" should have been run on "Client1" via uplink
    
    
@Workflow08
@SSHTestSuite
@NoGUITestSuite
Scenario: Running Workflow with remote tool, published via uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    When starting instances "Client1, Client2, Uplink1" in the given order
    And adding tool "common/LongTestTool" to "Client1"
    And executing command "components set-auth common/LongTestTool public" on "Client1"
    And starting workflow "UplinkRemoteWorkflowAbortLong.wf" on "Client2"
    And waiting for 10 seconds
    And cancelling workflow "UplinkRemoteWorkflowAbortLong.wf" on "Client2"
    
    Then workflow component "LongTestTool" should have been cancelled
    
@Workflow09
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario Outline: Workflow Info via Network

	Given instance "NodeA [WorkflowHost]" using the <NodeA_build> build
	And   instance "NodeB" using the <NodeB_build> build
    And   configured network connections "NodeA-[reg]->NodeB [autoStart], NodeB-[reg]->NodeA [autoStart]"
    
    When  starting all instances
    And   adding tool "common/TestTool" to "NodeB"
    And   executing command "components set-auth common/TestTool public" on "NodeB"
    And   executing workflow "UplinkRemoteWorkflow.wf" on "NodeA"
    
    Then  instance "NodeB" should see workflow "UplinkRemoteWorkflow"
    
    Examples:
    |NodeA_build|NodeB_build|
    |default|default|
    |default|base|
    |base|default|
    
@Workflow10
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario Outline: Workflow Data via Network

	Given instance "NodeA [WorkflowHost]" using the <NodeA_build> build
	And   instance "NodeB" using the <NodeB_build> build
    And   configured network connections "NodeA-[reg]->NodeB [autoStart], NodeB-[reg]->NodeA [autoStart]"
    
    When  starting all instances
    And   adding tool "common/TestTool" to "NodeB"
    And   executing command "components set-auth common/TestTool public" on "NodeB"
    And   executing workflow "UplinkRemoteWorkflow.wf" on "NodeA"
    
    Then  instances "NodeA, NodeB" should see identical data for workflow "UplinkRemoteWorkflow"
    
    Examples:
    |NodeA_build|NodeB_build|
    |default|default|
# executing these cases should be possible after the next major update, since the base version then has the necessary commands to export wf runs
#    |default|base|
#    |base|default|

# executing this scenario should be possible after the next major update, since the base version then has the necessary commands to export wf runs
#@Workflow11 
#@NoGUITestSuite
#@BasicIntegrationTestSuite
#Scenario: Workflow Data maintained different versions
#
#	Given instance "NodeA" using the default build
#	
#	When  starting instance "NodeA"
#	And   executing workflows "example_workflows\\01_01_Hello_World.wf, example_workflows\\01_02_Coupling_Components.wf, example_workflows\\01_03_Data_Types.wf, example_workflows\\01_04_Component_Execution_Scheduling.wf, example_workflows\\02_02_Evaluation_Drivers.wf, example_workflows\\03_01_Simple_Loop.wf, example_workflows\\03_02_Forwarding_Values.wf, example_workflows\\03_03_Nested_Loop.wf, example_workflows\\03_04_Fault-tolerant_Loop.wf" on "NodeA"
#	And   exporting all workflow runs from "NodeA" to "base"
#	And   stopping instance "NodeA"
#	
#	Given the same instance "NodeA" using the default build
#	
#	When  starting instance "NodeA"
#	And   exporting all workflow runs from "NodeA" to "default"
#	
#	Then  all exported workflow run directories from "NodeA" should be identical
	
# workflow 2.3 (Workflow Examples Project\\02_Component Groups\\02_03_XML_Components.wf [02_03_XML_Components.json]) is not tested as it produces errors.

@Workflow12
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario: Execute all example workflows

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	#And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf, Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf, Workflow Examples Project/01_First Steps/01_03_Data_Types.wf, Workflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf, Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json], Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf, Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json], Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf, Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf,  Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   executing workflows "Workflow Examples Project\\01_First Steps\\01_01_Hello_World.wf, Workflow Examples Project\\01_First Steps\\01_02_Coupling_Components.wf, Workflow Examples Project\\01_First Steps\\01_03_Data_Types.wf, Workflow Examples Project\\01_First Steps\\01_04_Component_Execution_Scheduling.wf, Workflow Examples Project\\02_Component Groups\\02_01_Data_Flow.wf [02_01_Data_Flow.json], Workflow Examples Project\\02_Component Groups\\02_02_Evaluation_Drivers.wf, Workflow Examples Project\\02_Component Groups\\02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json], Workflow Examples Project\\03_Workflow Logic\\03_01_Simple_Loop.wf, Workflow Examples Project\\03_Workflow Logic\\03_02_Forwarding_Values.wf, Workflow Examples Project\\03_Workflow Logic\\03_03_Nested_Loop.wf, Workflow Examples Project\\04_Tool Integration\\04_01_Example_Integration.wf, Workflow Examples Project\\04_Tool Integration\\04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_01_Data_Flow.wf [02_01_Data_Flow.json]" on "NodeA"
    #And   executing workflows "Workflow Examples Project\\01_First Steps\\01_01_Hello_World.wf, Workflow Examples Project\\01_First Steps\\01_02_Coupling_Components.wf, Workflow Examples Project\\01_First Steps\\01_03_Data_Types.wf, Workflow Examples Project\\01_First Steps\\01_04_Component_Execution_Scheduling.wf, Workflow Examples Project\\02_Component Groups\\02_02_Evaluation_Drivers.wf, Workflow Examples Project\\02_Component Groups\\02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json], Workflow Examples Project\\03_Workflow Logic\\03_01_Simple_Loop.wf, Workflow Examples Project\\03_Workflow Logic\\03_02_Forwarding_Values.wf, Workflow Examples Project\\03_Workflow Logic\\03_03_Nested_Loop.wf, Workflow Examples Project\\04_Tool Integration\\04_01_Example_Integration.wf, Workflow Examples Project\\04_Tool Integration\\04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json]" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
# the following example workflows are missing in this test scenario:
# workflow 2.3 (Workflow Examples Project\\02_Component Groups\\02_03_XML_Components.wf [02_03_XML_Components.json]) is not tested as it produces errors.
# > the files required by the input provider will not be copied in the working dir at the moment. A new Test Step is needed here
# workflow 3.4 Workflow Examples Project\\03_Workflow Logic\\03_04_Fault-tolerant_Loop.wf
# > This wf produces an error for testing purposes. Therefore the Then step is not applicable here. A new Test is needed. 
# The above described test scenarios has to implemented after release 10.1
# Kathrin Schaffert, 28.04.2020
# removed Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf because test get stuck during execution of wf
# might depend on incorrect adding of the Example tool in Step "adding tool "common/Example" to "NodeA"", but has to be checked
# Kathrin Schaffert, 18.06.2020
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

# Workflow13 is about running all example workflows
# If we will keep the separated Workflow13 instead of Workflow12, we should add the @BasicIntegrationTestSuite here. (K.Schaffert, 16.12.2020)

@Workflow13
@ExampleWorkflow0101
Scenario: Execute example workflow 01_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\01_First Steps\\01_01_Hello_World.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0102
Scenario: Execute example workflow 01_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\01_First Steps\\01_02_Coupling_Components.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0103
Scenario: Execute example workflow 01_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/01_First Steps/01_03_Data_Types.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\01_First Steps\\01_03_Data_Types.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0104
Scenario: Execute example workflow 01_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow ExamplesWorkflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\01_First Steps\\01_04_Component_Execution_Scheduling.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0201
Scenario: Execute example workflow 02_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json]" on "NodeA"
    And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_01_Data_Flow.wf [02_01_Data_Flow.json]" on "NodeA"
    And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0202
Scenario: Execute example workflow 02_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_02_Evaluation_Drivers.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0203
Scenario: Execute example workflow 02_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And waiting for 30 seconds
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    #And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    And copying configuration files "CPACS.xml, MappingRules.xsl, XMLMerger_Integrate.xml" of workflow group "02_Component Groups" into installation workspace
    And waiting for 30 seconds
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_03_XML_Components.wf [02_03_XML_Components.json]" on "NodeA"
    #And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_03_XML_Components.wf [02_03_XML_Components.json]" on "NodeA"
    And   waiting for 60 seconds
    And   stopping instance "NodeA"
    And   waiting for 60 seconds
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0204
Scenario: Execute example workflow 02_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json]" on "NodeA"
    And   executing workflows "Workflow Examples Project\\02_Component Groups\\02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json]" on "NodeA"
    And   waiting for 15 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0301
Scenario: Execute example workflow 03_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\03_Workflow Logic\\03_01_Simple_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0302
Scenario: Execute example workflow 03_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\03_Workflow Logic\\03_02_Forwarding_Values.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0303
Scenario: Execute example workflow 03_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\03_Workflow Logic\\03_03_Nested_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0304
Scenario: Execute example workflow 03_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_04_Fault-tolerant_Loop.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\03_Workflow Logic\\03_04_Fault-tolerant_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"

	Then   the log output of "NodeA" should contain the pattern "(?:de.rcenvironment.core.component.api.ComponentException: Script execution error: Exception: Example failure in <script> at line number 2)"
	And    the log output of "NodeA" should contain 1 error
	
# Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
#When  waiting for 5 second

@Workflow13
@ExampleWorkflow0401
Scenario: Execute example workflow 04_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\04_Tool Integration\\04_01_Example_Integration.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow13
@ExampleWorkflow0402
Scenario: Execute example workflow 04_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    #And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
	And   copying "workflows\\Workflow Examples Project" into workspace of "NodeA"
    #And   executing workflows "Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    And   executing workflows "Workflow Examples Project\\04_Tool Integration\\04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors


@Workflow14
@NoGUITestSuite
Scenario: Properties containing umlauts are echoed correctly

		Given running instance "NodeA" using the default build
		And   adding tool "common/Echo" to "NodeA"
		
		When executing workflow "EncodingTestWorkflow" on "NodeA"
		
		Then that workflow run should be identical to "EncodingTestWorkflow"
		And the log output of "NodeA" should indicate a clean shutdown with no warnings or errors
