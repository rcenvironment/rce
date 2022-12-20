Feature: ExampleWorkflows

# executing this scenario should be possible after the next major update (RCE 11.0), since the base version then has the necessary commands to export wf runs
#@Workflow01 
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

@ExampleWorkflowsFeature
@Workflow02
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario: Execute all example workflows

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf, Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf, Workflow Examples Project/01_First Steps/01_03_Data_Types.wf, Workflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf, Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json], Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf, Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json], Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf, Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_04_Fault-tolerant_Loop.wf, Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf, Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
# the following example workflows are missing in this test scenario:
# workflow 2.3 (Workflow Examples Project\\02_Component Groups\\02_03_XML_Components.wf [02_03_XML_Components.json]) is not tested as it produces errors.
# > the files required by the input provider will not be copied in the working dir at the moment; see Mantis #17767.
# workflow 3.4 Workflow Examples Project\\03_Workflow Logic\\03_04_Fault-tolerant_Loop.wf
# > This wf produces an intentional error that causes the test environment to flag this test "failed". 
# > Alternatively the test performs in suite @Workflow13, or as singel test @ExampleWorklow0304.
# The above described test scenarios has to implemented after release 10.1
# Kathrin Schaffert, 28.04.2020, updated by Matthias Wagner, 20.01.2022
# removed Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf because test get stuck during execution of wf
# might depend on incorrect adding of the Example tool in Step "adding tool "common/Example" to "NodeA"", but has to be checked
# Kathrin Schaffert, 18.06.2020

    Then   the log output of "NodeA" should contain the pattern "(?:de.rcenvironment.core.component.api.ComponentException: Script execution error: Exception: Example failure in <script> at line number 2)"
    And    the log output of "NodeA" should contain 1 error

# Workflow03 is about running all example workflows
# If we will keep the separated Workflow03 instead of Workflow02, we should add the @BasicIntegrationTestSuite here. (K.Schaffert, 16.12.2020)

@Workflow03
@ExampleWorkflow0101
Scenario: Execute example workflow 01_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0102
Scenario: Execute example workflow 01_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0103
Scenario: Execute example workflow 01_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_03_Data_Types.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0104
Scenario: Execute example workflow 01_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf" on "NodeA"
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0201
Scenario: Execute example workflow 02_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json]" on "NodeA"
    And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0202
Scenario: Execute example workflow 02_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf" on "NodeA"
    And   waiting for 15 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

# Currently disabled for suite @Workflow03, see Mantis #17767
#@Workflow03
@ExampleWorkflow0203
Scenario: Execute example workflow 02_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And waiting for 30 seconds
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And copying configuration files "CPACS.xml, MappingRules.xsl, XMLMerger_Integrate.xml" of workflow group "02_Component Groups" into installation workspace
    And waiting for 30 seconds
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_03_XML_Components.wf [02_03_XML_Components.json]" on "NodeA"
    And   waiting for 60 seconds
    And   stopping instance "NodeA"
    And   waiting for 60 seconds
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0204
Scenario: Execute example workflow 02_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json]" on "NodeA"
    And   waiting for 15 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0301
Scenario: Execute example workflow 03_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0302
Scenario: Execute example workflow 03_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0303
Scenario: Execute example workflow 03_03

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0304
Scenario: Execute example workflow 03_04

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_04_Fault-tolerant_Loop.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"

    Then   the log output of "NodeA" should contain the pattern "(?:de.rcenvironment.core.component.api.ComponentException: Script execution error: Exception: Example failure in <script> at line number 2)"
    And    the log output of "NodeA" should contain 1 error

# Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
#When  waiting for 5 second

@Workflow03
@ExampleWorkflow0401
Scenario: Execute example workflow 04_01

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors

@Workflow03
@ExampleWorkflow0402
Scenario: Execute example workflow 04_02

    Given instance "NodeA" using the default build
    
    When  starting instance "NodeA"
    And   adding tool "common/Example" to "NodeA"
    And   copying "workflows/Workflow Examples Project" into workspace of "NodeA"
    And   executing workflows "Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
    #And   waiting for 5 seconds
    And   stopping instance "NodeA"
    Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors