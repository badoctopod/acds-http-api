REM Initial variables
set APP_HOME=C:\applications\acds-http-api
set SERVICE_NAME=acds-http-api-prod

REM Uninstall service
%APP_HOME%\bin\acds-http-api.exe //DS//%SERVICE_NAME%

pause
