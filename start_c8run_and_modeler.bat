@echo off
setlocal EnableDelayedExpansion
title Camunda 8 Auto Runner (Root Launcher)
cd /d "%~dp0Project_Assignments\Assessment_4_PBJ2_End_to_End_Workflow_Automation"
call start_c8run_and_modeler.bat %*
