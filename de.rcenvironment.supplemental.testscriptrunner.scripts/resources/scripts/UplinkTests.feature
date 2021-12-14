Feature: UplinkTests

@Uplink01
Scenario: Simple check of uplink connections of two client and two uplink server instances

    Given instance "Uplink1, Uplink2, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client1-[upl]->Uplink2"
    And configured network connections "Client2-[upl]->Uplink2 [autoStart autoRetry]"
    
    When starting instances "Uplink1, Uplink2"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    Then the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client1" should contain "Uplink2"
    And the visible uplink network of "Client1" should not be connected to "Uplink2"
    And the visible uplink network of "Client2" should contain "Uplink2"
    And the visible uplink network of "Client2" should be connected to "Uplink2"
    And the visible uplink network of "Client2" should not contain "Uplink1"
    And the visible uplink network of "Client2" should not be connected to "Uplink1"
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instances "Uplink1, Uplink2"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instances "Uplink1, Uplink2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink02
Scenario: Check of uplink autoconnect after restart of Client1 (provides tool for Client2)

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds

    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client1"
    #And waiting for 15 seconds
    And  instance "Client1" should be stopped
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And starting instance "Client1"
    And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink02c
Scenario: Check of uplink autoconnect after crash and restart of Client1 (provides tool for Client2)

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"

    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds

    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When instance "Client1" crashes
    And waiting for 15 seconds
    # TODO: we have to define a step which verifies that Client1 is really down
    #And  instance "Client1" should be stopped
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    # This check at this position should fail, TODO: define a step for that the component can NOT be seen
    #And instance "Client2" should see these components:
    #    | Client1 (via userName/Client1_) | common/TestTool | local |
    And starting instance "Client1"
    And waiting for 60 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    """
    and client ID "Client1_" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And the log output of instances "Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    as it is already in use by session
    """

@Uplink03
Scenario: Check of uplink autoconnect after restart of Client2 (uses tool from Client1)

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds

    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client2"
    #And waiting for 15 seconds
    And  instance "Client2" should be stopped
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And starting instance "Client2"

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink04
Scenario: Check of uplink autoconnect after shutdown and restart of uplink server

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Uplink1"
    And waiting for 15 seconds
    And  instances "Uplink1" should be stopped
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should not be connected to "Uplink1"
    And waiting for 15 seconds
    # TODO: Clarify if the number of warnings is constant; and, the step is buggy (always shows 1 warning) and need to be fixed
    #And the log output of instance "Client1" should contain 2 warnings
    And the log output of instance "Client1" should contain "An Uplink connection (Uplink1_userName) finished with a warning or error"
    And the log output of instance "Client1" should contain "java.net.ConnectException: Connection refused"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should not be connected to "Uplink1"
    # TODO: Clarify if the number of warnings is constant; and, the step is buggy (always shows 1 warning) and need to be fixed
    #And the log output of instance "Client2" should contain 2 warnings
    And the log output of instance "Client2" should contain "An Uplink connection (Uplink1_userName) finished with a warning or error"
    And the log output of instance "Client2" should contain "java.net.ConnectException: Connection refused"

    And starting instance "Uplink1"
    And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instance "Client1"
    And waiting for 5 seconds
    And stopping instance "Client2"
    And waiting for 5 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    An Uplink connection (Uplink1_userName) finished with a warning or error
    java.net.ConnectException: Connection refused
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

# This combines the previous restart options. It is meant to be used in the context of automated
# regression testing in order to save time (compared with running each case independently).

@Uplink04c
Scenario: Check of uplink autoconnect after crash and restart of uplink server

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"

    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When instance "Uplink1" crashes
    And waiting for 15 seconds
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should not be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should not be connected to "Uplink1"
    And starting instance "Uplink1"
    And waiting for 60 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    java.net.ConnectException: Connection refused
    finished with a warning or error; inspect the log output above for details
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink02-04
Scenario: Combined check of uplink autoconnect after shutdown and restart of clients and uplink server

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client1"
    #And waiting for 15 seconds
    And  instance "Client1" should be stopped
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And starting instance "Client1"
    #And waiting for 30 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    When stopping instance "Client2"
    #And waiting for 15 seconds
    And  instance "Client2" should be stopped
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And starting instance "Client2"

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    When stopping instance "Uplink1"
    #And waiting for 15 seconds
    And  instances "Uplink1" should be stopped
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should not be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should not be connected to "Uplink1"
    And starting instance "Uplink1"

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    And stopping instances "Client1, Client2"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink05
Scenario: Autoconnect after startup with uplink server started before clients
    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"

    When starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    
    Then the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    # Client1 is shut down first, otherwise we sometimes get an error like this:
    # ERROR - de.rcenvironment.toolkit.modules.concurrency.internal.AsyncOrderedCallbackManagerImpl$InternalAsyncOrderedCallbackQueue - Error in asynchronous callback; shutting down queue (as defined by exception policy);
    # java.lang.IllegalStateException: Service not available: de.rcenvironment.core.component.management.api.LocalComponentRegistrationService ...
    And stopping instance "Client1"
    And waiting for 5 seconds
    And stopping instance "Client2"
    And waiting for 5 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 5 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink06
