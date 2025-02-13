@echo off
cls

:: Display banner
echo ================================
echo       3D Renderer    
echo ================================
echo.

:: Display menu options
echo 1. Build Project
echo 2. Create Jar
echo 3. Clean Build Artifacts
echo 4. Open Project
echo 5. Exit
echo.

:: Prompt for choice
set /p choice="Please choose an option (1-5): "

:: Process user choice
if "%choice%"=="1" (
    echo.
    echo Building Project...
    if not exist bin mkdir bin
    :: Recursively list all Java files in src and compile them
    dir /b /s src\*.java > sources.txt
    javac -d bin @sources.txt
    if errorlevel 1 (
        echo Build failed.
        pause
        exit /b 1
    ) else (
        echo Build succeeded.
    )
) else if "%choice%"=="2" (
    echo.
    echo Creating Jar...
    jar cvfe app.jar Main -C bin .
    if errorlevel 1 (
        echo Jar packaging failed.
        pause
        exit /b 1
    ) else (
        echo Jar packaging succeeded.
    )
) else if "%choice%"=="3" (
    echo.
    echo Cleaning Build Artifacts...
    if exist bin rmdir /s /q bin
    if exist app.jar del app.jar
    echo Clean complete.
) else if "%choice%"=="4" (
    echo.
    echo Opening Project...
    java -cp bin Main
) else if "%choice%"=="5" (
    echo.
    echo Exiting...
    exit /b 0
) else (
    echo.
    echo Invalid choice. Please run the script again and select a valid option.
)

pause
