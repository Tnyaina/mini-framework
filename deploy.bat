@echo off
setlocal EnableDelayedExpansion

:: Configuration
set "PROJECT_ROOT=%CD%"
set "WEBAPP_NAME=mini-framework"
set "TOMCAT_HOME=C:\Program Files\Apache Software Foundation\Tomcat 10.1"
set "TOMCAT_WEBAPPS=%TOMCAT_HOME%\webapps"

:: Création des répertoires temporaires
set "BUILD_DIR=%PROJECT_ROOT%\build"
set "CLASSES_DIR=%BUILD_DIR%\WEB-INF\classes"
if not exist "%CLASSES_DIR%" mkdir "%CLASSES_DIR%"

echo === Nettoyage des anciens builds ===
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"

echo === Compilation des fichiers Java ===
:: Création du CLASSPATH
set "CLASSPATH="
for %%i in ("%PROJECT_ROOT%\lib\*.jar") do (
    if "!CLASSPATH!"=="" (
        set "CLASSPATH=%%i"
    ) else (
        set "CLASSPATH=!CLASSPATH!;%%i"
    )
)

:: Compilation avec la nouvelle structure
dir /s /b "%PROJECT_ROOT%\src\main\java\*.java" > sources.txt
javac -cp "%CLASSPATH%" -d "%CLASSES_DIR%" @sources.txt
if errorlevel 1 (
    echo Erreur lors de la compilation
    del sources.txt
    exit /b 1
)
del sources.txt

echo === Copie des ressources Web ===
:: Copie du contenu webapp avec la nouvelle structure
xcopy /E /I /Y "%PROJECT_ROOT%\src\main\webapp\*" "%BUILD_DIR%"

:: Copie des bibliothèques
if not exist "%BUILD_DIR%\WEB-INF\lib" mkdir "%BUILD_DIR%\WEB-INF\lib"
xcopy /E /I /Y "%PROJECT_ROOT%\lib\*.jar" "%BUILD_DIR%\WEB-INF\lib\"
:: Suppression de servlet-api.jar du WAR
if exist "%BUILD_DIR%\WEB-INF\lib\servlet-api.jar" del "%BUILD_DIR%\WEB-INF\lib\servlet-api.jar"

echo === Création du WAR ===
cd "%BUILD_DIR%"
jar -cvf "%WEBAPP_NAME%.war" *

echo === Déploiement vers Tomcat ===
:: Suppression de l'ancienne version si elle existe
if exist "%TOMCAT_WEBAPPS%\%WEBAPP_NAME%.war" del "%TOMCAT_WEBAPPS%\%WEBAPP_NAME%.war"
if exist "%TOMCAT_WEBAPPS%\%WEBAPP_NAME%" rd /s /q "%TOMCAT_WEBAPPS%\%WEBAPP_NAME%"

:: Copie du nouveau WAR
copy "%WEBAPP_NAME%.war" "%TOMCAT_WEBAPPS%"

echo === Déploiement terminé ===
echo.
echo Accédez à votre application sur: http://localhost:8888/%WEBAPP_NAME%
echo.
cd "%PROJECT_ROOT%"
