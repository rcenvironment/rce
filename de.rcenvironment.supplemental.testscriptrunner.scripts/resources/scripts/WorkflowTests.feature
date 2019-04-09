Feature: WorkflowTests


@Workflow01
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

  
@WfRobustness01
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


@WfRobustness02
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

  
@WfRobustness03
Scenario: Remote node restart during distributed workflow with local controller

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
  And   scheduling a restart of "NodeB" after 5 seconds
  And   executing workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "NodeA"

  Then  the workflow controller should have been "NodeA"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  # The workflow should have failed within a reasonable time (instead of waiting indefinitely)
  And   the workflow should have reached the FAILED state
  # Also, NodeA should have received and logged an exception stating that the remote node restart was specifically detected
  And   the log output of "NodeA" should contain the pattern "(?:has been restarted|the remote node was restarted)"

  # Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
  When  waiting for 1 second