Scenario: Autoconnect after startup with clients started before uplink sever
    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"

    When starting instances "Client1, Client2"
    And waiting for 15 seconds
    And starting instance "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
    
    Then the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    And stopping instance "Client1"
    And waiting for 5 seconds
    And stopping instance "Client2"
    And waiting for 5 seconds
    And stopping instance "Uplink1"
    And waiting for 15 seconds
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    java.net.ConnectException: Connection refused
    """
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink07
Scenario: Check of disonnect and connect of clients in an uplink connection

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    And configured network connections "Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instances "Client1, Client2"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"

    And adding tool "common/TestTool" to "Client1"
    And waiting for 15 seconds
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 45 seconds
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When executing command "uplink stop Uplink1_userName" on "Client1"
    And waiting for 15 seconds
    And the visible uplink network of "Client1" should contain "Uplink1"
    And the visible uplink network of "Client1" should not be connected to "Uplink1"
    And executing command "uplink start Uplink1_userName" on "Client1"
    And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds

    When executing command "uplink stop Uplink1_userName" on "Client2"
    And waiting for 15 seconds
    And the visible uplink network of "Client2" should contain "Uplink1"
    And the visible uplink network of "Client2" should not be connected to "Uplink1"
    And executing command "uplink start Uplink1_userName" on "Client2"
    And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"
    And the visible uplink network of "Client2" should be connected to "Uplink1"
    And instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    And waiting for 15 seconds
    # TODO: The Uplink1 instance has some warnings, has to be investigated with MANTIS #17659
    #And the log output of all instances should not contain any warning

    And stopping instance "Client1"
    And waiting for 5 seconds
    And stopping instance "Client2"
    And waiting for 5 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    """

@Uplink08
Scenario: Two clients with same ID access the uplink server after startup and dis- and reconnecting, uplink server rejects the second. 

    Given instances "Uplink1, Client3a, Client3b" using the default build
    And configured cloned network connections "Client3a-[upl]->Uplink1 [autoStart autoRetry], Client3b-[upl]->Uplink1 [autoStart autoRetry]"


    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds

    And the visible uplink network of "Client3a" should be connected to "Uplink1"
    And starting instance "Client3b"
    And waiting for 15 seconds
    And the visible uplink network of "Client3b" should not be connected to "Uplink1"
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When executing command "uplink stop Uplink1_userName" on "Client3a"
    And waiting for 15 seconds
    And executing command "uplink start Uplink1_userName" on "Client3b"
    And waiting for 15 seconds
    And executing command "uplink start Uplink1_userName" on "Client3a"
    And waiting for 15 seconds

    Then the visible uplink network of "Client3a" should not be connected to "Uplink1"
    And the visible uplink network of "Client3b" should be connected to "Uplink1"
    And the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    And stopping instances "Client3a, Client3b"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    and client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    from using namespace userNameClient3a as it is already in use
    """

@Uplink09
Scenario: Two clients with same ID access the uplink server after startup and repeated stop/restart, uplink server rejects the second. 

    Given instances "Uplink1, Client3a, Client3b" using the default build
    And configured cloned network connections "Client3a-[upl]->Uplink1 [autoStart autoRetry], Client3b-[upl]->Uplink1 [autoStart autoRetry]"

    And starting instances "Uplink1"
    And waiting for 15 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds

    And the visible uplink network of "Client3a" should be connected to "Uplink1"
    And starting instance "Client3b"
    And waiting for 15 seconds
    And the visible uplink network of "Client3b" should not be connected to "Uplink1"
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When stopping instances "Client3a, Client3b"
    And waiting for 15 seconds
    And starting instance "Client3b"
    And waiting for 45 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds

    And the visible uplink network of "Client3b" should be connected to "Uplink1"
    Then the visible uplink network of "Client3a" should not be connected to "Uplink1"
    And the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    When stopping instances "Client3a, Client3b"
    And waiting for 15 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds
    And starting instance "Client3b"
    And waiting for 15 seconds

    Then the visible uplink network of "Client3a" should be connected to "Uplink1"
    And the visible uplink network of "Client3b" should not be connected to "Uplink1"
    And the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    And stopping instances "Client3a, Client3b"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    and client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    from using namespace userNameClient3a as it is already in use
    """

