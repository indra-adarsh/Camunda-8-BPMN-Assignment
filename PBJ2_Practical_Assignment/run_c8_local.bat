@echo off
setlocal EnableDelayedExpansion

:: ==============================================================================
:: Camunda 8 Local Runner for Assessment 4 (PBJ-2 Labs 1-3)
:: Supports:
::   1) Camunda 8 Run (c8run.exe) - Native local Camunda 8 (Zeebe, Operate, Tasklist)
::   2) Camunda 8 Docker Compose  - Containerized full stack (Zeebe, Operate, Tasklist, Connectors)
::   3) Spring Boot Workers       - External job workers & mock REST APIs (Labs 1-3)
:: ==============================================================================

title Camunda 8 Local Runner - Assessment 4
cd /d "%~dp0"

set "IS_INTERACTIVE=1"
if "%~1" neq "" (
    set "IS_INTERACTIVE=0"
    if /i "%~1"=="c8run" goto :start_c8run
    if /i "%~1"=="start" goto :start_c8run
    if /i "%~1"=="docker" goto :start_docker
    if /i "%~1"=="workers" goto :start_workers
    if /i "%~1"=="all" goto :start_all
    if /i "%~1"=="stop" goto :stop_all
    if /i "%~1"=="status" goto :check_status
    if /i "%~1"=="ui" goto :open_ui
    if /i "%~1"=="open" goto :open_ui
    echo [ERROR] Unknown option "%~1"
    goto :exit
)

:menu
cls
echo ============================================================================
echo           CAMUNDA 8 LOCAL RUNNER - ASSESSMENT 4 (PBJ-2 LABS 1-3)
echo ============================================================================
echo.
echo  [1] Start Camunda 8 Run (c8run.exe start)  [Recommended for Local Windows]
echo  [2] Start Camunda 8 via Docker Compose    (Zeebe, Operate, Tasklist, Connectors)
echo  [3] Start Spring Boot External Workers     (Labs 1-3 Workers + Mock REST APIs)
echo  [4] Start ALL (Camunda 8 + Spring Boot Workers)
echo  [5] Stop Camunda 8 Services (c8run stop / docker compose down)
echo  [6] Open Web Interfaces in Browser        (Operate & Tasklist)
echo  [7] Check Port & Service Status           (26500, 8080, 8081, 8082, 8085)
echo  [8] Exit
echo.
echo ============================================================================
set /p choice="Enter your choice [1-8]: "

if "%choice%"=="1" goto :start_c8run
if "%choice%"=="2" goto :start_docker
if "%choice%"=="3" goto :start_workers
if "%choice%"=="4" goto :start_all
if "%choice%"=="5" goto :stop_all
if "%choice%"=="6" goto :open_ui
if "%choice%"=="7" goto :check_status
if "%choice%"=="8" goto :exit
goto :menu

:: ------------------------------------------------------------------------------
:: Find c8run.exe
:: ------------------------------------------------------------------------------
:find_c8run
set "C8RUN_EXE="
if exist ".\c8run.exe" set "C8RUN_EXE=.\c8run.exe" & goto :found_c8run
if exist ".\c8run\c8run.exe" set "C8RUN_EXE=.\c8run\c8run.exe" & goto :found_c8run
if exist "%USERPROFILE%\c8run\c8run.exe" set "C8RUN_EXE=%USERPROFILE%\c8run\c8run.exe" & goto :found_c8run
if exist "C:\camunda\c8run\c8run.exe" set "C8RUN_EXE=C:\camunda\c8run\c8run.exe" & goto :found_c8run
if exist "C:\c8run\c8run.exe" set "C8RUN_EXE=C:\c8run\c8run.exe" & goto :found_c8run

for /f "tokens=*" %%i in ('where.exe c8run.exe 2^>nul') do (
    set "C8RUN_EXE=%%i"
    goto :found_c8run
)

for /d %%d in ("%USERPROFILE%\Downloads\c8run*") do (
    if exist "%%d\c8run.exe" (
        set "C8RUN_EXE=%%d\c8run.exe"
        goto :found_c8run
    )
)

:found_c8run
goto :eof

:: ------------------------------------------------------------------------------
:: Option 1: Start Camunda 8 Run (c8run)
:: ------------------------------------------------------------------------------
:start_c8run
cls
echo [INFO] Searching for c8run.exe...
call :find_c8run

if not defined C8RUN_EXE (
    echo.
    echo [WARNING] c8run.exe was not found in common paths:
    echo   - Current directory (.\c8run.exe)
    echo   - Subfolder (.\c8run\c8run.exe)
    echo   - %USERPROFILE%\c8run\c8run.exe
    echo   - %USERPROFILE%\Downloads\c8run*\c8run.exe
    echo   - System PATH
    echo.
    echo How to get c8run:
    echo   1. Download Camunda 8 Run for Windows:
    echo      https://camunda.com/download/c8run/
    echo   2. Extract it into this folder or C:\camunda\c8run
    echo   3. Re-run this script!
    echo.
    echo Or you can use Option 2 (Docker Compose) if Docker Desktop is installed.
    echo.
    if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )
)

