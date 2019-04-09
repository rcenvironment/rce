Feature: InstanceManagement


@Test01
@DefaultTestSuite
Scenario: Basic multi-instance handling and command execution

  Given instances "NodeA, NodeB" using the default build
  Then  instances "NodeA, NodeB" should be stopped

  When  starting all instances
  Then  instances "NodeA, NodeB" should be running
  And   the visible network of "NodeA" should consist of "NodeA"
  And   the visible network of "NodeB" should consist of "NodeB"
  
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors


@Test02
Scenario: Basic networking between three instances (auto-start connections, no relay flag)

  Given instances "NodeA, NodeB, NodeC" using the default build
  And   configured network connections "NodeA->NodeC [autoStart], NodeB->NodeC [autoStart]"

  When  starting all instances
  Then  all auto-start network connections should be ready within 20 seconds 
  And   the visible network of "NodeA" should consist of "NodeA, NodeC"
  And   the visible network of "NodeB" should consist of "NodeB, NodeC"

  # TODO reconfigure NodeC as relay and restart it


@Test03
@DefaultTestSuite
Scenario: Executing "wf self-test"

  # note: this test frequently fails when testing 8.1.0 or 8.1.1 as the SSH timeout was accidentally set too low in these releases  

  Given the running instance "NodeA" using the default build

  When  executing the command "wf self-test" on "NodeA"
  Then  the output should contain "Verification SUCCEEDED"


@Test04
@DefaultTestSuite
Scenario: Concurrent headless instance starting and stopping

  Given instances "Node1, Node2, Node3, Node4, Node5" using the default build
  When  starting all instances concurrently
  Then  instances "Node1, Node2, Node3, Node4, Node5" should be running
  
  When  stopping all instances concurrently
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors


@Test05
Scenario: Concurrent GUI instance starting and stopping

  Given instances "Node1, Node2, Node3, Node4, Node5" using the default build
  When  starting all instances concurrently in GUI mode
  Then  instances "Node1, Node2, Node3, Node4, Node5" should be running
  
  When  stopping all instances concurrently
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors


@Test06
@DefaultTestSuite
Scenario: Frequent single-instance headless start/stop

  Given instance "Node1" using the default build

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors


@Test07
Scenario: Frequent single-instance GUI start/stop

  Given instance "Node1" using the default build

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no unexpected warnings or errors

