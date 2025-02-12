@echo off
cls

:: Display banner
echo ================================
echo       Welcome to My Project      
echo ================================
echo.

:: Show options
echo 1. Open Project
echo 2. Exit
echo.

:: Prompt for choice
set /p choice="Please choose an option (1 or 2): "

:: Check user choice
if "%choice%"=="1" (
    echo Opening Project...
    :: Add your project-opening command here
    start "" "start_project.bat"
) else if "%choice%"=="2" (
    echo Exiting...
    exit
) else (
    echo Invalid choice. Please select 1 or 2.
)

pause
