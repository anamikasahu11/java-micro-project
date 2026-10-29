@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\main\java\com\signaldesk\model\Incident.java src\main\java\com\signaldesk\model\ServiceStatus.java src\main\java\com\signaldesk\model\TimelineEvent.java src\main\java\com\signaldesk\os\*.java src\main\java\com\signaldesk\StandaloneSignalDeskServer.java
if errorlevel 1 (
  echo Java compilation failed.
  exit /b 1
)
java -cp out com.signaldesk.StandaloneSignalDeskServer
