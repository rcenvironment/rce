Feature: Basic SSH Command Testing

# NOTE: these tests are "work in progress" and only committed as reference.
#       they may or may not run at this point! - misc_ro

Background:

	#Given a SSH connection to ${ssh.host} on port ${ssh.port} with login "${ssh.login}" and password "${ssh.password}"
	Given a SSH connection to localhost on port 31055 with login "im_master" and password "ra_demo"
	
Scenario: basicCommandExecution

	When  executing the command "dummy"
	Then  the output should contain "Dummy command executing"
	And   the output should be 1 line long
	
Scenario: verifyHelpOutput
	
	When  executing the command "help"
	Then  the output should contain "RCE Console Commands:"
	And   the output should contain "net filter"
	And   the output should contain "saveto"
	And   the output should contain "wf run"
	
Scenario: verifyNetCommands

	When  executing the command "net"
	Then  the output should contain "Reachable network nodes (1 total)"
	And   the output should contain "Integration Test Server"
	

Scenario: runWorkflows
	
	When  executing the command "wf list"
	Then  the output should contain "TOTAL COUNT: 0 workflow(s): 0 running, 0 paused, 0 finished, 0 cancelled, 0 failed, 0 other"
	When  executing the command "wf run doesnotexist.wf"
	And   executing the command "wf list"
	Then  the output should contain "TOTAL COUNT: 0 workflow(s): 0 running, 0 paused, 0 finished, 0 cancelled, 0 failed, 0 other"
	When  executing the command "wf run C:\Users\misc_ro\.rce\default\workspace\test\test.wf"
	And   executing the command "wf list"
	Then  the output should contain "TOTAL COUNT: 0 workflow(s): 0 running, 0 paused, 1 finished, 0 cancelled, 0 failed, 0 other"

Scenario: Client/Server setup via IM, not connected to each other

	When executing the command "im configure server --set-name TestServer --enable-im-ssh-access 22222" (expecting "Updated the configuration")
	And  executing the command "im configure client --set-name TestClient --enable-im-ssh-access 22223" (expecting "Updated the configuration")
	And  executing the command "im stop client,server"
	And  executing the command "im start server trunk"
	And  the output should contain "started instance server"
	And  executing the command "im start client trunk"
	Then the output should contain "started instance client"
	
	# temporary fix until output parsing for versions <7.1.0 is added
	When waiting for 5 seconds   

	When executing the command "im execute-on client \"net\""
	Then the output should contain "nodes (1 total)"
	And  the output should contain "TestClient"

	When executing the command "im execute-on server \"net\""
	Then the output should contain "nodes (1 total)"
	And  the output should contain "TestServer"

	When executing the command "im stop client,server"
	Then the output should contain "stopped instance client"
	And  the output should contain "stopped instance server"

@single
Scenario: Client/Server setup via IM, connected to each other

	When executing the command "im stop client,server"
	And  executing the command "im configure server --reset --set-name TestServer --enable-im-ssh-access 22222 --add-server-port port_1 localhost 31999" (expecting "Updated the configuration")
	And  executing the command "im configure client --reset --set-name TestClient --enable-im-ssh-access 22223 --add-connection conn_1 localhost 31999 true" (expecting "Updated the configuration")
	And  executing the command "im start server trunk"
	Then the output should contain "started instance server"
	When executing the command "im start client trunk"
	Then the output should contain "started instance client"
	
	# temporary fix until output parsing for versions <7.1.0 is added
	When waiting for 10 seconds   

	When executing the command "im execute-on client \"net\""
	Then the output should contain "nodes (2 total)"
	And  the output should contain "TestClient"
	And  the output should contain "TestServer"
	
	When executing the command "im execute-on server \"net\""
	Then the output should contain "nodes (2 total)"
	And  the output should contain "TestClient"
	And  the output should contain "TestServer"

	When executing the command "im stop client,server"
	Then the output should contain "stopped instance client"
	And  the output should contain "stopped instance server"


Scenario: Temp/Disabled

	When executing the command "im configure server --add-server-port main localhost 31999 --allow-ssh-commands-via-im \"net\" "
	And  executing the command "im configure client --add-connection conn1 localhost 31999 --allow-ssh-commands-via-im \"net\"
	And  executing the command "im start client,server imtest"
	Then the output should contain "Started instance client"
	And  the output should contain "Started instance server"
	
	When waiting for 3 seconds
	And  executing the command "im execute-on client net"
	Then the output should contain "TestClient"
	Then the output should contain "TestServer"
	
	