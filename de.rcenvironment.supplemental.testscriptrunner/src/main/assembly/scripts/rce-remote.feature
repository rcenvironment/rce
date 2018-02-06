Feature: Remote Access Standalone Client


Scenario: listToolsAndExpectSingleTestTool

	When  calling rce-remote with parameters
		"""
		-h ${testserver.host} -p ${testserver.port} list-tools
		"""
	Then  the exit code should be 0
	And   the standard output should be 2 lines long
	And   the standard output should contain
		"""
		testTool${list-tools-col-separator}1.0${list-tools-col-separator}${testserver.nodeId}:0${list-tools-col-separator}
		"""
	And   the standard output should contain
		"""
		valid name test${list-tools-col-separator}1
		"""
	And   the error output should be empty


Scenario Outline: Password file success cases

	Given a password file "pwfile.tmp" containing "<pwfileContent>"
	When  calling rce-remote with parameters
		"""
		-h ${testserver.host} -p ${testserver.port} -u pwtest -f "${workdir}${/}pwfile.tmp" protocol-version
		"""
	Then  the exit code should be 0
	And   the standard output should be 1 line long
	And   the standard output should contain "${protocol.version.expected}"
	And   the error output should be empty
	
	Examples:
		| pwfileContent	         |
		| ${actual-pw}           |
		| ${actual-pw}${LF}      |
		
	@windows-only
	Examples:
		| pwfileContent	         |
		| ${actual-pw}${CR}${LF} | 
	  
	
Scenario Outline: Password file error cases

	Given a password file "pwfile.tmp" containing "<pwfileContent>"
	When  calling rce-remote with parameters
		"""
		-h ${testserver.host} -p ${testserver.port} -u pwtest -f "${workdir}/pwfile.tmp" protocol-version
		"""
	Then  the exit code should be <exitCode>
	And   the standard output should be <stdout.linecount> lines long
	And   the error output should be <stderr.linecount> lines long
	And   the error output should contain "<stderr.expectedtext>"
	
	Examples:
		| pwfileContent	                | exitCode | stdout.linecount | stderr.linecount | stderr.expectedtext      |
		| wrongPW                       | 1        | 0                | 1                | Authentication failure   |
		|                               | 1        | 0                | 1                | Authentication failure   |
		| ${actual-pw}${LF}Garbage      | 2        | 0                | 1                | additional content after |
		
	@windows-only
	Examples:
		| pwfileContent	                | exitCode | stdout.linecount | stderr.linecount | stderr.expectedtext      |
		| ${actual-pw}${CR}${LF}Garbage | 2        | 0                | 1                | additional content after |
		
		
Scenario: Missing password file

	Given no password file at "pwfile.tmp"
	When  calling rce-remote with parameters
		"""
		-h ${testserver.host} -p ${testserver.port} -u pwtest -f "${workdir}${/}pwfile.tmp" protocol-version
		"""
	Then  the exit code should be 2
	And   the standard output should be empty
	And   the error output should be 1 line long
	And   the error output should contain "The specified password file does not exist"
