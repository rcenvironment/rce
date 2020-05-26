Feature: NetworkingTests

@Network01
@DefaultTestSuite
@NoGUITestSuite
Scenario: Basic multi-instance handling and command execution

  Given instances "NodeA, NodeB" using the default build
  Then  instances "NodeA, NodeB" should be stopped

  When  starting all instances
  Then  instances "NodeA, NodeB" should be running
  And   the visible network of "NodeA" should consist of "NodeA"
  And   the visible network of "NodeB" should consist of "NodeB"
  
  When  stopping all instances
  Then  the log output of all instances should indicate a clean shutdown with no warnings or errors


@Network02
@NoGUITestSuite
Scenario: Basic networking between three instances (auto-start connections, no relay flag)

  Given instances "NodeA, NodeB, NodeC" using the default build
  And   configured network connections "NodeA->NodeC [autoStart], NodeB->NodeC [autoStart]"

  When  starting all instances
  Then  all auto-start network connections should be ready within 20 seconds 
  And   the visible network of "NodeA" should consist of "NodeA, NodeC"
  And   the visible network of "NodeB" should consist of "NodeB, NodeC"

@Network03
@NoGUITestSuite
Scenario: Basic networking between three instances with relay
	
  Given instances "NodeA,NodeB,NodeC" using the default build
  And   configured network connections "NodeA->NodeC [autoStart relay], NodeB->NodeC [autoStart relay]"
  
  When  starting all instances
  Then  all auto-start network connections should be ready within 20 seconds
  And   the visible network of "NodeA" should consist of "NodeA, NodeB, NodeC"
  And   the visible network of "NodeB" should consist of "NodeA, NodeB, NodeC"
  And   the visible network of "NodeC" should consist of "NodeA, NodeB, NodeC"
  	
@Network10
@SSHTestSuite
@NoGUITestSuite
Scenario: Configuring standard uplink setup

    Given instance "Uplink, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink [autoStart autoRetry], Client2-[upl]->Uplink [autoStart autoRetry]"
    
    When starting instances "Uplink, Client1, Client2" in the given order
    And stopping instances "Client1, Client2, Uplink" in the given order
    
    Then the log output of "Uplink" should consist of:
        |Presence|Type|Origin|Message|
        |yes|Error|de.rcenvironment.core.communication.uplink.network.internal.ServerSideUplinkLowLevelProtocolWrapper|Error while receiving a message, closing the connection: java.io.EOFException|
        |yes|Warning|de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionImpl|Truncating client ID 'Client1_Uplink' to 'Client1_' as it exceeds the significant character limit (8)|
        |yes|Warning|de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionImpl|Truncating client ID 'Client2_Uplink' to 'Client2_' as it exceeds the significant character limit (8)|
        |yes|Warning|de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionImpl|Non-protocol error in session SSH User "userName" (will be closed): java.io.EOFException|
         
        
#TODO work out other method to test connection establishment than checking visibility of a tool    
@Network11
@SSHTestSuite
@NoGUITestSuite
Scenario: Connection established with autoRetry

    Given instance "Uplink, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink [autoStart autoRetry], Client2-[upl]->Uplink [autoStart autoRetry]"
    
    When starting instances "Client1, Client2, Uplink" in the given order 
    #waiting to ensure they are connected by auto-retry
    And waiting for 5 seconds
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    
    
@Network12
@SSHTestSuite
@NoGUITestSuite
Scenario: Connection established after restart

    Given instance "Uplink, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink [autoStart autoRetry], Client2-[upl]->Uplink [autoStart autoRetry]"
    
    When starting all instances
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And scheduling an instance restart of "Uplink" after 1 seconds
    #TODO replace with dynamic wait until restart has finishes - has to be implemented
    And waiting for 20 seconds
    
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
        
@Network13
@NoGUITestSuite
Scenario: Connection with other major version - regular connection

    Given instance "NodeA" using the default build
    And   instance "NodeB" using the legacy build
    And   configured network connections "NodeA-[reg]->NodeB, NodeB-[reg]->NodeA"
    
    When  starting all instances
    
    Then  the visible network of "NodeA" should consist of "NodeA"
    And   the visible network of "NodeB" should consist of "NodeB"
    
@Network14
@NoGUITestSuite
Scenario Outline: Connection with other major version - uplink connection

    Given instance "Client1" using the <client1_build> build
    And   instance "Client2" using the <client2_build> build
    And   instance "Uplink" using the <uplink_build> build
    And   configured network connections "Client1-[upl]->Uplink [autoStart autoRetry], Client2-[upl]->Uplink [autoStart autoRetry]"
    
    When  starting all instances
    And   adding tool "common/TestTool" to "Client1"
    And   executing command "components set-auth common/TestTool public" on "Client1"
    And   waiting for 5 seconds
   
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | (absent) |
    
    Examples:
    |client1_build|client2_build|uplink_build|
    |legacy|default|default|
    |legacy|legacy|default|
    |legacy|default|legacy|
    |default|legacy|default|
    |default|default|legacy|
    |default|legacy|legacy|
    
@Network15
@NoGUITestSuite
Scenario: Connection with other minor version - regular connection

    Given instance "NodeA" using the default build
    And   instance "NodeB" using the base build
    And   configured network connections "NodeA-[reg]->NodeB [autoStart], NodeB-[reg]->NodeA [autoStart]"
    
    When  starting all instances
    
    Then  the visible network of "NodeA" should consist of "NodeA, NodeB"
    And   the visible network of "NodeB" should consist of "NodeA, NodeB"
    
@Network16
@NoGUITestSuite
Scenario Outline: Connection with other minor version - uplink connection

    Given instance "Client1" using the <client1_build> build
    And   instance "Client2" using the <client2_build> build
    And   instance "Uplink" using the <uplink_build> build
    And   configured network connections "Client1-[upl]->Uplink [autoStart autoRetry], Client2-[upl]->Uplink [autoStart autoRetry]"
    
    When  starting all instances
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And waiting for 5 seconds
   
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    
    Examples:
    |client1_build|client2_build|uplink_build|
    |base|default|default|
    |base|base|default|
    |base|default|base|
    |default|base|default|
    |default|default|base|
    |default|base|base|
	