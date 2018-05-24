Feature: RCE

  Scenario: starting RCE without any parameters
    When calling RCE
    # exit code should be 1 as we cancel the execution as soon as the early startup is completed
    Then the exit code should be 1
    And the standard output should contain
      """
      (use -p/--profile <id or path> to override)
      """

  Scenario: starting RCE with non existing profile folder
    Given a random profile path with placeholder ${profilepath}
    When calling RCE with parameters
      """
      -p ${profilepath}
      """
    Then the exit code should be 1
    And the standard output should not contain
      """
      (use -p/--profile <id or path> to override)
      """
    And the standard output should contain
      """
      (as specified by the -p/--profile option)
      """

  Scenario: starting RCE with locked existing profile folder
    Given a random profile path with placeholder ${profilepath} which is locked
    When calling RCE with parameters
      """
      -p ${profilepath}
      """
    Then the exit code should be 1
    And the standard output should contain
      """
      Using fallback profile directory
      """
    And the standard output should not contain
      """
      ${profilepath}
      """
    And the standard output should contain
      """
      (use -p/--profile <id or path> to override)
      """

  Scenario: starting RCE with locked existing profile folder and exitOnLockedProfile
    Given a random profile path with placeholder ${profilepath} which is locked
    When calling RCE with parameters
      """
      -p ${profilepath} -vmargs -Drce.launch.exitOnLockedProfile
      """
    Then the exit code should be 1
    And the error output should contain
      """
      Failed to lock profile directory ${profilepath} - most likely, another instance is already using it
      """
    And the error output should contain
      """
      Fallback profile is disabled, shutting down.
      """

  Scenario: starting RCE with locked existing profile folder in headless mode
    Given a random profile path with placeholder ${profilepath} which is locked
    When calling RCE with parameters
      """
      -p ${profilepath} --headless
      """
    Then the exit code should be 1
    And the error output should contain
      """
      Failed to lock profile directory ${profilepath} - most likely, another instance is already using it
      """
    And the error output should contain
      """
      Fallback profile is disabled, shutting down.
      """

  #@RCESSHServer TODO use hooks here
  Scenario: checking if RCE was started with the RCE custom launcher
    Given a RCE instance running as SSH server
    Given a SSH connection to 127.0.0.1 on port 31005 with login "ra_demo" and password "ra_demo"
    When executing the command "osgi getprop de.rcenvironment.launcher"
    Then the output should contain
      """
      de.rcenvironment.launcher=de.rcenvironment.launcher
      """
    And shutdown the RCE instance

#  #@RCESSHServer TODO use hooks here
#  Scenario: checking if the path to osgi.install.area is absolute
#    Given a RCE instance running as SSH server
#    Given a SSH connection to 127.0.0.1 on port 31005 with login "ra_demo" and password "ra_demo"
#    When executing the command "osgi getprop osgi.install.area"
#    Then the property osgi.install.area should be an absolute path TODO
#    And shutdown the RCE instance