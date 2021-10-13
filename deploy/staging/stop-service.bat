REM Initial variables
set APP_HOME=D:\applications\acds-http-api
set SERVICE_NAME=acds-http-api-staging

REM Stop service
sc stop %SERVICE_NAME%

REM Waiting for 10 seconds...
timeout 10 > NUL

REM Quering service state
sc queryex %SERVICE_NAME%

pause
