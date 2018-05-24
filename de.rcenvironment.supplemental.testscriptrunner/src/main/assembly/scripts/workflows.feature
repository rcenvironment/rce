Feature: Workflow Testing

Background:

#	Given a SSH connection to ${ssh.host} on port ${ssh.port} with login "${ssh.login}" and password "${ssh.password}"
	Given a SSH connection to 127.0.0.1 on port 31005 with default login
		

Scenario: wfSelfTest
	
	When  executing the command "wf self-test"
	Then  the output should contain "Verification SUCCEEDED"
	