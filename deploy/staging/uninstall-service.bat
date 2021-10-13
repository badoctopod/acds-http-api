REM Initial variables
set APP_HOME=D:\applications\acds-http-api
set SERVICE_NAME=acds-http-api-staging

REM Uninstall service
%APP_HOME%\bin\acds-http-api.exe //DS//%SERVICE_NAME%

pause
