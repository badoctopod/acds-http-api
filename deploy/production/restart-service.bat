REM Initial variables
set APP_HOME=C:\applications\acds-http-api
set SERVICE_NAME=acds-http-api-prod

REM Stop service
sc stop %SERVICE_NAME%

REM Waiting for 10 seconds...
timeout 10 > NUL

REM Start service
sc start %SERVICE_NAME%

REM Waiting for 10 seconds...
timeout 10 > NUL

REM Quering service state
sc queryex %SERVICE_NAME%

pause
