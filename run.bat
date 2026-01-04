@echo off
REM Update the path below to your Maven installation
REM Example: "C:\Program Files\JetBrains\IntelliJ IDEA ...\bin\mvn.cmd"
SET MAVEN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.2\plugins\maven\lib\maven3\bin\mvn.cmd"

IF NOT EXIST %MAVEN_CMD% (
    echo Maven not found at %MAVEN_CMD%
    echo Please edit run.bat to set the correct MAVEN_CMD
    echo OR run 'mvn clean javafx:run' if maven is in your PATH.
    pause
    exit /b
)

%MAVEN_CMD% clean javafx:run
