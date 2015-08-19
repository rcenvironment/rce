#!/bin/bash

###############################################################################
#
# Example batch file for using the SSH Remote Access interface. Parameters 
# marked with "###" must be filled in before the script can be run.
#
# Copyright (C) 2014 DLR, Germany
# All rights reserved
# Author: Robert Mischke
#
###############################################################################

TOOL_ID=###
TOOL_VERSION=###
TOOL_PARAMETERS=###

USERNAME=ra_demo
PASSWORD=ra_demo
HOST=localhost
PORT=31005

### end of configuration section

export SSHPASS=${PASSWORD}
SSH_COMMAND="sshpass -e ssh -o StrictHostKeyChecking=no -p ${PORT} -l ${USERNAME} ${HOST}"
SCP_COMMAND="sshpass -e scp -P ${PORT}"

### examples of related commands; remove the "#" comment marker to test them

# ${SSH_COMMAND} net info
# ${SSH_COMMAND} components list

echo Initializing session...
${SSH_COMMAND} ra init --compact >session_token.tmp
SESSION_TOKEN=$(awk 'gsub(/\r/,""){print $1}' session_token.tmp)
rm session_token.tmp
SCP_REMOTE_ROOT_PATH="${USERNAME}@${HOST}:/ra/${SESSION_TOKEN}"
echo Received session token ${SESSION_TOKEN}

echo Uploading input files...
${SCP_COMMAND} -r input/* ${SCP_REMOTE_ROOT_PATH}/input

echo Executing remote tool...
${SSH_COMMAND} ra run-tool ${SESSION_TOKEN} --show-output ${TOOL_ID} ${TOOL_VERSION} ${TOOL_PARAMETERS}
EXIT_CODE=$?
if [ "${EXIT_CODE}" -ne "0" ]; then
	echo Tool run ended with an error, skipping download of output
	exit ${EXIT_CODE}
fi

echo Downloading output files...
${SCP_COMMAND} -r ${SCP_REMOTE_ROOT_PATH}/output output

echo Downloading log files...
${SCP_COMMAND} -r ${SCP_REMOTE_ROOT_PATH}/logs logs

${SSH_COMMAND} ra dispose ${SESSION_TOKEN}

exit 0
