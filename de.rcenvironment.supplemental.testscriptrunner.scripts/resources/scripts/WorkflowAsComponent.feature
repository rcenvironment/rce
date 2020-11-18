Feature: WorkflowAsComponent

@WorkflowIntegrationOnCommandline
Scenario: Workflow can be integrated as component via console command
	Given running instance "Instance1" using the default build
	When  integrating workflow "IntegerIncrement.wf" as component "IntegerIncrementComponent" on instance "Instance1" with the following endpoint definitions:
	      | Script:input:input |
	      | Script:output:output |
	Then  the log output of instance "Instance1" should not contain any error
  And   instance "Instance1" should see the component "workflow/IntegerIncrementComponent"
  And   that component should have a static input with name "input"
  And   that input should have the default data type "Integer"
  And 	that input should have the input handling "Queue"
  And   that input should have the execution constraint "Required"
  And   that component should have a static output with name "output"
  And   that output should have the default data type "Integer"
    
@WorkflowIntegrationViaFolder
Scenario: Workflow can be integrated as component by dropping integration folder
	Given running instance "Instance1" using the default build
	When  adding tool "workflow/IntegerIncrement" to "Instance1"
	Then  the log output of instance "Instance1" should not contain any error
  And   instance "Instance1" should see these components:
        | Instance1 | workflow/IntegerIncrement   | local         |
        
@LocalExecutionOfIntegratedWorkflows
Scenario: Workflows containing integrated workflows can be executed locally
	Given running instance "Instance1" using the default build
	When 	adding tool "workflow/IntegerIncrement" to "Instance1"
	And   integrating workflow "stringAppender.wf" as component "StringAppenderComponent" on instance "Instance1" with the following endpoint definitions:
				| Script:input:inputString |
				| Script:output:outputString |
	And   executing the workflow "incrementAndAppend.wf" on "Instance1"
	Then  the log output of instance "Instance1" should not contain any error
	
@RemoteExecutionOfIntegratedWorkflows
Scenario: Components backed by workflows can be executed remotely even if they contain components that are not accessible to the workflow controller
  Given instances "DataSource,Controller,ComputeInterface,ComputeWorker,DataSink" using the default build
  And   configured network connections "DataSource->Controller [autoStart relay]"
  And   configured network connections "ComputeInterface->Controller [autoStart relay],ComputeWorker->ComputeInterface [autoStart]"
  And   configured network connections "DataSink->Controller [autoStart relay]"
  And   starting all instances
  And   integrating workflow "DataSource.wf" as component "DataSource" on instance "DataSource" with the following endpoint definitions:
  			| InputProvider:output:inputData |
  And   executing command "components set-auth workflow/DataSource public" on "DataSource"
  And   adding tool "common/ProcessorImpl" to "ComputeWorker"
  And   executing command "auth import GroupA:0123456789abcdef:1:cVWfx4BDLnxXCsrRqO-9PzuNXOFIC09uoVCtaA4ThAU" on "ComputeInterface"
  And   executing command "auth import GroupA:0123456789abcdef:1:cVWfx4BDLnxXCsrRqO-9PzuNXOFIC09uoVCtaA4ThAU" on "ComputeWorker"
  And   executing command "components set-auth common/ProcessorImpl GroupA" on "ComputeWorker"
  And   waiting for 1 second
  And   integrating workflow "ProcessorInterface.wf" as component "DataProcessor" on instance "ComputeInterface" with the following endpoint definitions:
        | Processor:inputData:inputData |
        | Processor:resultData:resultData |
  And   executing command "components set-auth workflow/DataProcessor public" on "ComputeInterface"
  And   integrating workflow "DataSink.wf" as component "DataSink" on instance "DataSink" with the following endpoint definitions:
  			| OutputWriter:input:resultData |
  And   executing command "components set-auth workflow/DataSink public" on "DataSink"
  And   waiting for 1 second
  When  executing the workflow "DataProcessing.wf" on "Controller"
  Then  the log output of all instances should not contain any errors