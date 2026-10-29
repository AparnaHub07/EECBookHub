@echo off
cd /d "%~dp0"
echo Starting EEC BookHub...
mvn spring-boot:run
pause
