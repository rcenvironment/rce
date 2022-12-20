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
    

# The following does the same tests, looks ugly, but is 6 times faster:
@AltAllCommandsPresent
@DefaultTestSuite
@NoGUITestSuite
Scenario: Alternative approach for verifying accessibility of console commands for role "developer"

    Given running instance "Node3" using the default build with im master role "developer"
    When  executing the command "auth" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "auth create" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "auth delete" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "auth export" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "auth import" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "auth list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "cn" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "cn add" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "cn list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "cn start" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "cn stop" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "components" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "components list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "components list-auth" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "components set-auth" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "help" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "keytool ssh-pw" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "keytool uplink-pw" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "mail" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "net" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "net filter" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "net filter reload" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "net info" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ra-admin list-wsf" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ra-admin publish-wf" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ra-admin unpublish-wf" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ssh" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ssh list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ssh start" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "ssh stop" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "sysmon api avgcpu+ram 4 100" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "sysmon local" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "sysmon -l" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "sysmon remote" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "sysmon -r" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "uplink" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "uplink add" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "uplink list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "uplink start" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "uplink stop" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "version" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf cancel" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf delete" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf details" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf integrate" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf list" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf open" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf pause" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf resume" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf run" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    When  executing the command "wf verify" on "Node3"
    # Assert that no developer-facing error message is shown
    Then  the output should not contain "Unknown command"
    # Assert that no user-facing error message is shown
    And   the output should not contain "not executed. You either do not have the privileges to execute this command or it does not exist."
    
