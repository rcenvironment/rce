@echo off

REM *******************************************************************************
REM *
REM * Example batch file for using the SSH Remote Access interface. Parameters 
REM * marked with "###" must be filled in before the script can be run. As a 
REM * prerequisite, the "PuTTY" software must be installed on the local system.
REM * (Download: http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html)
REM *
REM * This script invokes a workflow file that was previously published using the
REM * "ra-admin publish-wf" command. The id that was provided to that command must
REM * be set as the WORKFLOW_ID parameter below. (Note that workflow files must 
REM * meet certain criteria to be successfully published; these are validated by 
REM * the "ra-admin publish-wf" command.)
REM *
REM * Copyright (C) 2014 DLR, Germany
REM * All rights reserved
REM * Author: Robert Mischke
REM *
REM *******************************************************************************

set PUTTY_INSTALLATION_PATH=C:\Program Files (x86)\PuTTY

set WORKFLOW_ID=###
set WORKFLOW_PARAMETERS=###

set USERNAME=ra_demo
set PASSWORD=ra_demo
set HOST=localhost
set PORT=31005

REM *** end of configuration section

set SSH_COMMAND="%PUTTY_INSTALLATION_PATH%\plink" -P %PORT% -l %USERNAME% -pw %PASSWORD% %HOST%
set SCP_COMMAND="%PUTTY_INSTALLATION_PATH%\pscp" -P %PORT% -l %USERNAME% -pw %PASSWORD% 

REM *** examples of related commands; remove the "REM" comment marker to test them

REM %SSH_COMMAND% net info
REM %SSH_COMMAND% components list

echo Initializing session...
%SSH_COMMAND% ra init --compact >session_token.tmp
set /p SESSION_TOKEN=<session_token.tmp
del session_token.tmp
set SCP_ROOT_PATH=%HOST%:/ra/%SESSION_TOKEN%
echo Received session token %SESSION_TOKEN%

echo Uploading input files...
%SCP_COMMAND% -r input/* %SCP_ROOT_PATH%/input

echo Executing remote tool...
%SSH_COMMAND% ra run-wf %SESSION_TOKEN% --show-output %WORKFLOW_ID% %WORKFLOW_PARAMETERS%
set EXIT_CODE=%ERRORLEVEL%
echo Exit code of remote execution command: %EXIT_CODE%

if %EXIT_CODE% == 0 goto :FINISHED_OK
echo Tool run ended with an error, skipping download of output
goto :END

:FINISHED_OK

echo Downloading output files...
%SCP_COMMAND% -r %SCP_ROOT_PATH%/output output

echo Downloading log files...
%SCP_COMMAND% -r %SCP_ROOT_PATH%/logs logs

%SSH_COMMAND% ra dispose %SESSION_TOKEN%

:END
