Feature: InstanceManagement


@AllCommandsPresent
@DefaultTestSuite
@NoGUITestSuite
Scenario Outline: Verifying accessibility of console command <command> for role "developer"

  Given running instance "Node1" using the default build with im master role "developer"
  When  executing the command "<command>" on "Node1"
  # Assert that no developer-facing error message is shown
  Then  the output should not contain "Unknown command"
  # Assert that no user-facing error message is shown
  And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
  
  Examples:
  	|command|
  	|auth|
  	|auth create|
  	|auth delete|
  	|auth export|
  	|auth import|
  	|auth list|
  	|cn|
  	|cn add|
  	|cn list|
  	|cn start|
  	|cn stop|
  	|components|
  	|components list|
  	|components list-auth|
  	|components set-auth|
  	|help|
  	|keytool ssh-pw|
  	|keytool uplink-pw|
  	|mail|
  	|net|
  	|net filter|
  	|net filter reload|
  	|net info|
  	|ra-admin list-wfs|
  	|ra-admin publish-wf|
  	|ra-admin unpublish-wf|
  	|ssh|
  	|ssh add|
  	|ssh list|
  	|ssh start|
  	|ssh stop|
  	|sysmon api avgcpu+ram 4 100|
  	|sysmon local|
  	|sysmon -l|
  	|sysmon remote|
  	|sysmon -r|
  	|uplink|
  	|uplink add|
  	|uplink list|
  	|uplink start|
  	|uplink stop|
  	|version|
  	|wf|
  	|wf cancel|
  	|wf delete|
  	|wf details|
  	|wf integrate|
  	|wf list|
  	|wf open|
  	|wf pause|
  	|wf resume|
  	|wf run|
  	|wf verify|