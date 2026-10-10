@echo off
setlocal enabledelayedexpansion
title Online Project Management Tool - Enterprise Server

echo =========================================================================
echo   Online Project Management Tool - Enterprise Server Launcher
echo =========================================================================
echo.

set JAVA_HOME=C:\Users\luvsi\.jdks\ms-21.0.12.1
set PATH=%JAVA_HOME%\bin;%PATH%

if not exist target\classes mkdir target\classes

echo [1/3] Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d target\classes @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed. Check the error log above.
    del sources.txt
    pause
    exit /b %errorlevel%
)
del sources.txt
echo [OK] Compilation successful (105 Java source files).

echo [2/3] Syncing schema and configuration resources...
if not exist target\classes\schema.sql copy /y src\main\resources\schema.sql target\classes\schema.sql > nul
if not exist target\classes\application.properties copy /y src\main\resources\application.properties target\classes\application.properties > nul

echo [3/3] Launching HTTP Application Server on http://localhost:8080...
start "" http://localhost:8080

java -cp target\classes com.pmtool.AppServer