echo [INFO] Found c8run at: "%C8RUN_EXE%"
echo [INFO] Starting Camunda 8 Run...
echo.
"%C8RUN_EXE%" start
echo.
echo [INFO] c8run started.
echo [INFO] Zeebe Gateway: 127.0.0.1:26500
echo [INFO] Web Interfaces: http://localhost:8080 (or http://localhost:8081)
echo.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 2: Start Camunda 8 via Docker Compose
:: ------------------------------------------------------------------------------
:start_docker
cls
echo [INFO] Checking Docker installation...
where.exe docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not installed or not in PATH.
    echo Please install Docker Desktop (https://www.docker.com/products/docker-desktop)
    echo or use Option 1 (c8run).
    echo.
    if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )
)

echo [INFO] Starting Camunda 8 local stack via Docker Compose...
echo [INFO] Services: Zeebe (26500), Operate (8081), Tasklist (8082), Connectors (8085), Elasticsearch (9200)
echo.
docker compose -f docker-compose-c8-local.yaml up -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Docker Compose failed to start.
    echo Ensure Docker Desktop is running and try again.
    echo.
    if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )
)
echo.
echo [INFO] Camunda 8 containers started successfully!
echo   - Zeebe Gateway:      127.0.0.1:26500
echo   - Camunda Operate:    http://localhost:8081 (Credentials: demo / demo)
echo   - Camunda Tasklist:   http://localhost:8082 (Credentials: demo / demo)
echo   - Connectors Runtime: http://localhost:8085
echo.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 3: Start Spring Boot External Workers
:: ------------------------------------------------------------------------------
:start_workers
cls
echo [INFO] Starting Spring Boot 3.2 External Workers and Mock REST APIs...
echo [INFO] Directory: %~dp0spring_boot_workers
echo.
if not exist "%~dp0spring_boot_workers\pom.xml" (
    echo [ERROR] pom.xml not found in %~dp0spring_boot_workers!
    if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )
)

start "PBJ-2 Spring Boot Workers & Mock APIs" cmd /k "cd /d ""%~dp0spring_boot_workers"" && (if exist mvnw.cmd (mvnw.cmd spring-boot:run) else (mvn spring-boot:run))"
echo [INFO] Spring Boot workers process launched in a separate window.
echo [INFO] REST Endpoints will listen on http://localhost:8080:
echo   - POST /api/credit/assess
echo   - POST /api/loan/disburse
echo   - POST /api/payment/process
echo   - POST /api/order/fulfill
echo   - POST /api/hospital/scheduling/available-slots
echo   - POST /api/hospital/scheduling/confirm-booking
echo   - POST /api/notifications/send
echo.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 4: Start ALL (Docker C8 + Workers or c8run + Workers)
:: ------------------------------------------------------------------------------
:start_all
cls
echo [INFO] Starting Full Environment...
call :find_c8run
if defined C8RUN_EXE (
    echo [INFO] Starting c8run...
    "%C8RUN_EXE%" start
) else (
    echo [INFO] c8run not found, trying Docker Compose...
    docker compose -f docker-compose-c8-local.yaml up -d
)

timeout /t 3 /nobreak >nul
echo.
echo [INFO] Launching Spring Boot Workers...
start "PBJ-2 Spring Boot Workers & Mock APIs" cmd /k "cd /d ""%~dp0spring_boot_workers"" && (if exist mvnw.cmd (mvnw.cmd spring-boot:run) else (mvn spring-boot:run))"
echo.
echo [INFO] All services initiated!
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 5: Stop Services
:: ------------------------------------------------------------------------------
:stop_all
cls
echo [INFO] Stopping Camunda 8 local services...
call :find_c8run
if defined C8RUN_EXE (
    echo [INFO] Stopping c8run...
    "%C8RUN_EXE%" stop
)

where.exe docker >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [INFO] Stopping Docker Compose containers...
    docker compose -f docker-compose-c8-local.yaml down
)

echo.
echo [INFO] Camunda 8 local services stopped.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 6: Open Web Interfaces
:: ------------------------------------------------------------------------------
:open_ui
cls
echo [INFO] Opening Camunda Web Interfaces in browser...
start "" "http://localhost:8081"
start "" "http://localhost:8082"
echo.
echo   - Camunda Operate:  http://localhost:8081  (Login: demo / demo)
echo   - Camunda Tasklist: http://localhost:8082  (Login: demo / demo)
echo.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:: ------------------------------------------------------------------------------
:: Option 7: Check Port & Service Status
:: ------------------------------------------------------------------------------
:check_status
cls
echo ============================================================================
echo                    PORT AND CONNECTIVITY STATUS CHECK
echo ============================================================================
echo.
powershell -NoProfile -Command ^
    "$ports = @( @{Name='Zeebe Broker Gateway'; Port=26500}, @{Name='Spring Boot / Mock APIs'; Port=8080}, @{Name='Camunda Operate UI'; Port=8081}, @{Name='Camunda Tasklist UI'; Port=8082}, @{Name='Connectors Runtime'; Port=8085}, @{Name='Elasticsearch'; Port=9200} );" ^
    "foreach ($p in $ports) {" ^
    "  $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port $p.Port -WarningAction SilentlyContinue;" ^
    "  $status = if ($tcp.TcpTestSucceeded) { '[LISTENING / READY]' } else { '[OFFLINE / NOT REACHABLE]' };" ^
    "  Write-Host ('{0,-28} (Port {1,5}) : {2}' -f $p.Name, $p.Port, $status);" ^
    "}"
echo.
if "%IS_INTERACTIVE%"=="1" ( pause & goto :menu ) else ( goto :exit )

:exit
exit /b 0
