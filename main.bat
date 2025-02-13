@echo off
setlocal enabledelayedexpansion

:: Set log file name and clear it at the start of the build.
set LOGFILE=build.log
if exist %LOGFILE% del %LOGFILE%

call :log "Build started."

:: Check if build.properties exists
if not exist build.properties (
  call :log "build.properties not found!"
  echo build.properties not found!
  exit /b 1
)

:: Read properties from build.properties and set environment variables
for /f "usebackq tokens=1,2 delims==" %%A in ("build.properties") do (
  set "%%A=%%B"
)
call :log "Loaded build.properties."
echo Using Java version: %java.version%
echo Main class: %main.class%
echo Dependencies: %dependencies%

:: Construct the classpath: include dependencies if specified
if defined dependencies (
  set CP=%dependencies%;bin
) else (
  set CP=bin
)
call :log "Classpath set to: %CP%."

:: Function to compile source files recursively
:compile
call :log "Compiling Java sources..."
echo Compiling Java sources...
if not exist bin mkdir bin
:: Create a list of Java source files (recursively)
dir /b /s src\*.java > sources.txt
javac %javac.options% -cp "%CP%" -source %java.version% -target %java.version% -d bin -sourcepath src -encoding UTF-8 @sources.txt
if errorlevel 1 (
  call :log "Compilation failed."
  echo Compilation failed.
  exit /b 1
)
call :log "Compilation succeeded."
echo Compilation succeeded.
goto :eof

:: Function to package compiled classes into a JAR file
:package
call :log "Packaging JAR file: %jar.name%."
echo Creating JAR file: %jar.name%
jar cvfe %jar.name% %main.class% -C bin .
if errorlevel 1 (
  call :log "JAR packaging failed."
  echo JAR packaging failed.
  exit /b 1
)
call :log "JAR packaging succeeded."
echo JAR packaging succeeded.
goto :eof

:: Function to clean the build
:clean
call :log "Cleaning build artifacts..."
echo Cleaning build artifacts...
if exist bin rmdir /s /q bin
if exist %jar.name% del %jar.name%
call :log "Clean complete."
echo Clean complete.
goto :eof

:: Check command-line arguments for build targets
if "%1"=="clean" (
  call :clean
  goto :end
) else if "%1"=="package" (
  call :compile
  call :package
  goto :end
) else if "%1"=="run" (
  call :compile
  call :log "Running %main.class%..."
  echo Running %main.class%...
  java -cp "%CP%" %main.class%
  goto :end
) else (
  :: Default action: compile and run
  call :compile
  call :log "Running %main.class%..."
  echo Running %main.class%...
  java -cp "%CP%" %main.class%
)

:end
call :log "Build finished."
pause
exit /b

:: Log function: Logs messages with a timestamp to the log file.
:log
set LOGMSG=%*
echo %date% %time% - %LOGMSG% >> %LOGFILE%
goto :eof