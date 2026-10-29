# Signal Desk

Signal Desk is a Java-integrated incident investigation dashboard for analyzing application logs, reconstructing timelines, identifying probable root causes, showing affected services, and suggesting next steps.

## Features

- Calm internal engineering dashboard
- Incident monitoring and history
- `.log` file upload and sample logs
- Java rule-based log analysis
- Timeline, evidence, service impact, and recommendations
- Resolve incident workflow
- Spring Boot REST API with browser fallback
- Responsive UI

## Project structure

```text
Signal-Desk/
|- index.html
|- style.css
|- script.js
|- pom.xml
|- src/main/java/com/signaldesk/
|  |- SignalDeskApplication.java
|  |- controller/IncidentController.java
|  |- model/
|  `- service/IncidentService.java
`- README.md
```

## Run the Java backend

The project includes a zero-dependency Java server. It uses the JDK's built-in HTTP server, so Maven is not required.

Double-click `run-java-server.bat`, or run this from PowerShell:

```powershell
.\run-java-server.bat
```

The script compiles the Java files with `javac` and starts the server with `java`.

The server and dashboard are available at `http://localhost:8080`.

Open `http://localhost:8080` in a browser after starting the backend. The dashboard loads incidents from Java, log analysis uses `POST /api/incidents/analyze`, and resolving an incident uses `PATCH /api/incidents/{id}/resolve`.

If the backend is stopped, the UI automatically falls back to browser demo data and local storage.

The Spring Boot files remain included as a standard Maven version for environments that do have Maven. The standalone server is the recommended demo path for this project.

## OS Lab: CPU scheduling simulator

The **OS Lab** navigation item provides an independent CPU scheduling practical. Enter processes with an ID, arrival time, burst time, and priority, then run or compare:

- FCFS (First Come, First Served)
- SJF (non-preemptive Shortest Job First)
- Priority scheduling (a smaller number means higher priority)
- Round Robin (preemptive, with a configurable positive time quantum)

The simulator calculates completion, turnaround, waiting, and response times, as well as average metrics, busy time, elapsed time, and CPU utilization. It also renders a proportional Gantt chart, including idle intervals, and includes an algorithm guide suitable for a viva or practical demonstration.

The standalone API endpoints are:

```text
POST /api/os/scheduling/run
POST /api/os/scheduling/compare
```

Example request:

```json
{
  "algorithm": "ROUND_ROBIN",
  "timeQuantum": 2,
  "processes": [
    {"pid": "P1", "arrivalTime": 0, "burstTime": 5, "priority": 2},
    {"pid": "P2", "arrivalTime": 1, "burstTime": 3, "priority": 1}
  ]
}
```

The scheduling engine is kept separate from HTTP routing in `src/main/java/com/signaldesk/os`, so the algorithms can be tested without starting the server.