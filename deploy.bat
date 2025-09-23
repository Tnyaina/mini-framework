@echo off
setlocal EnableDelayedExpansion

:: Configuration
set "PROJECT_ROOT=%CD%"
set "BUILD_DIR=%PROJECT_ROOT%\build"
set "CLASSES_DIR=%BUILD_DIR%\classes"

:: Nettoyage ancien build
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"

:: Compilation Java
set "CLASSPATH="
for %%i in ("%PROJECT_ROOT%\lib\*.jar") do (
    if "!CLASSPATH!"=="" (
        set "CLASSPATH=%%i"
    ) else (
        set "CLASSPATH=!CLASSPATH!;%%i"
    )
)
dir /s /b "%PROJECT_ROOT%\src\main\java\*.java" > sources.txt
javac -cp "%CLASSPATH%" -d "%CLASSES_DIR%" @sources.txt
if errorlevel 1 (
    echo Erreur compilation
    del sources.txt
    exit /b 1
)
del sources.txt

:: Création du JAR
if exist "%PROJECT_ROOT%\mini-framework.jar" del "%PROJECT_ROOT%\mini-framework.jar"
cd "%CLASSES_DIR%"
jar -cvf "%PROJECT_ROOT%\mini-framework.jar" *

:: Copier le JAR dans C:\lib
if not exist "C:\lib" mkdir "C:\lib"
copy "%PROJECT_ROOT%\mini-framework.jar" "C:\lib\"

echo Mini-framework JAR créé et copié dans C:\lib
cd "%PROJECT_ROOT%"
