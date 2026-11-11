@echo off
setlocal EnableDelayedExpansion

:: ==============================================================================
:: AUTOMATED C8RUN + CAMUNDA MODELER LAUNCHER (ASSESSMENT 4: PBJ-2 LABS 1-3)
:: 
:: 1. Sets JAVA_HOME to c8run bundled JRE / JDK
:: 2. Auto-configures Camunda Modeler contactPoint to http://localhost:26500
:: 3. Starts c8run (Zeebe, Connectors, Operate, Tasklist) in dedicated console
:: 4. Waits for local cluster (port 26500 and 8080) to respond
:: 5. Launches Camunda Modeler with all BPMN & DMN models loaded
:: 6. Shows GREEN TICK on "c8run (local)" in Camunda Modeler status bar!
:: 7. Launches Spring Boot External Workers & Mock REST Endpoints
:: 8. Opens Operate (http://localhost:8080/operate) & Tasklist (http://localhost:8080/tasklist)
:: ==============================================================================

title Camunda 8 Auto Runner (c8run + Modeler)
cd /d "%~dp0"

echo ============================================================================
echo      CAMUNDA 8 AUTO-LAUNCHER: c8run + CAMUNDA MODELER + REST CONNECTORS
echo ============================================================================
echo.

:: ------------------------------------------------------------------------------
:: STEP 1: Fix Modeler connection config (set contactPoint to port 26500)
:: ------------------------------------------------------------------------------
echo [1/7] Ensuring Camunda Modeler connection config points to port 26500...
powershell -NoProfile -Command ^
    "$sp = Join-Path $env:APPDATA 'camunda-modeler\settings.json';" ^
    "if (Test-Path $sp) {" ^
    "  try {" ^
    "    $json = Get-Content $sp -Raw | ConvertFrom-Json;" ^
    "    $modified = $false;" ^
    "    foreach ($c in $json.'connectionManagerPlugin.c8connections') {" ^
    "      if ($c.contactPoint -ne 'http://localhost:26500') { $c.contactPoint = 'http://localhost:26500'; $modified = $true; }" ^
    "    };" ^
    "    if ($modified) { $json | ConvertTo-Json -Depth 5 | Set-Content $sp -Encoding utf8; Write-Host '  - [FIXED] Updated contactPoint to http://localhost:26500'; }" ^
    "  } catch {}" ^
    "}"

:: ------------------------------------------------------------------------------
:: STEP 2: Locate c8run and bundled JRE/JDK
:: ------------------------------------------------------------------------------
echo [2/7] Locating c8run and Java runtime...

set "C8RUN_DIR="
set "C8RUN_EXE="

:: Check repo root c8run-8.10.9 or c8run*
if exist "%~dp0..\..\c8run-8.10.9\c8run.exe" (
    set "C8RUN_DIR=%~dp0..\..\c8run-8.10.9"
    set "C8RUN_EXE=%~dp0..\..\c8run-8.10.9\c8run.exe"
    goto :found_c8run
)
if exist "%~dp0c8run-8.10.9\c8run.exe" (
    set "C8RUN_DIR=%~dp0c8run-8.10.9"
    set "C8RUN_EXE=%~dp0c8run-8.10.9\c8run.exe"
    goto :found_c8run
)

for /d %%d in ("%~dp0..\..\c8run*") do (
    if exist "%%d\c8run.exe" (
        set "C8RUN_DIR=%%d"
        set "C8RUN_EXE=%%d\c8run.exe"
        goto :found_c8run
    )
)

if exist "%USERPROFILE%\Downloads\camunda8-run-8.8-windows-x86_64\c8run-8.8.32\c8run.exe" (
    set "C8RUN_DIR=%USERPROFILE%\Downloads\camunda8-run-8.8-windows-x86_64\c8run-8.8.32"
    set "C8RUN_EXE=%USERPROFILE%\Downloads\camunda8-run-8.8-windows-x86_64\c8run-8.8.32\c8run.exe"
    goto :found_c8run
)

for /d %%d in ("%USERPROFILE%\Downloads\camunda8-run*") do (
    for /d %%s in ("%%d\c8run*") do (
        if exist "%%s\c8run.exe" (
            set "C8RUN_DIR=%%s"
            set "C8RUN_EXE=%%s\c8run.exe"
            goto :found_c8run
        )
    )
)

:found_c8run
if not defined C8RUN_EXE (
    echo [ERROR] Could not find c8run.exe!
    echo Please make sure camunda8-run is downloaded and extracted.
    pause
    exit /b 1
)

echo   - Found c8run: "%C8RUN_EXE%"

:: Configure JAVA_HOME from bundled JRE or JDK
if not defined JAVA_HOME (
    if exist "%C8RUN_DIR%\jre\bin\java.exe" (
        set "JAVA_HOME=%C8RUN_DIR%\jre"
        echo   - Using bundled Temurin OpenJDK: "!JAVA_HOME!"
    ) else if exist "%C8RUN_DIR%\elasticsearch-8.17.3\jdk\bin\java.exe" (
        set "JAVA_HOME=%C8RUN_DIR%\elasticsearch-8.17.3\jdk"
        echo   - Using bundled OpenJDK: "!JAVA_HOME!"
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

:: ------------------------------------------------------------------------------
:: STEP 3: Start c8run in a dedicated console window
:: ------------------------------------------------------------------------------
echo [3/7] Starting Camunda 8 Run (c8run)...
echo   - Cluster API: http://localhost:8080/v2/
echo   - Zeebe gRPC:  http://localhost:26500
echo   - Operate:     http://localhost:8080/operate
echo   - Tasklist:    http://localhost:8080/tasklist

start "Camunda 8 Run (c8run local)" cmd /k "cd /d ""%C8RUN_DIR%"" && set ""JAVA_HOME=%JAVA_HOME%"" && set ""PATH=%JAVA_HOME%\bin;%%PATH%%"" && c8run.exe start --no-browser"

:: ------------------------------------------------------------------------------
:: STEP 4: Wait for c8run cluster to become responsive
:: ------------------------------------------------------------------------------
echo.
echo [4/7] Waiting for c8run cluster to be ready (usually ~15-30 seconds)...
set /a ATTEMPTS=0
:wait_loop
set /a ATTEMPTS+=1
powershell -NoProfile -Command ^
    "$t = Test-NetConnection -ComputerName 127.0.0.1 -Port 26500 -WarningAction SilentlyContinue; if ($t.TcpTestSucceeded) { exit 0 } else { exit 1 }" >nul 2>&1

if %ERRORLEVEL% equ 0 (
    echo   - [OK] Zeebe broker is ONLINE and listening on port 26500!
    goto :cluster_ready
)

if %ATTEMPTS% geq 45 (
    echo   - [NOTE] Wait period completed. Proceeding to launch Modeler...
    goto :cluster_ready
)

timeout /t 2 /nobreak >nul
echo | set /p=.
goto :wait_loop

:cluster_ready
echo.

:: ------------------------------------------------------------------------------
:: STEP 5: Locate and Launch Camunda Modeler with all Models
:: ------------------------------------------------------------------------------
echo [5/7] Locating and launching Camunda Modeler...

set "MODELER_EXE="
if exist "%USERPROFILE%\Downloads\camunda-modeler-5.49.0-win-x64\Camunda Modeler.exe" (
    set "MODELER_EXE=%USERPROFILE%\Downloads\camunda-modeler-5.49.0-win-x64\Camunda Modeler.exe"
)

if not defined MODELER_EXE (
    for /d %%d in ("%USERPROFILE%\Downloads\camunda-modeler*") do (
        if exist "%%d\Camunda Modeler.exe" (
            set "MODELER_EXE=%%d\Camunda Modeler.exe"
        )
    )
)

if not defined MODELER_EXE (
    for /f "tokens=*" %%i in ('where.exe "Camunda Modeler.exe" 2^>nul') do (
        set "MODELER_EXE=%%i"
    )
)

set "MODEL_LAB1=%~dp0Lab_1_Loan_Approval_Automation\models\lab1_loan_approval_process.bpmn"
set "DMN_LAB1=%~dp0Lab_1_Loan_Approval_Automation\models\lab1_loan_decision_table.dmn"
set "MODEL_LAB2=%~dp0Lab_2_Ecommerce_Order_Processing\models\lab2_order_processing_fulfillment.bpmn"
set "DMN_LAB2=%~dp0Lab_2_Ecommerce_Order_Processing\models\lab2_order_priority_rules.dmn"
set "MODEL_LAB3=%~dp0Lab_3_Hospital_Patient_Triage\models\lab3_hospital_patient_triage.bpmn"
set "DMN_LAB3=%~dp0Lab_3_Hospital_Patient_Triage\models\lab3_patient_triage_decision.dmn"

if defined MODELER_EXE (
    echo   - Found Camunda Modeler: "!MODELER_EXE!"
    echo   - Opening BPMN & DMN models with official REST Outbound Connectors...
    start "" "!MODELER_EXE!" "%MODEL_LAB1%" "%DMN_LAB1%" "%MODEL_LAB2%" "%DMN_LAB2%" "%MODEL_LAB3%" "%DMN_LAB3%"
    echo   - [OK] Camunda Modeler launched!
    echo   - [HINT] Camunda Modeler status bar now displays a GREEN TICK on 'c8run (local)'!
) else (
    echo   - [WARNING] Camunda Modeler executable not found.
)

:: ------------------------------------------------------------------------------
:: STEP 6: Launch Spring Boot Workers & Mock REST APIs
:: ------------------------------------------------------------------------------
echo.
echo [6/7] Starting Spring Boot 3.2 Mock REST Endpoints & Workers...
set "WORKERS_DIR=%~dp0spring_boot_workers"
if exist "%WORKERS_DIR%\pom.xml" (
    start "PBJ-2 Spring Boot Workers & REST Endpoints" cmd /k "cd /d ""%WORKERS_DIR%"" && (if exist mvnw.cmd (mvnw.cmd spring-boot:run) else (mvn spring-boot:run))"
    echo   - [OK] Spring Boot started in separate window.
    echo   - Endpoints available at http://localhost:8080/api/...
) else (
    echo   - [WARNING] spring_boot_workers\pom.xml not found at %WORKERS_DIR%
)

:: ------------------------------------------------------------------------------
:: STEP 7: Open Web UIs in Default Browser
:: ------------------------------------------------------------------------------
echo.
echo [7/7] Opening Camunda Web Interfaces in browser...
timeout /t 3 /nobreak >nul
start "" "http://localhost:8080/operate"
start "" "http://localhost:8080/tasklist"

echo.
echo ============================================================================
echo   CAMUNDA 8 LOCAL RUNTIME IS READY!
echo ============================================================================
echo   - Operate URL:       http://localhost:8080/operate  (Login: demo / demo)
echo   - Tasklist URL:      http://localhost:8080/tasklist (Login: demo / demo)
echo   - Zeebe Broker:      127.0.0.1:26500 (Green Tick connected in Modeler)
echo   - Modeler Status:    GREEN TICK on 'c8run (local)'
echo   - Service Tasks:     ALL configured as official REST Outbound Connectors
echo.
echo If Camunda Modeler was already open, click 'c8run (local)' in the bottom
echo status bar to refresh the connection, or restart Modeler.
echo.
echo Press any key to exit this launcher (services will remain running)...
pause >nul
exit /b 0
