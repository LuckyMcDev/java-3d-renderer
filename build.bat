@echo off
setlocal enabledelayedexpansion

:: =======================================================
:: Configuration Variables
:: =======================================================
set "JAVAFX_LIB=lib\javafx-sdk-17.0.14\lib"
set "IMGUI_LIB=lib\dear_imgui\java-libraries"
set "MAIN_CLASS=Main"
set "APP_JAR=app.jar"
set "SRC_DIR=src"
set "BIN_DIR=bin"
set "DOCS_DIR=docs"
set "BUILD_TIMESTAMP=build.timestamp"
set "ERROR_LOG=error.log"

:: Default build mode (debug by default)
set "BUILD_MODE=debug"
set "JAVAC_OPTIONS=-g"

:: =======================================================
:: Build Mode Selection
:: =======================================================
:chooseBuildMode
cls
echo =====================================
echo           Choose Build Mode
echo =====================================
echo   1. Debug (default, includes debug symbols)
echo   2. Release (optimized, no debug symbols)
echo =====================================
set /p mode="Enter 1 or 2: "
if "%mode%"=="2" (
    set "BUILD_MODE=release"
    set "JAVAC_OPTIONS="
) else if "%mode%"=="1" (
    set "BUILD_MODE=debug"
    set "JAVAC_OPTIONS=-g"
) else (
    echo Invalid choice, defaulting to Debug mode.
    set "BUILD_MODE=debug"
    set "JAVAC_OPTIONS=-g"
)
goto menu

:: =======================================================
:: Main Menu Loop
:: =======================================================
:menu
cls
echo =====================================
echo          3D Renderer - Menu
echo =====================================
echo.
echo :build   Build Project (Incremental)
echo :jar     Create Jar (with manifest for dependencies)
echo :clean   Clean Build Artifacts
echo :run     Run Project
echo :javadoc Generate Javadoc Documentation
echo :help    Show Help
echo :exit    Exit
echo.
set /p choice="Please choose an option (:build, :jar, :clean, :run, :javadoc, :help, :exit): "

