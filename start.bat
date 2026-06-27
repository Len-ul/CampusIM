@echo off
chcp 65001 >nul
cd /d D:\CampusIM
echo Starting server...
start "CampusIM Server" cmd /k "mvn exec:java@run-server"
timeout /t 3 /nobreak
::echo Starting server...
::start "CampusIM Admin" cmd /k "mvn exec:java@run-admin"
::timeout /t 3 /nobreak
echo Starting clients...
start "CampusIM Client 1" cmd /k "mvn javafx:run"
timeout /t 3 /nobreak
echo Starting clients...
start "CampusIM Client 2" cmd /k "mvn javafx:run"
::timeout /t 3 /nobreak
::echo Starting clients...
::start "CampusIM Client 3" cmd /k "mvn javafx:run"
echo All terminals started
