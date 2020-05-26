Feature: ComponentTests

@Comp01
@DefaultTestSuite
@NoGUITestSuite
Scenario: Single-instance component publishing and visibility control

  Given running instance "NodeA" using the default build
  
  # TODO these steps are needed to reset potentially stored settings; a general "wipe profile data" feature would be better for this
  When  executing command "components set-auth rce/Switch local ; components set-auth rce/Joiner local ; components set-auth rce/Database local" on "NodeA"
    
  # temporary workaround for the increased pre-publication time on instance startup; a specific wait command would be better
  When  waiting for 5 seconds
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Switch   | local         |
        # verify that any component with legacy configuration as "public" is still "local"
        | NodeA | rce/Joiner   | local         |
        | NodeA | rce/Database | local-only    |  
        # self-test of "absence" syntax 
        | NodeA | rce/Dummy    | (absent)      |
        | NodeA | common/Dummy | (absent)      |
        
  When  executing command "auth import GroupA:0123456789abcdef:1:cVWfx4BDLnxXCsrRqO-9PzuNXOFIC09uoVCtaA4ThAU" on "NodeA"
  Then  the output should contain "Successfully imported group GroupA"
  
  When  executing command "components set-auth rce/Switch public" on "NodeA"
  Then  the output should contain "Set access authorization"
  
  When  executing command "components set-auth rce/Joiner GroupA" on "NodeA"
  Then  the output should contain "Set access authorization"
  And   instance "NodeA" should see these components:
        | NodeA | rce/Switch   | shared:public                  |
        | NodeA | rce/Joiner   | shared:GroupA:0123456789abcdef |
        | NodeA | rce/Database | local-only                     |

  # check that local-only components remain "local-only", regardless of their authorization settings
  
  When  executing command "components set-auth rce/Database public" on "NodeA"
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Database | local-only               |
  When  executing command "components set-auth rce/Database GroupA" on "NodeA"
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Database | local-only               |
        
  # test persistence after a restart
  
  When  stopping all instances
  And   starting all instances
  And   waiting for 5 seconds
  
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Switch   | shared:public                  |
        | NodeA | rce/Joiner   | shared:GroupA:0123456789abcdef |
        | NodeA | rce/Database | local-only                     |
        
@Comp02
@DefaultTestSuite
@NoGUITestSuite
Scenario: Multi-instance component publishing and visibility control

  Given instance "NodeA" using the default build
  Given instance "NodeB" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  
  When  starting all instances concurrently
  Then  all auto-start network connections should be ready within 20 seconds
  
  # TODO these steps are needed to reset potentially stored settings; a general "wipe profile data" feature would be better for this
  When  executing command "components set-auth rce/Switch local ; components set-auth rce/Joiner local ; components set-auth rce/Database local" on "NodeA"
  And   executing command "auth import GroupA:0123456789abcdef:1:cVWfx4BDLnxXCsrRqO-9PzuNXOFIC09uoVCtaA4ThAU" on "NodeA"
  And   executing command "components set-auth rce/Switch local ; components set-auth rce/Joiner local ; components set-auth rce/Database local" on "NodeB"
  And   executing command "auth import GroupA:0123456789abcdef:1:cVWfx4BDLnxXCsrRqO-9PzuNXOFIC09uoVCtaA4ThAU" on "NodeB"
  And   waiting for 1 second
    
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Joiner           | local         |
        | NodeA | rce/Switch           | local         |
        | NodeA | rce/Database         | local-only    |        
        | NodeB | rce/Joiner           | (absent)      |
        | NodeB | rce/Switch           | (absent)      |
        | NodeB | rce/Database         | (absent)      |        
        
  Then  instance "NodeB" should see these components:
        | NodeA | rce/Joiner           | (absent)      |
        | NodeA | rce/Switch           | (absent)      |
        | NodeA | rce/Database         | (absent)      |     
        | NodeB | rce/Joiner           | local         |
        | NodeB | rce/Switch           | local         |
        | NodeB | rce/Database         | local-only    |

  When  executing command "components set-auth rce/Switch public" on "NodeA"
  Then  the output should contain "public"
  When  executing command "components set-auth rce/Switch GroupA" on "NodeB"
  Then  the output should contain "GroupA"

  When  waiting for 1 second
 
  Then  instance "NodeA" should see these components:
        | NodeA | rce/Joiner           | local                          |
        | NodeA | rce/Switch           | shared:public                  |
        | NodeB | rce/Joiner           | (absent)                       |
        | NodeB | rce/Switch           | remote:GroupA:0123456789abcdef |
        
  Then  instance "NodeB" should see these components:
        | NodeA | rce/Joiner           | (absent)                       |
        | NodeA | rce/Switch           | remote:public                  |
        | NodeB | rce/Joiner           | local                          |
        | NodeB | rce/Switch           | shared:GroupA:0123456789abcdef |

  # TODO add more constellations
  
  
@Comp03   
@Comp03a
@SSHTestSuite
@NoGUITestSuite
Scenario: Accessing Component through uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    When starting all instances
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |

@Comp03 
@Comp03b
@SSHTestSuite
@NoGUITestSuite
Scenario: Accessing Component through uplink

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"

    When starting instance "Client1"
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And starting instances "Uplink1, Client2" in the given order
    And waiting for 2 seconds
    
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | local |
    
@Comp04
@SSHTestSuite
@NoGUITestSuite
Scenario: Component inaccessible after uplink shutdown

    Given instance "Uplink1, Client1, Client2" using the default build
    And configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
    When starting instances "Client1, Client2, Uplink1"
    And adding tool "common/TestTool" to "Client1"
    And executing command "components set-auth common/TestTool public" on "Client1"
    And scheduling an instance shutdown of "Uplink1" after 1 second
    And waiting for 2 second
    
    Then instance "Client2" should see these components:
        | Client1 (via userName/Client1_) | common/TestTool | (absent) |
