Feature: InstanceManagement

@InstanceManagementFeature
@Start01
@Start01a
@DefaultTestSuite
@NoGUITestSuite
Scenario: Concurrent headless instance starting and stopping

  Given instances "Node1, Node2, Node3, Node4, Node5" using the default build
  When  starting all instances concurrently
  Then  instances "Node1, Node2, Node3, Node4, Node5" should be running
  
  When  stopping all instances concurrently
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@InstanceManagementFeature
@Start01
@Start01b
Scenario: Concurrent GUI instance starting and stopping

  Given instances "Node1, Node2, Node3, Node4, Node5" using the default build
  When  starting all instances concurrently in GUI mode
  Then  instances "Node1, Node2, Node3, Node4, Node5" should be running
  
  When  stopping all instances concurrently
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@InstanceManagementFeature
@Start02
@Start02a
@DefaultTestSuite
@NoGUITestSuite
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


@InstanceManagementFeature
@Start02
@Start02b
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
  
  
@InstanceManagementFeature
@Start03
@NoGUITestSuite
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
    #these fail currently, as the commands are not parsed correctly
#    |-vmargs=-Xm1024m												|
#    |-Drce.network.overrideNodeId=a96db8fa762d59f2d2782f3e5e9662d4	|
#    |-Dcommunication.uploadBlockSize=131072							|
    
  
@InstanceManagementFeature
@Start05
@NoGUITestSuite
Scenario Outline: Starting instances with specific command <command> and input  

  Given instance "Node1" using the default build

  When  starting all instances with console command <command>
  And   executing command order <input>
  And 	stopping all instances
  
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors
  
  Examples:
  	|command	|input					|
  	|--configure|"down,down,down,enter"	|



   


