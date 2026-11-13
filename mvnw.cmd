@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set "DIR=%~dp0"
if "%JAVA_HOME%"=="" (
  echo JAVA_HOME is not set. Please set JAVA_HOME.
)
set "WRAPPER_JAR=%DIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%DIR%\.mvn\wrapper\maven-wrapper.properties"

mvn %*
if %ERRORLEVEL% NEQ 0 (
  echo Attempting build via mvn wrapper or standard tools.
)
exit /b %ERRORLEVEL%
