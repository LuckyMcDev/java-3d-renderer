@echo off
cls

:: Set the path to the JavaFX SDK
set "JAVAFX_LIB=C:\Users\Fynn\Projects\tiny-java-3d-render\lib\javafx-sdk-17.0.14\lib"
set "IMGUI_LIB=C:\Users\Fynn\Projects\tiny-java-3d-render\lib\dear_imgui\java-libraries"

:menu
cls
echo ======Configuration Menu======
echo.
echo :build Build Project
echo :jar   Create Jar
echo :clean Clean Build Artifacts
echo :run   Run Project
echo :exit  Exit
echo.

:: Prompt for choice
set /p choice="Please choose an option (:build, :jar, :clean, :run, :exit): "

:: Process user choice
if "%choice%"==":build" (
    echo.
    echo Building Project...
    if not exist bin mkdir bin
    :: Recursively list all Java files in src and compile them
    dir /b /s src\*.java > sources.txt
    javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d bin @sources.txt
    if errorlevel 1 (
        echo Build failed.
        pause
    ) else (
        echo Build succeeded.
        pause
    )
    goto menu
) else if "%choice%"==":jar" (
    echo.
    echo Creating Jar...
    jar cvfe app.jar Main -C bin .
    if errorlevel 1 (
        echo Jar packaging failed.
        pause
    ) else (
        echo Jar packaging succeeded.
        pause
    )
    goto menu
) else if "%choice%"==":clean" (
    echo.
    echo Cleaning Build Artifacts...
    if exist bin rmdir /s /q bin
    if exist app.jar del app.jar
    echo Clean complete.
    pause
    goto menu
)else if "%choice%"==":run" (
    echo.
    echo Building Project...
    if not exist bin mkdir bin
    :: Recursively list all Java files in src and compile them
    dir /b /s src\*.java > sources.txt
    javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d bin @sources.txt
    if errorlevel 1 (
        echo Build failed.
        pause
    ) else (
        echo Build succeeded.
        echo.
        echo Running Project...
        java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -cp bin;%IMGUI_LIB%\* Main
        pause
    )
    goto menu
) else if "%choice%"==":exit" (
    echo.
    echo Exiting...
    exit /b 0
) else (
    echo.
    echo Invalid choice. Please select a valid option.
    pause
    goto menu
)
