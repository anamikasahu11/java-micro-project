/* =========================================================
   AI INCIDENT INVESTIGATOR
   Browser UI with optional Spring Boot backend
========================================================= */

/* =========================================================
   DEMO DATA
========================================================= */

const DEMO_INCIDENTS = [

    {
        id: 1001,

        title: "RDS Connection Failure",

        severity: "HIGH",

        status: "INVESTIGATING",

        rootCause:
            "RDS database connection pool exhaustion",

        confidence: 87,

        createdAt:
            "2026-08-28 10:32:10",

        timeline: [

            {
                time: "10:31:42",
                service: "RDS",
                message:
                    "Connection timeout while connecting to database"
            },

            {
                time: "10:31:44",
                service: "RDS",
                message:
                    "Connection pool exhausted"
            },

            {
                time: "10:31:47",
                service: "Order-Service",
                message:
                    "API response time exceeded 5000ms"
            },

            {
                time: "10:31:52",
                service: "Order-Service",
                message:
                    "Database connection failed"
            },

            {
                time: "10:31:56",
                service: "API-Gateway",
                message:
                    "Upstream service timeout"
            },

            {
                time: "10:32:01",
                service: "Monitoring",
                message:
                    "Order Service health check failed"
            }

        ],

        affectedServices: [

            {
                name: "RDS",
                status: "CRITICAL"
            },

            {
                name: "Order Service",
                status: "CRITICAL"
            },

            {
                name: "API Gateway",
                status: "DEGRADED"
            },

            {
                name: "Authentication Service",
                status: "HEALTHY"
            }

        ],

        evidence: [

            "[RDS] Connection pool exhausted",

            "[Order-Service] Database connection failed",

            "[Order-Service] API response time exceeded 5000ms",

            "[API-Gateway] Upstream service timeout",

            "[Monitoring] Order Service health check failed"

        ],

        recommendations: [

            "Increase the RDS database connection pool size.",

            "Investigate long-running or leaked database connections.",

            "Review Order Service database connection lifecycle.",

            "Configure connection pool timeout and retry policies.",

            "Monitor active database connections and pool utilization."

        ]

    },

    {
        id: 1002,

        title: "API Latency Spike",

        severity: "MEDIUM",

        status: "INVESTIGATING",

        rootCause:
            "Database query slowdown causing excessive API latency",

        confidence: 82,

        createdAt:
            "2026-08-28 09:15:42",

        timeline: [

            {
                time: "09:14:20",
                service: "API",
                message:
                    "Average response time increased above 2000ms"
            },

            {
                time: "09:14:25",
                service: "Order-Service",
                message:
                    "Slow database query detected"
            },

            {
                time: "09:14:31",
                service: "API",
                message:
                    "Request latency exceeded threshold"
            },

            {
                time: "09:14:39",
                service: "Monitoring",
                message:
                    "API latency alert triggered"
            }

        ],

        affectedServices: [

            {
                name: "API Gateway",
                status: "DEGRADED"
            },

            {
                name: "Order Service",
                status: "DEGRADED"
            },

            {
                name: "Database",
                status: "CRITICAL"
            },

            {
                name: "Authentication Service",
                status: "HEALTHY"
            }

        ],

        evidence: [

            "[API] Average response time increased above 2000ms",

            "[Order-Service] Slow database query detected",

            "[API] Request latency exceeded threshold"

        ],

        recommendations: [

            "Identify slow database queries.",

            "Review database indexes.",

            "Check database CPU and memory utilization.",

            "Review API timeout configuration.",

            "Enable query performance monitoring."

        ]

    },

    {
        id: 1003,

        title: "Service Deployment Failure",

        severity: "HIGH",

        status: "RESOLVED",

        rootCause:
            "Application configuration and environment variable mismatch",

        confidence: 91,

        createdAt:
            "2026-08-27 16:42:18",

        timeline: [

            {
                time: "16:40:12",
                service: "Deployment",
                message:
                    "New application version deployment started"
            },

            {
                time: "16:40:29",
                service: "Order-Service",
                message:
                    "Application startup failed"
            },

            {
                time: "16:40:31",
                service: "Order-Service",
                message:
                    "Required environment variable DATABASE_URL missing"
            },

            {
                time: "16:40:38",
                service: "Monitoring",
                message:
                    "Deployment health check failed"
            }

        ],

        affectedServices: [

            {
                name: "Order Service",
                status: "CRITICAL"
            },

            {
                name: "API Gateway",
                status: "DEGRADED"
            },

            {
                name: "Authentication Service",
                status: "HEALTHY"
            }

        ],

        evidence: [

            "[Order-Service] Application startup failed",

            "[Order-Service] Required environment variable DATABASE_URL missing",

            "[Monitoring] Deployment health check failed"

        ],

        recommendations: [

            "Verify deployment environment variables.",

            "Validate application configuration before deployment.",

            "Add environment variable validation during startup.",

            "Improve deployment health checks.",

            "Use configuration templates to prevent missing variables."

        ]

    }

];

