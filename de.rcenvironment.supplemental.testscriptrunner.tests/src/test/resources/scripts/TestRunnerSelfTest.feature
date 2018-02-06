Feature: TestRunnerSelfTest

Scenario: Basic Setup Test

  Given the initial state
  Then the test flag should be false
  And the default build id should be "dummyBuild"
  
  Given the flag was initialized
  Then the test flag should be true
