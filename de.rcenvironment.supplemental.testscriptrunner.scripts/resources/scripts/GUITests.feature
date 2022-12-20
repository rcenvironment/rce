Feature: GUITests
    
@GUITestsFeature
@GUITestSingular
Scenario: For testing purposes 
    
  Given instance "NodeA" using the default build
  
  When  starting all instances in GUI mode
  And   waiting for 2 seconds
  And   executing command "tc close_welcome" on "NodeA"
  And   waiting for 4 seconds
  And   executing command "tc open_view Optimizer" on "NodeA"
  And   waiting for 2 seconds
  And   stopping all instances
  
  Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors
    
@GUITestsFeature
@GUI01
@BasicIntegrationTestSuite
Scenario Outline: Open all views and check for errors

  Given instance "NodeA" using the default build
  
  When  starting all instances in GUI mode
  And   waiting for 2 seconds
  And   executing command "tc close_welcome" on "NodeA"
  And   waiting for 4 seconds
  And   executing command "tc open_view <viewname>" on "NodeA"
  And   waiting for 2 seconds
  And   stopping all instances
  
  Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors
	
	Examples: 
	|viewname  				|
    |Optimizer              |
	|Component_Publishing	|	
	|Cluster_Job_Monitor	|
	|Command_Console		|
	|CPACS_Writer			|
	|Data_Management_Browser|
	|Log					|
    |Network                |
	|Parametric_Study		|
	|Properties				|
	|Timeline				|
	|Workflow_List			|
	|Workflow_Console		|
    |Excel                  |
# excluded TIGL_Viewer view, as this needs a working exe of the tigl viewer, which cannot be shipped with RCE and 
# it can therefore not be ensured that a copy of the tigl viewer is present in the testing environment

#    |TIGL_Viewer            |

@GUITestsFeature
@GUI02
@BasicIntegrationTestSuite
Scenario Outline: Open view, close view and open view and check for errors 

#waiting necessary for command to have an effect. Should be improved to not

  Given instance "NodeA" using the default build
  
  When  starting all instances in GUI mode
  And   waiting for 2 seconds
  And   executing command "tc close_welcome" on "NodeA"
  And   waiting for 4 seconds
  And   executing command "tc open_view <viewname>" on "NodeA"
  And   waiting for 2 seconds
  And   executing command "tc close_view <viewname>" on "NodeA"
  And   waiting for 2 seconds
  And   executing command "tc open_view <viewname>" on "NodeA"
  And   waiting for 2 seconds
  And   stopping all instances
  
  Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors
    
    Examples: 
    |viewname               |
    |Component_Publishing   |
    |Cluster_Job_Monitor    |
    |Command_Console        |
    |CPACS_Writer           |
    |Data_Management_Browser|
    |Log                    |
    |Network                |
    |Optimizer              |
    |Parametric_Study       |
    |Properties             |
    |Timeline               |
    |Workflow_List          |
    |Workflow_Console       |   
    |Excel                  |
# excluded TIGL_Viewer view, as this needs a working exe of the tigl viewer, which cannot be shipped with RCE and 
# it can therefore not be ensured that a copy of the tigl viewer is present in the testing environment
#    |TIGL_Viewer            |
    
    