/* =========================================================
   STORAGE
========================================================= */

let incidents = [];
const API_BASE_URL = "http://localhost:8080/api";

async function loadIncidents() {

    try {

        const response = await fetch(
            `${API_BASE_URL}/incidents`
        );

        if (!response.ok) {
            throw new Error(`Backend returned ${response.status}`);
        }

        const backendIncidents = await response.json();

        if (backendIncidents.length) {
            incidents = backendIncidents.map(function (backendIncident) {

                const browserDetail =
                    DEMO_INCIDENTS.find(
                        demoIncident =>
                            Number(demoIncident.id) === Number(backendIncident.id)
                    );

                return browserDetail
                    ? {
                        ...browserDetail,
                        ...backendIncident,
                        timeline:
                            backendIncident.timeline.length
                                ? backendIncident.timeline
                                : browserDetail.timeline,
                        affectedServices:
                            backendIncident.affectedServices.length
                                ? backendIncident.affectedServices
                                : browserDetail.affectedServices,
                        evidence:
                            backendIncident.evidence.length
                                ? backendIncident.evidence
                                : browserDetail.evidence,
                        recommendations:
                            backendIncident.recommendations.length
                                ? backendIncident.recommendations
                                : browserDetail.recommendations
                    }
                    : backendIncident;
            });

            saveIncidents();
            return;
        }

    } catch (error) {

        console.info(
            "Signal Desk backend is unavailable; using browser demo data.",
            error
        );

    }

    const stored =
        localStorage.getItem(
            "aiIncidentInvestigatorIncidents"
        );

    if (stored) {

        try {

            incidents = JSON.parse(stored);

        } catch {

            incidents = [...DEMO_INCIDENTS];

        }

    } else {

        incidents = [...DEMO_INCIDENTS];

        saveIncidents();

    }

}

function saveIncidents() {

    localStorage.setItem(

        "aiIncidentInvestigatorIncidents",

        JSON.stringify(incidents)

    );

}

/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        await loadIncidents();

        showPage("dashboard");

    }
);

/* =========================================================
   PAGE NAVIGATION
========================================================= */

function showPage(page) {

    const pages = [

        "dashboard",
        "incidents",
        "analyze",
        "history",
        "os-lab",
        "incident"

    ];

    pages.forEach(function (name) {

        const element =
            document.getElementById(
                name + "-page"
            );

        if (element) {

            element.classList.add("hidden");

        }

    });

    const target =
        document.getElementById(
            page + "-page"
        );

    if (target) {

        target.classList.remove("hidden");

    }

    document.querySelectorAll(".nav-item")
        .forEach(function (button) {

            button.classList.remove("active");

        });

    const nav =
        document.getElementById(
            "nav-" + page
        );

    if (nav) {

        nav.classList.add("active");

    }

    if (page === "dashboard") {

        setPageTitle(
            "Dashboard",
            "A calm view of what needs your attention"
        );

        renderDashboard();

    }

    if (page === "incidents") {

        setPageTitle(
            "Incidents",
            "The issues your team is currently looking into"
        );

        renderAllIncidents();

    }

    if (page === "analyze") {

        setPageTitle(
            "Analyze Logs",
            "Share a few logs and we will help connect the dots"
        );

    }

    if (page === "history") {

        setPageTitle(
            "History",
            "A record of the incidents you have worked through"
        );

        renderHistory();

    }

    if (page === "os-lab") {

        setPageTitle(
            "OS Lab",
            "Learn how scheduling policies change CPU performance"
        );

        if (!document.querySelector("#os-processes tr")) {
            loadOsSample(false);
        }

    }

    if (page === "incident") {

        setPageTitle(
            "Investigation",
            "Incident investigation details"
        );

    }

}

