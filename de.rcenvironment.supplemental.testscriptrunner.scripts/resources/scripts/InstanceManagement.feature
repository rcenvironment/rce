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
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


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
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@Test05
Scenario: Concurrent GUI instance starting and stopping

  Given instances "Node1, Node2, Node3, Node4, Node5" using the default build
  When  starting all instances concurrently in GUI mode
  Then  instances "Node1, Node2, Node3, Node4, Node5" should be running
  
  When  stopping all instances concurrently
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@Test06
@DefaultTestSuite
Scenario: Frequent single-instance headless start/stop

  Given instance "Node1" using the default build

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@Test07
Scenario: Frequent single-instance GUI start/stop

  Given instance "Node1" using the default build

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors

  When  starting all instances in GUI mode
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors
  
  
@Test08
Scenario Outline: Starting instance with specific command <command>

  Given instance "Node1" using the default build

  When  starting all instances with console command <command>
  And 	stopping all instances
  
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors
  
  Examples:
    |command														| 
    |--headless														|
    |--exec "dummy"													|
    |--batch "dummy"												|
    |--disable-components											|
    |--upgrade-profile												|
    |--use-default-workspace										|
    |--showAdvancedTab												|
    |-data @noDefault												|
    |-clean															|
    #these fail currently, still to determine if error in code or test
    |-vmargs=-Xm1024m												|
    |-Drce.network.overrideNodeId=a96db8fa762d59f2d2782f3e5e9662d4	|
    |-Dcommunication.uploadBlockSize=131072							|
    

#Test currently not possible, because Program exits upon error and Test is not continued
#@Test09
#Scenario Outline: Starting instances with specific command <command> expecting exception during startUp  
#
#  Given instance "Node1" using the default build
#
#  When  starting all instances in GUI mode with console command <command>
#  
#  Then  the startUp debugLog should contain <exceptionMessage> #step does not exist yet
#   
#  Examples:
#    |command			|exceptionMessage| 
#    |--profile			|"Invalid combination of command-line parameters: cannot specify the same parameter several times"|
#    |--profile "test"	|"Invalid combination of command-line parameters: cannot specify the same parameter several times"|
  
@Test10
Scenario Outline: Starting instances with specific command <command> and input  

  Given instance "Node1" using the default build

  When  starting all instances with console command <command>
  And   closing the configureUI
  And 	stopping all instances
  
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors
  
  Examples:
  	|command	|
  	|--configure|
  	
@Test11
@SSHTestSuite
Scenario: Configuring standard uplink setup

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1"
    And connect "Client2" via uplink to ssh server "Uplink1"
    
    When starting instances in the following order "Uplink1, Client1, Client2"
    And stopping instances in the following order "Client2, Client1, Uplink1"
    
    Then the log output of all instances should indicate a clean shutdown with no warnings or errors
    
@Test12    
@Test12a
@SSHTestSuite
Scenario: Accessing Tool through uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1" with autoRetry enabled
    And connect "Client2" via uplink to ssh server "Uplink1" with autoRetry enabled
    
    When starting all instances
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    
    Then "Client2" has access to testtool

@Test12 
@Test12b
@SSHTestSuite
Scenario: Accessing Tool through uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1" with autoRetry enabled
    And connect "Client2" via uplink to ssh server "Uplink1" with autoRetry enabled

    When starting instances in the following order "Client1"
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And starting instances in the following order "Uplink1, Client2"
    And waiting for 2 seconds
    
    Then "Client2" has access to testtool
    
@Test13
@SSHTestSuite
Scenario: Tool inaccessible after uplink shutdown

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1"
    And connect "Client2" via uplink to ssh server "Uplink1"
    
    When starting instances in the following order "Uplink1, Client1, Client2"
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And scheduling an instance shutdown of "Uplink1" after 1 second
    And waiting for 1 second
    
    Then "Client2" has no access to testtool
    
#TODO work out other method to test connection establishment than checking visibility of a tool    
@Test14
@SSHTestSuite
Scenario: Connection established with autoRetry

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1" with autoRetry enabled
    And connect "Client2" via uplink to ssh server "Uplink1" with autoRetry enabled
    
    When starting instances in the following order "Client1, Client2, Uplink1"
    #waiting to ensure they are connected by auto-retry
    And waiting for 5 seconds
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    
    Then "Client2" has access to testtool
    
@Test15
@SSHTestSuite
Scenario: Connection established after restart

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1" with autoRetry enabled
    And connect "Client2" via uplink to ssh server "Uplink1" with autoRetry enabled
    
    When starting all instances
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And scheduling an instance restart of "Uplink1" after 1 seconds
    #TODO replace with dynamic wait until restart has finishes - has to be implemented
    And waiting for 20 seconds
     
    Then "Client2" has access to testtool
   
@Test16
@SSHTestSuite
Scenario: Running Workflow with remote tool, published via uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configuring "Uplink1" as ssh server
    And connect "Client1" via uplink to ssh server "Uplink1"
    And connect "Client2" via uplink to ssh server "Uplink1"
    
    When starting instances in the following order "Uplink1, Client1, Client2"
    And adding testtool to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"  
    And executing workflow "UplinkRemoteWorkflow.wf" on "Client2"
    And wait until all workflows on "Client2" are finished or 120 seconds have passed
    
    Then the workflow controller should have been "Client2"
    And workflow component "Optimizer" should have been run on "Client2"
    And workflow component "TestTool" should have been run on "Client1" via uplink