if "%choice%"==":build" (
    call :timestamp "Starting Build"
    :: Check if source directory exists
    if not exist "%SRC_DIR%" (
        echo Source directory "%SRC_DIR%" not found!
        echo [%time%] Error: Source directory not found >> %ERROR_LOG%
        pause
        goto menu
    )
    if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
    
    :: Incremental Build Check: If BUILD_TIMESTAMP exists, compile only if any .java file is newer
    if exist "%BUILD_TIMESTAMP%" (
        for %%T in ("%BUILD_TIMESTAMP%") do set "BT=%%~tT"
        set "NEED_BUILD=0"
        for /r "%SRC_DIR%" %%F in (*.java) do (
            if "%%~tF" GTR "%BT%" (
                set "NEED_BUILD=1"
                goto :found_newer
            )
        )
        :found_newer
        if "%NEED_BUILD%"=="0" (
            echo No changes detected. Skipping compilation.
            call :timestamp "Build Skipped (Up-to-date)"
            pause
            goto menu
        )
    )
    echo Building Project...
    dir /b /s "%SRC_DIR%\*.java" > sources.txt
    javac %JAVAC_OPTIONS% --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d "%BIN_DIR%" @sources.txt 2>> %ERROR_LOG%
    if errorlevel 1 (
        echo Build failed. Check %ERROR_LOG% for details.
        call :timestamp "Build Failed"
        pause
    ) else (
        echo Build succeeded.
        call :timestamp "Build Succeeded"
        rem Update build timestamp
        echo. > "%BUILD_TIMESTAMP%"
        pause
    )
    goto menu

) else if "%choice%"==":jar" (
    call :timestamp "Packaging Jar Started"
    echo Creating Jar...
    rem Create a temporary manifest file with Main-Class and a basic Class-Path (adjust if you want to include dependencies)
    echo Main-Class: %MAIN_CLASS% > manifest.txt
    echo Class-Path: . >> manifest.txt
    jar cvfm %APP_JAR% manifest.txt -C "%BIN_DIR%" . 2>> %ERROR_LOG%
    if errorlevel 1 (
        echo Jar packaging failed. Check %ERROR_LOG% for details.
        call :timestamp "Jar Packaging Failed"
        pause
    ) else (
        echo Jar packaging succeeded.
        call :timestamp "Jar Packaging Succeeded"
        pause
    )
    goto menu

) else if "%choice%"==":clean" (
    call :timestamp "Clean Started"
    echo Cleaning Build Artifacts...
    if exist "%BIN_DIR%" rmdir /s /q "%BIN_DIR%"
    if exist %APP_JAR% del %APP_JAR%
    if exist "%BUILD_TIMESTAMP%" del "%BUILD_TIMESTAMP%"
    if exist "%DOCS_DIR%" rmdir /s /q "%DOCS_DIR%"
    echo Clean complete.
    call :timestamp "Clean Completed"
    pause
    goto menu

) else if "%choice%"==":run" (
    call :timestamp "Run Started"
    echo Building Project...
    if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
    dir /b /s "%SRC_DIR%\*.java" > sources.txt
    javac %JAVAC_OPTIONS% --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d "%BIN_DIR%" @sources.txt 2>> %ERROR_LOG%
    if errorlevel 1 (
        echo Build failed. Check %ERROR_LOG% for details.
        call :timestamp "Build Failed"
        pause
    ) else (
        echo Build succeeded.
        call :timestamp "Build Succeeded"
        echo Running Project...
        :: The classpath includes both the bin folder and all jars in the ImGui library folder
        java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -cp "%BIN_DIR%;%IMGUI_LIB%\*" %MAIN_CLASS%
        call :timestamp "Run Completed"
        pause
    )
    goto menu

) else if "%choice%"==":javadoc" (
    call :timestamp "Javadoc Generation Started"
    echo Generating Javadoc...
    del docs\
    :: Create the docs directory if it does not exist
    if not exist "%DOCS_DIR%" mkdir "%DOCS_DIR%"
    :: Generate Javadoc from the source files, including all subpackages
    :: Ensure to include the JavaFX and ImGui libraries in the classpath for Javadoc generation
    javadoc -d docs -sourcepath src src\Main.java --module-path lib\javafx-sdk-17.0.14\lib --add-modules javafx.controls,javafx.fxml @sources.txt 2>> %ERROR_LOG%
    if errorlevel 1 (
        echo Javadoc generation failed. Check %ERROR_LOG% for details.
        call :timestamp "Javadoc Generation Failed"
        pause
    ) else (
        echo Javadoc generation succeeded.
        call :timestamp "Javadoc Generation Succeeded"
        pause
    )
    goto menu

) else if "%choice%"==":help" (
    call :displayHelp
    goto menu

) else if "%choice%"==":exit" (
    echo Exiting...
    exit /b 0
) else if "%choice%"==":openjavadoc" (
    echo Opening Javadoc...
    start docs\index.html
    goto menu
) else (
    echo Invalid choice. Please select a valid option.
    pause
    goto menu
)

:: =======================================================
:: Help Function
:: =======================================================
:displayHelp
cls
echo =====================================
echo                 Help
echo =====================================
echo :build   - Compiles the project. Performs an incremental build by
echo            checking if any .java file has been modified since the
echo            last build (using a rudimentary timestamp check).
echo :jar     - Packages the compiled classes into a jar file with a
echo            manifest specifying the main class. (Dependencies are not
echo            bundled; see packaging enhancements.)
echo :clean   - Removes build artifacts (bin folder, jar file, build timestamp, and docs).
echo :run     - Builds (if necessary) and runs the project, including JavaFX
echo            and ImGui libraries.
echo :javadoc - Generates Javadoc documentation from the source files and
echo            outputs it to the "docs" directory.
echo :openjavadoc - Opens the Javadoc documentation in a web browser.
echo :help    - Displays this help message.
echo :exit    - Exits the script.
echo.
pause
goto menu

:: =======================================================
:: Timestamp Logging Function
:: =======================================================
:timestamp
:: Usage: call :timestamp "Your message here"
echo [%time%] %~1
goto :eof