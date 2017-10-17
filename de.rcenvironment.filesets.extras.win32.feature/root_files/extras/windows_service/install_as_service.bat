::  System dependent variable: Please adapt!
::  Absolute path to RCE installation folder
SET RCE_ROOT_PATH=D:\RCE

::  Name of the local user account the RCE service should run as.
SET RCE_SERVICE_USER=rce_user

::  System undependent variables: Change if you like.
:: Name of the RCE profile the service should use.
SET RCE_PROFILE_NAME=default

::  ID of your service
SET RCE_SERVICE_ID=RCE_Service

::  Display name
SET PR_DISPLAYNAME=RCE Service

::  Service description
SET PR_DESCRIPTION=RCE - Remote Component Environment running in headless mode as service.

::  System undependent variables: Leave them unmodified!
::  Absolute path to the prunsrv.exe
SET SRVEXE=%RCE_ROOT_PATH%/extras/windows_service/prunsrv.exe
::  Name of executable
SET EXE=rce.exe
::  Parameters
SET PARAM1=--headless
SET PARAM2=-noSplash
SET PARAM3=-p
SET PARAM4=%RCE_PROFILE_NAME%
SET SD_PARAM1=--headless
SET SD_PARAM2=--shutdown

SET PR_LOGPATH=%DIR%
SET PR_LOGPREFIX=%RCE_SERVICE_ID%
SET PR_LOGLEVEL=Info

IF DEFINED RCE_SERVICE_USER (
	SET PR_SERVICEUSER=.\%RCE_SERVICE_USER%
)

%SRVEXE% //IS//%RCE_SERVICE_ID% --Install=%SRVEXE% --Startup=auto --StartMode=exe --StartImage=%RCE_ROOT_PATH%/%EXE% --StartPath=%RCE_ROOT_PATH% --StartParams=%PARAM1%;%PARAM2%;%PARAM3%;%PARAM4% --StopMode=exe --StopImage=%RCE_ROOT_PATH%/%EXE% --StopPath=%RCE_ROOT_PATH% --StopParams=%SD_PARAM1%;%SD_PARAM2%;%PARAM3%;%PARAM4%