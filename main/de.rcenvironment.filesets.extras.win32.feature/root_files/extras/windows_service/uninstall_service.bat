::  System dependent variable: Please adapt!
::  Absolute path to RCE installation folder
SET RCE_ROOT_PATH=D:/RCE

::  ID of your service
::  Must be equal to RCE_SERVICE_ID set in install_as_service.bat!
SET RCE_SERVICE_ID=RCE_Service

::  System undependent variables: Leave them unmodified!
::  Absolute path to the prunsrv.exe
SET SRVEXE=%RCE_ROOT_PATH%/extras/windows_service/prunsrv.exe


%SRVEXE% //DS//%RCE_SERVICE_ID%