@Uplink10
Scenario: Two clients with same ID access the uplink server after startup and repeated crash/restart, uplink server rejects the second. 

    Given instances "Uplink1, Client3a, Client3b" using the default build
    And configured cloned network connections "Client3a-[upl]->Uplink1 [autoStart autoRetry], Client3b-[upl]->Uplink1 [autoStart autoRetry]"

    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds

    And the visible uplink network of "Client3a" should be connected to "Uplink1"
    And starting instance "Client3b"
    And waiting for 15 seconds
    And the visible uplink network of "Client3b" should not be connected to "Uplink1"
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When instance "Client3a" crashes
    And waiting for 15 seconds
    And executing command "uplink start Uplink1_userName" on "Client3b"
    # We need sufficient time: after crash some clean-up work is being performed before Client3b can connect
    And waiting for 60 seconds
    And starting instance "Client3a"
    And waiting for 15 seconds

    Then the visible uplink network of "Client3a" should not be connected to "Uplink1"
    And the visible uplink network of "Client3b" should be connected to "Uplink1"
    And the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    When instance "Client3b" crashes
    And waiting for 15 seconds
    And executing command "uplink start Uplink1_userName" on "Client3a"
    # We need sufficient time: after crash some clean-up work is being performed before Client3a can connect
    And waiting for 60 seconds
    And starting instance "Client3b"
    And waiting for 15 seconds

    Then the visible uplink network of "Client3a" should be connected to "Uplink1"
    And the visible uplink network of "Client3b" should not be connected to "Uplink1"
    And the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    And stopping instances "Client3a, Client3b"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    and client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    from using namespace userNameClient3a as it is already in use
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    """

@Uplink11
Scenario: After crash, the same client connects again to the uplink server (s. Mantis #17415).

    Given instances "Uplink1, Client1" using the default build
    And configured network connection "Client1-[upl]->Uplink1 [autoStart autoRetry]"

    And starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instance "Client1"
    And waiting for 15 seconds

    And the visible uplink network of "Client1" should be connected to "Uplink1"

    When instance "Client1" crashes
    And waiting for 15 seconds
    And starting instance "Client1"
    # it is essential here to give enough time to wait for the automatic reconnect
    And waiting for 45 seconds

    Then the visible uplink network of "Client1" should be connected to "Uplink1"

    And stopping instance "Client1"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    And the log output of instances "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    """
    and client ID "Client1_" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And waiting for 15 seconds
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    from using namespace userNameClient1_ as it is already in use
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    )
    """
# The following scenario is meant as demonstrative example to allow certain warnings at shutdown.
@Uplink003
Scenario: Check of allowed warnings after shutdown

    Given instance "Uplink1, Client1" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry]"
    
    When starting instance "Uplink1"
    And waiting for 15 seconds
    And starting instance "Client1"
    And waiting for 15 seconds
    And the visible uplink network of "Client1" should be connected to "Uplink1"
    And waiting for 15 seconds

    And stopping instance "Client1"
    And waiting for 15 seconds
    And stopping instance "Uplink1"
    
    Then the log output of instance "Client1" should indicate a clean shutdown with no warnings or errors
    #Then the log output of instance "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    #    """
    #    finished with a warning or error
    #    java.net.ConnectException: Connection refused
    #    """
    
    #And the log output of all instances should indicate a clean shutdown with no warnings or errors
    And the log output of instance "Uplink1" should indicate a clean shutdown with these allowed warnings or errors: 
        """
        stream is already closed
        as it exceeds the significant character limit (8)
        Unregistered session or session already closed
        Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
        """
    And the log output of instance "Uplink1" should contain 2 warnings