/* =========================================================
       CPU SCHEDULING LAB
    ========================================================= */

    const OS_SAMPLE = [
        { pid: "P1", arrivalTime: 5, burstTime: 2, priority: 2 },
        { pid: "P2", arrivalTime: 0, burstTime: 3, priority: 1 },
        { pid: "P3", arrivalTime: 1, burstTime: 2, priority: 3 },
        { pid: "P4", arrivalTime: 2, burstTime: 4, priority: 2 }
    ];

    const OS_IDLE_SAMPLE = [
        { pid: "P1", arrivalTime: 3, burstTime: 4, priority: 2 },
        { pid: "P2", arrivalTime: 8, burstTime: 2, priority: 1 },
        { pid: "P3", arrivalTime: 10, burstTime: 3, priority: 3 }
    ];

    function loadOsSample(includeIdle) {
        const processes = includeIdle ? OS_IDLE_SAMPLE : OS_SAMPLE;
        document.getElementById("os-processes").innerHTML = "";
        processes.forEach(function (process) {
            addOsProcess(process);
        });
        document.getElementById("os-quantum").value = 2;
        toggleQuantum();
        clearOsMessage();
    }

    function addOsProcess(process) {
        const value = process || { pid: "", arrivalTime: 0, burstTime: 1, priority: 1 };
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><input class="os-pid" value="${escapeHtml(String(value.pid))}" aria-label="Process ID"></td>
            <td><input class="os-arrival" type="number" min="0" value="${value.arrivalTime}" aria-label="Arrival time"></td>
            <td><input class="os-burst" type="number" min="1" value="${value.burstTime}" aria-label="Burst time"></td>
            <td><input class="os-priority" type="number" value="${value.priority}" aria-label="Priority"></td>
            <td><button class="remove-process" type="button" onclick="this.closest('tr').remove()">Remove</button></td>`;
        document.getElementById("os-processes").appendChild(row);
    }

    function clearOsProcesses() {
        document.getElementById("os-processes").innerHTML = "";
        document.getElementById("os-results").classList.add("hidden");
        clearOsMessage();
    }

    function toggleQuantum() {
        document.getElementById("quantum-wrap").classList.toggle(
            "hidden",
            document.getElementById("os-algorithm").value !== "ROUND_ROBIN"
        );
    }

    function readOsProcesses() {
        const rows = Array.from(document.querySelectorAll("#os-processes tr"));
        if (!rows.length) {
            throw new Error("Add at least one process before running a simulation.");
        }
        const seen = new Set();
        return rows.map(function (row, index) {
            const pid = row.querySelector(".os-pid").value.trim();
            const arrivalTime = Number(row.querySelector(".os-arrival").value);
            const burstTime = Number(row.querySelector(".os-burst").value);
            const priority = Number(row.querySelector(".os-priority").value);
            if (!pid) throw new Error(`Process ${index + 1}: process ID is required.`);
            if (seen.has(pid)) throw new Error(`Process ID "${pid}" is duplicated.`);
            if (!Number.isInteger(arrivalTime) || arrivalTime < 0) throw new Error(`${pid}: arrival time must be a non-negative integer.`);
            if (!Number.isInteger(burstTime) || burstTime <= 0) throw new Error(`${pid}: burst time must be a positive integer.`);
            if (!Number.isInteger(priority)) throw new Error(`${pid}: priority must be an integer.`);
            seen.add(pid);
            return { pid, arrivalTime, burstTime, priority };
        });
    }

    async function runOsSimulation() {
        try {
            const processes = readOsProcesses();
            const algorithm = document.getElementById("os-algorithm").value;
            const quantum = Number(document.getElementById("os-quantum").value);
            if (algorithm === "ROUND_ROBIN" && (!Number.isInteger(quantum) || quantum <= 0)) {
                throw new Error("Time quantum must be a positive integer.");
            }
            clearOsMessage();
            const response = await fetch(`${API_BASE_URL}/os/scheduling/run`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ algorithm, processes, timeQuantum: quantum })
            });
            if (!response.ok) {
                const error = await response.json().catch(function () { return {}; });
                throw new Error(error.error || `Scheduling request failed (${response.status}).`);
            }
            const result = await response.json();
            const comparisonResponse = await fetch(`${API_BASE_URL}/os/scheduling/compare`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ processes, timeQuantum: quantum })
            });
            if (!comparisonResponse.ok) throw new Error("The selected simulation ran, but comparison data was unavailable.");
            renderOsResults(result, await comparisonResponse.json());
        } catch (error) {
            showOsMessage(error.message || "Unable to run the simulation.");
        }
    }

    function renderOsResults(result, comparison) {
        document.getElementById("os-results").classList.remove("hidden");
        const metric = result.metrics || [];
        document.getElementById("os-summary").innerHTML = [
            ["Processes", result.totalProcesses],
            ["Busy time", result.totalBusyTime],
            ["Elapsed time", result.totalElapsedTime],
            ["CPU utilization", `${Number(result.cpuUtilization).toFixed(2)}%`],
            ["Average WT", Number(result.averageWaitingTime).toFixed(2)],
            ["Average TAT", Number(result.averageTurnaroundTime).toFixed(2)],
            ["Average RT", Number(result.averageResponseTime).toFixed(2)]
        ].map(function (item) { return `<div class="stat-card"><div class="stat-label">${item[0]}</div><div class="stat-value">${item[1]}</div></div>`; }).join("");
        document.getElementById("os-metrics").innerHTML = metric.map(function (item) {
            return `<tr><td>${escapeHtml(item.pid)}</td><td>${item.arrivalTime}</td><td>${item.burstTime}</td><td>${item.priority}</td><td>${item.completionTime}</td><td>${item.turnaroundTime}</td><td>${item.waitingTime}</td><td>${item.responseTime}</td></tr>`;
        }).join("");
        renderGantt(result.segments || []);
        const rows = comparison.results || [];
        const minWt = Math.min.apply(null, rows.map(function (item) { return item.averageWaitingTime; }));
        const minTat = Math.min.apply(null, rows.map(function (item) { return item.averageTurnaroundTime; }));
        document.getElementById("os-comparison").innerHTML = rows.map(function (item) {
            const best = item.averageWaitingTime === minWt || item.averageTurnaroundTime === minTat;
            return `<tr class="${best ? "best-row" : ""}"><td>${escapeHtml(item.algorithm)}</td><td>${Number(item.averageWaitingTime).toFixed(2)}${item.averageWaitingTime === minWt ? " *" : ""}</td><td>${Number(item.averageTurnaroundTime).toFixed(2)}${item.averageTurnaroundTime === minTat ? " *" : ""}</td><td>${Number(item.averageResponseTime).toFixed(2)}</td><td>${Number(item.cpuUtilization).toFixed(2)}%</td></tr>`;
        }).join("");
        const winners = [];
        if (rows.length) {
            winners.push(`Lowest average waiting time: ${rows.filter(function (item) { return item.averageWaitingTime === minWt; }).map(function (item) { return item.algorithm; }).join(", ")}.`);
            winners.push(`Lowest average turnaround time: ${rows.filter(function (item) { return item.averageTurnaroundTime === minTat; }).map(function (item) { return item.algorithm; }).join(", ")}.`);
            winners.push(`Round Robin produced ${result.segments.filter(function (segment) { return segment.pid !== "IDLE"; }).length} CPU execution segment(s) in the selected run.`);
            if (result.segments.some(function (segment) { return segment.pid === "IDLE"; })) {
                winners.push("The CPU was idle during at least one interval because no process had arrived.");
            }
        }
        document.getElementById("os-explanation").textContent = winners.join(" ");
    }

    function renderGantt(segments) {
        const chart = document.getElementById("os-gantt");
        if (!segments.length) {
            chart.innerHTML = "<p>No execution segments returned.</p>";
            return;
        }
        const total = segments[segments.length - 1].endTime;
        const scale = segments.map(function (segment) { return segment.startTime; }).concat([total]);
        chart.innerHTML = `<div class="gantt-track">${segments.map(function (segment) {
            const width = Math.max(5, (segment.duration / total) * 100);
            return `<div class="gantt-segment ${segment.pid === "IDLE" ? "idle" : ""}" style="width:${width}%"><strong>${escapeHtml(segment.pid)}</strong><small>${segment.startTime}-${segment.endTime}</small></div>`;
        }).join("")}</div><div class="gantt-scale">${scale.map(function (time) { return `<span>${time}</span>`; }).join("")}</div>`;
    }

    function showOsMessage(message) {
        const element = document.getElementById("os-error");
        element.textContent = message;
        element.classList.remove("hidden");
    }

    function clearOsMessage() {
        document.getElementById("os-error").classList.add("hidden");
    }

function setPageTitle(title, subtitle) {

    document.getElementById(
        "page-title"
    ).textContent = title;

    document.getElementById(
        "page-subtitle"
    ).textContent = subtitle;

}

/* =========================================================
   DASHBOARD
========================================================= */

function renderDashboard() {

    const active =
        incidents.filter(
            incident =>
                incident.status !== "RESOLVED"
        ).length;

    const critical =
        incidents.filter(
            incident =>
                incident.severity === "CRITICAL"
        ).length;

    const investigating =
        incidents.filter(
            incident =>
                incident.status === "INVESTIGATING"
        ).length;

    const resolved =
        incidents.filter(
            incident =>
                incident.status === "RESOLVED"
        ).length;

    document.getElementById(
        "active-count"
    ).textContent = active;

    document.getElementById(
        "critical-count"
    ).textContent = critical;

    document.getElementById(
        "investigating-count"
    ).textContent = investigating;

    document.getElementById(
        "resolved-count"
    ).textContent = resolved;

    const activeIncidents =
        incidents.filter(
            incident =>
                incident.status !== "RESOLVED"
        );

    const container =
        document.getElementById(
            "dashboard-incidents"
        );

    if (!activeIncidents.length) {

        container.innerHTML = emptyState(
            "No active incidents",
            "Everything currently looks healthy."
        );

        return;

    }

    container.innerHTML =
        activeIncidents
            .slice(0, 6)
            .map(createIncidentCard)
            .join("");

}

/* =========================================================
   INCIDENT CARDS
========================================================= */

function createIncidentCard(incident) {

    const severity =
        String(
            incident.severity || "LOW"
        ).toLowerCase();

    return `

        <div class="incident-card"
             onclick="openIncident(${incident.id})">

            <div class="incident-top">

                <span class="incident-id">
                    INC-${incident.id}
                </span>

                <span class="badge ${severity}">
                    ${incident.severity}
                </span>

            </div>

            <h4>
                ${escapeHtml(incident.title)}
            </h4>

            <div class="root-cause">

                ${escapeHtml(
                    incident.rootCause
                )}

            </div>

            <div class="incident-meta">

                <span>
                    ${incident.status}
                </span>

                <span>
                    ${formatDate(
                        incident.createdAt
                    )}
                </span>

            </div>

        </div>

    `;

}

/* =========================================================
   ALL INCIDENTS
========================================================= */

function renderAllIncidents() {

    const container =
        document.getElementById(
            "all-incidents"
        );

    if (!incidents.length) {

        container.innerHTML =
            emptyState(
                "No incidents",
                "Start a new investigation by analyzing logs."
            );

        return;

    }

    container.innerHTML =
        incidents
            .map(createIncidentCard)
            .join("");

}

/* =========================================================
   HISTORY
========================================================= */

function renderHistory() {

    const tbody =
        document.getElementById(
            "history-table"
        );

    tbody.innerHTML =
        incidents
            .map(function (incident) {

                const severity =
                    String(
                        incident.severity
                    ).toLowerCase();

                const status =
                    incident.status ===
                    "RESOLVED"
                        ? "resolved"
                        : "";

                return `

                    <tr>

                        <td>
                            INC-${incident.id}
                        </td>

                        <td>
                            ${escapeHtml(
                                incident.title
                            )}
                        </td>

                        <td>

                            <span class="badge ${severity}">
                                ${incident.severity}
                            </span>

                        </td>

                        <td>
                            ${escapeHtml(
                                incident.rootCause
                            )}
                        </td>

                        <td>

                            <span class="
                                badge
                                ${status}
                            ">

                                ${incident.status}

                            </span>

                        </td>

                        <td>
                            ${formatDate(
                                incident.createdAt
                            )}
                        </td>

                        <td>

                            <button
                                class="table-action"
                                onclick="openIncident(${incident.id})">

                                Open

                            </button>

                        </td>

                    </tr>

                `;

            })
            .join("");

}

/* =========================================================
   OPEN INCIDENT
========================================================= */

function openIncident(id) {

    const incident =
        incidents.find(
            item =>
                Number(item.id) === Number(id)
        );

    if (!incident) {

        alert(
            "Incident could not be found."
        );

        return;

    }

    renderIncidentDetail(incident);

    showPage("incident");

}

/* =========================================================
   INVESTIGATION DETAIL
========================================================= */

function renderIncidentDetail(incident) {

    const container =
        document.getElementById(
            "incident-detail"
        );

    const resolved =
        incident.status === "RESOLVED";

    container.innerHTML = `

        <button
            class="back-button"
            onclick="showPage('dashboard')">

            &lt;- Back to open incidents

        </button>

        <div class="investigation-header">

            <div class="investigation-title">

                <h1>
                    ${escapeHtml(
                        incident.title
                    )}
                </h1>

                <div class="investigation-subtitle">

                    <span class="
                        badge
                        ${String(
                            incident.severity
                        ).toLowerCase()}
                    ">

                        ${incident.severity}

                    </span>

                    <span class="dot-separator">
                        |
                    </span>

                    <span class="status-label"
                          style="
                            color:
                            ${resolved
                                ? "var(--green)"
                                : "var(--yellow)"};
                          ">

                        ${incident.status}

                    </span>

                    <span class="dot-separator">
                        |
                    </span>

                    <span class="incident-id">
                        INC-${incident.id}
                    </span>

                </div>

            </div>

            ${
                resolved
                    ? ""

                    : `

                        <button
                            class="resolve-button"
                            onclick="resolveIncident(${incident.id})">

                            Mark as Resolved

                        </button>

                    `
            }

        </div>

        <!-- ROOT CAUSE -->

        <div class="root-cause-panel">

            <div class="root-cause-label">
                LIKELY CAUSE
            </div>

            <div class="root-cause-text">

                ${escapeHtml(
                    incident.rootCause
                )}

            </div>

            <div class="confidence">

                <div class="confidence-row">

                    <span>
                        Confidence
                    </span>

                    <span class="confidence-value">

                        ${incident.confidence}%

                    </span>

                </div>

                <div class="confidence-bar">

                    <div
                        class="confidence-fill"
                        style="
                            width:
                            ${incident.confidence}%;
                        ">
                    </div>

                </div>

            </div>

        </div>

        <!-- INVESTIGATION GRID -->

        <div class="investigation-grid">

            <!-- TIMELINE -->

            <div class="panel">

                <div class="panel-header">

                    <h3>
                        Incident Timeline
                    </h3>

                    <p>
                        Chronological sequence of detected events
                    </p>

                </div>

                <div class="timeline">

                    ${
                        incident.timeline
                            .map(function (item) {

                                return `

                                    <div
                                        class="timeline-item">

                                        <div
                                            class="timeline-dot">
                                        </div>

                                        <div>

                                            <span
                                                class="timeline-time">

                                                ${escapeHtml(
                                                    item.time
                                                )}

                                            </span>

                                            <span
                                                class="timeline-service">

                                                ${escapeHtml(
                                                    item.service
                                                )}

                                            </span>

                                        </div>

                                        <div
                                            class="timeline-message">

                                            ${escapeHtml(
                                                item.message
                                            )}

                                        </div>

                                    </div>

                                `;

                            })
                            .join("")
                    }

                </div>

            </div>

            <!-- SERVICES -->

            <div class="panel">

                <div class="panel-header">

                    <h3>
                        Services involved
                    </h3>

                    <p>
                        Current service impact
                    </p>

                </div>

                <div class="service-list">

                    ${
                        incident.affectedServices
                            .map(function (service) {

                                return `

                                    <div
                                        class="service-row">

                                        <span
                                            class="service-name">

                                            ${escapeHtml(
                                                service.name
                                            )}

                                        </span>

                                        <span
                                            class="
                                                service-status
                                                ${String(
                                                    service.status
                                                ).toLowerCase()}
                                            ">

                                            ${service.status}

                                        </span>

                                    </div>

                                `;

                            })
                            .join("")
                    }

                </div>

            </div>

            <!-- EVIDENCE -->

            <div class="panel">

                <div class="panel-header">

                    <h3>
                        What we found
                    </h3>

                    <p>
                        Log signals supporting the analysis
                    </p>

                </div>

                <div class="evidence-list">

                    ${
                        incident.evidence
                            .map(function (evidence) {

                                return `

                                    <div
                                        class="evidence-item">

                                        ${escapeHtml(
                                            evidence
                                        )}

                                    </div>

                                `;

                            })
                            .join("")
                    }

                </div>

            </div>

            <!-- RECOMMENDATIONS -->

            <div class="panel">

                <div class="panel-header">

                    <h3>
                        Suggested next steps
                    </h3>

                    <p>
                        Suggested remediation steps
                    </p>

                </div>

                <div class="recommendations">

                    ${
                        incident.recommendations
                            .map(function (
                                recommendation,
                                index
                            ) {

                                return `

                                    <div
                                        class="recommendation">

                                        <div
                                            class="recommendation-number">

                                            ${index + 1}

                                        </div>

                                        <p>

                                            ${escapeHtml(
                                                recommendation
                                            )}

                                        </p>

                                    </div>

                                `;

                            })
                            .join("")
                    }

                </div>

            </div>

            <!-- LOGS -->

            ${
                incident.rawLogs
                    ? `

                        <div class="panel full-width">

                            <div class="panel-header">

                                <h3>
                                    Submitted Logs
                                </h3>

                            </div>

                            <pre style="
                                margin:0;
                                white-space:pre-wrap;
                                font-family:monospace;
                                font-size:11px;
                                color:#aebdce;
                                line-height:1.7;
                            ">${escapeHtml(
                                incident.rawLogs
                            )}</pre>

                        </div>

                    `
                    : ""
            }

        </div>

    `;

}

/* =========================================================
   RESOLVE INCIDENT
========================================================= */

async function resolveIncident(id) {

    const incident =
        incidents.find(
            item =>
                Number(item.id) === Number(id)
        );

    if (!incident) {

        return;

    }

    incident.status = "RESOLVED";

    saveIncidents();

    renderIncidentDetail(incident);

    renderDashboard();

    try {

        const response = await fetch(
            `${API_BASE_URL}/incidents/${id}/resolve`,
            {
                method: "PATCH"
            }
        );

        if (!response.ok) {
            throw new Error(`Backend returned ${response.status}`);
        }

        const resolvedIncident = await response.json();
        const index = incidents.findIndex(
            item => Number(item.id) === Number(id)
        );

        if (index !== -1) {
            incidents[index] = resolvedIncident;
            saveIncidents();
            renderIncidentDetail(resolvedIncident);
            renderDashboard();
        }

    } catch (error) {

        console.info(
            "Backend resolve request failed; browser state was updated.",
            error
        );

    }

}

/* =========================================================
   LOG INPUT
========================================================= */

function loadSampleLogs() {

    document.getElementById(
        "log-input"
    ).value = `[10:31:42] [RDS] Connection timeout while connecting to database
[10:31:44] [RDS] Connection pool exhausted
[10:31:47] [Order-Service] API response time exceeded 5000ms
[10:31:52] [Order-Service] Database connection failed
[10:31:56] [API-Gateway] Upstream service timeout
[10:32:01] [Monitoring] Order Service health check failed`;

}

function clearLogs() {

    document.getElementById(
        "log-input"
    ).value = "";

}

function handleFileUpload(event) {

    const file =
        event.target.files[0];

    if (!file) {

        return;

    }

    if (
        !file.name.endsWith(".log") &&
        !file.name.endsWith(".txt")
    ) {

        alert(
            "Please upload a .log or .txt file."
        );

        return;

    }

    const reader =
        new FileReader();

    reader.onload =
        function (event) {

            document.getElementById(
                "log-input"
            ).value =
                event.target.result;

        };

    reader.onerror =
        function () {

            alert(
                "Unable to read the selected file."
            );

        };

    reader.readAsText(file);

}

/* =========================================================
   LOG PARSER
========================================================= */

function parseLogs(text) {

    const lines =
        text
            .split(/\r?\n/)
            .map(line => line.trim())
            .filter(Boolean);

    const parsed = [];

    const pattern =
        /^\[([^\]]+)\]\s*\[([^\]]+)\]\s*(.+)$/;

    lines.forEach(function (line) {

        const match =
            line.match(pattern);

        if (!match) {

            return;

        }

        parsed.push({

            time: match[1],

            service: match[2],

            message: match[3],

            raw: line

        });

    });

    return parsed;

}

/* =========================================================
   ANALYSIS ENGINE
========================================================= */

function analyzeIncident(parsed, rawLogs) {

    const combined =
        parsed
            .map(item => item.message)
            .join(" ")
            .toLowerCase();

    let result;

    /* -----------------------------------------------------
       RDS CONNECTION POOL
    ----------------------------------------------------- */

    if (
        combined.includes(
            "connection pool exhausted"
        ) ||
        combined.includes(
            "pool exhaustion"
        )
    ) {

        result = {

            title:
                "RDS Connection Failure",

            severity:
                "HIGH",

            rootCause:
                "RDS database connection pool exhaustion",

            confidence:
                87,

            affectedServices:
                [

                    {
                        name: "RDS",
                        status: "CRITICAL"
                    },

                    {
                        name: "Order Service",
                        status: "CRITICAL"
                    },

                    {
                        name: "API Gateway",
                        status: "DEGRADED"
                    },

                    {
                        name: "Authentication Service",
                        status: "HEALTHY"
                    }

                ],

            evidence:
                parsed
                    .filter(item =>
                        /pool|connection|timeout|failed/i
                            .test(item.message)
                    )
                    .map(item =>
                        `[${item.service}] ${item.message}`
                    ),

            recommendations:
                [

                    "Increase the RDS database connection pool size.",

                    "Investigate long-running or leaked database connections.",

                    "Review Order Service database connection lifecycle.",

                    "Configure connection pool timeout and retry policies.",

                    "Monitor active database connections and pool utilization."

                ]

        };

    }

    /* -----------------------------------------------------
       DEPLOYMENT FAILURE
    ----------------------------------------------------- */

    else if (

        /environment variable|startup failed|deployment failed|missing configuration/i
            .test(combined)

    ) {

        result = {

            title:
                "Service Deployment Failure",

            severity:
                "HIGH",

            rootCause:
                "Application configuration and environment variable mismatch",

            confidence:
                91,

            affectedServices:
                [

                    {
                        name: "Order Service",
                        status: "CRITICAL"
                    },

                    {
                        name: "API Gateway",
                        status: "DEGRADED"
                    },

                    {
                        name: "Authentication Service",
                        status: "HEALTHY"
                    }

                ],

            evidence:
                parsed
                    .map(item =>
                        `[${item.service}] ${item.message}`
                    ),

            recommendations:
                [

                    "Verify deployment environment variables.",

                    "Validate application configuration before deployment.",

                    "Add environment variable validation during startup.",

                    "Improve deployment health checks.",

                    "Use configuration templates to prevent missing variables."

                ]

        };

    }

    /* -----------------------------------------------------
       API LATENCY
    ----------------------------------------------------- */

    else if (

        /slow query|latency|response time|slow database|query slowdown/i
            .test(combined)

    ) {

        result = {

            title:
                "API Latency Spike",

            severity:
                "MEDIUM",

            rootCause:
                "Database query slowdown causing excessive API latency",

            confidence:
                82,

            affectedServices:
                [

                    {
                        name: "API Gateway",
                        status: "DEGRADED"
                    },

                    {
                        name: "Order Service",
                        status: "DEGRADED"
                    },

                    {
                        name: "Database",
                        status: "CRITICAL"
                    },

                    {
                        name: "Authentication Service",
                        status: "HEALTHY"
                    }

                ],

            evidence:
                parsed
                    .map(item =>
                        `[${item.service}] ${item.message}`
                    ),

            recommendations:
                [

                    "Identify slow database queries.",

                    "Review database indexes.",

                    "Check database CPU and memory utilization.",

                    "Review API timeout configuration.",

                    "Enable query performance monitoring."

                ]

        };

    }

    /* -----------------------------------------------------
       UNKNOWN INCIDENT
    ----------------------------------------------------- */

    else {

        result = {

            title:
                "Application Incident",

            severity:
                "MEDIUM",

            rootCause:
                "Unable to determine a specific root cause from the submitted logs",

            confidence:
                55,

            affectedServices:
                parsed.length
                    ? parsed.map(function (item) {

                        return {

                            name: item.service,

                            status: "DEGRADED"

                        };

                    })

                    : [

                        {
                            name: "Unknown Service",
                            status: "DEGRADED"
                        }

                    ],

            evidence:
                parsed
                    .map(item =>
                        `[${item.service}] ${item.message}`
                    ),

            recommendations:
                [

                    "Collect additional application logs.",

                    "Review service health metrics.",

                    "Check recent configuration or deployment changes.",

                    "Investigate correlated errors across services.",

                    "Run the investigation again with a larger log window."

                ]

        };

    }

    return {

        ...result,

        timeline:
            parsed,

        rawLogs:
            rawLogs

    };

}

/* =========================================================
   ANALYZE LOGS
========================================================= */

async function analyzeLogs() {

    const textarea =
        document.getElementById(
            "log-input"
        );

    const rawLogs =
        textarea.value.trim();

    if (!rawLogs) {

        alert(
            "Please paste logs or upload a log file first."
        );

        return;

    }

    const parsed =
        parseLogs(rawLogs);

    if (!parsed.length) {

        alert(
            "Invalid log format.\n\nExpected format:\n[10:31:42] [Service] Message"
        );

        return;

    }

    const progress =
        document.getElementById(
            "analysis-progress"
        );

    progress.classList.remove(
        "hidden"
    );

    resetProgress();

    await delay(500);

    activateStep(1);

    await delay(700);

    completeStep(1);

    activateStep(2);

    await delay(700);

    completeStep(2);

    activateStep(3);

    await delay(900);

    completeStep(3);

    activateStep(4);

    await delay(700);

    completeStep(4);

    let newIncident;

    try {

        const response = await fetch(
            `${API_BASE_URL}/incidents/analyze`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    logs: rawLogs
                })
            }
        );

        if (!response.ok) {
            throw new Error(`Backend returned ${response.status}`);
        }

        newIncident = await response.json();

    } catch (error) {

        console.info(
            "Backend analysis unavailable; using browser analysis.",
            error
        );

        const result =
            analyzeIncident(
                parsed,
                rawLogs
            );

        newIncident = {

            id:
                generateIncidentId(),

            title:
                result.title,

            severity:
                result.severity,

            status:
                "INVESTIGATING",

            rootCause:
                result.rootCause,

            confidence:
                result.confidence,

            createdAt:
                getCurrentDateTime(),

            timeline:
                result.timeline,

            affectedServices:
                result.affectedServices,

            evidence:
                result.evidence,

            recommendations:
                result.recommendations,

            rawLogs:
                result.rawLogs

        };

    }

    incidents.unshift(
        newIncident
    );

    saveIncidents();

    await delay(400);

    openIncident(
        newIncident.id
    );

}

/* =========================================================
   PROGRESS HELPERS
========================================================= */

function resetProgress() {

    for (
        let i = 1;
        i <= 4;
        i++
    ) {

        const step =
            document.getElementById(
                "step-" + i
            );

        step.classList.remove(
            "active",
            "completed"
        );

    }

}

function activateStep(number) {

    document.getElementById(
        "step-" + number
    ).classList.add(
        "active"
    );

}

function completeStep(number) {

    const step =
        document.getElementById(
            "step-" + number
        );

    step.classList.remove(
        "active"
    );

    step.classList.add(
        "completed"
    );

    step.querySelector(
        ".step-icon"
    ).textContent = "OK";

}

/* =========================================================
   UTILITIES
========================================================= */

function delay(ms) {

    return new Promise(
        resolve =>
            setTimeout(
                resolve,
                ms
            )
    );

}

function generateIncidentId() {

    if (!incidents.length) {

        return 1001;

    }

    return Math.max(
        ...incidents.map(
            incident =>
                Number(incident.id)
        )
    ) + 1;

}

function getCurrentDateTime() {

    const now =
        new Date();

    const year =
        now.getFullYear();

    const month =
        String(
            now.getMonth() + 1
        ).padStart(2, "0");

    const day =
        String(
            now.getDate()
        ).padStart(2, "0");

    const hours =
        String(
            now.getHours()
        ).padStart(2, "0");

    const minutes =
        String(
            now.getMinutes()
        ).padStart(2, "0");

    const seconds =
        String(
            now.getSeconds()
        ).padStart(2, "0");

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;

}

function formatDate(value) {

    if (!value) {

        return "-";

    }

    return value;

}

function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";

    }

    return String(value)

        .replace(
            /&/g,
            "&amp;"
        )

        .replace(
            /</g,
            "&lt;"
        )

        .replace(
            />/g,
            "&gt;"
        )

        .replace(
            /"/g,
            "&quot;"
        )

        .replace(
            /'/g,
            "&#039;"
        );

}

function emptyState(
    title,
    description
) {

    return `

        <div class="panel"
             style="
                grid-column:1/-1;
                text-align:center;
                padding:50px;
             ">

            <h3>
                ${escapeHtml(title)}
            </h3>

            <p style="
                color:var(--muted);
                font-size:12px;
            ">

                ${escapeHtml(
                    description
                )}

            </p>

        </div>

    `;

}
