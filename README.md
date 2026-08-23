# camunda-springboot-app

Spring Boot 3 application embedding the **Camunda 7 (Community Edition, 7.24.0)** process
engine, backed by **MySQL**, with **Spring Data JPA**, **Spring Boot DevTools**, and
**Spring Boot Actuator**, plus a **generic REST API** to deploy *any* BPMN/DMN/CMMN process
and start process instances with (optional) variables.

## Stack

| Concern              | Technology                                            |
|-----------------------|-------------------------------------------------------|
| Language / Build      | Java 17, Maven                                        |
| Framework              | Spring Boot 3.3.4                                     |
| Process engine         | Camunda 7.24.0 (`camunda-bpm-spring-boot-starter-rest`, `-webapp`) |
| Database (prod)        | MySQL 8 (`com.mysql:mysql-connector-j`)               |
| Database (tests)       | H2 in-memory (no MySQL required to run the test suite) |
| Persistence            | Spring Data JPA / Hibernate                            |
| Ops                     | Spring Boot Actuator, Spring Boot DevTools             |

## Project layout

```
src/main/java/com/example/camunda
├── CamundaApplication.java          # main() entry point
├── controller/                      # generic REST API
│   ├── ProcessDeploymentController.java   # POST /api/camunda/deployments
│   └── ProcessInstanceController.java     # POST /api/camunda/process-instances/start, GET .../{id}, GET .../{id}/variables
├── service/
│   ├── ProcessDeploymentService.java      # deploys any uploaded BPMN/DMN/CMMN file
│   └── ProcessInstanceService.java        # starts/queries process instances generically
├── entity/DeploymentLog.java        # JPA entity: audit trail of deployments (MySQL)
├── repository/DeploymentLogRepository.java
├── dto/                             # request/response payloads
└── exception/GlobalExceptionHandler.java  # consistent JSON error responses

src/main/resources
├── application.yml                  # MySQL + Camunda + Actuator config
└── processes/sample-approval-process.bpmn   # demo process, auto-deployed at startup

src/test/...                         # MockMvc + JPA slice tests, run against H2
```

## Running locally

The app is pre-configured to connect to a **local MySQL instance** with:
- schema: `camunda`
- username: `camunda7`

You only need to supply the password — never commit it to `application.yml`. Pick one:

**Option A — environment variable (recommended)**
```bash
# macOS/Linux
export DB_PASSWORD=your_password_here
mvn spring-boot:run
```
```powershell
# Windows PowerShell
$env:DB_PASSWORD="your_password_here"
mvn spring-boot:run
```

**Option B — IntelliJ run configuration**
`Run ▸ Edit Configurations... ▸ CamundaApplication ▸ Environment variables` and add
`DB_PASSWORD=your_password_here`.

If `camunda7` doesn't have `CREATE` privileges and the `camunda` schema doesn't already
exist, either create it manually first (`CREATE DATABASE camunda;`) or drop
`createDatabaseIfNotExist=true` from the JDBC URL after doing so.

Once the password is set:
1. Run the app:
   ```bash
   mvn spring-boot:run
   ```
2. The app starts on `http://localhost:8080`:
    - Custom generic REST API: `http://localhost:8080/api/camunda/**`
    - Camunda's own REST API: `http://localhost:8080/engine-rest/**`
    - Camunda Cockpit/Tasklist/Admin: `http://localhost:8080/camunda/` (login `admin` / `admin`)
    - Actuator: `http://localhost:8080/actuator/health`

Datasource credentials can also be overridden entirely with `DB_URL` if your MySQL isn't
on `localhost:3306`.

`docker-compose.yml` is included as an optional convenience if you'd rather run MySQL in
a container instead of your local instance — not needed if you already have one running.

## Importing into IntelliJ IDEA

1. `File ▸ Open...` and select the project's `pom.xml` (or the project root — IntelliJ
   detects the Maven project automatically).
2. Let IntelliJ download dependencies (first import may take a minute).
3. Make sure the **Lombok** plugin is enabled (bundled with recent IntelliJ versions) and
   annotation processing is on — IntelliJ enables it automatically for Maven projects with
   Lombok on the classpath, but if you see "cannot find symbol" for `getX()/builder()`
   methods, go to `Settings ▸ Build, Execution, Deployment ▸ Compiler ▸ Annotation
   Processors` and confirm "Enable annotation processing" is checked.
4. Run `CamundaApplicationTests` (or `mvn test`) — the whole suite runs against an
   in-memory H2 database, so **no MySQL instance is required to build or test the
   project**. MySQL is only needed to actually run the application.

## Generic REST API

### 1. Deploy any BPMN/DMN/CMMN file

```
POST /api/camunda/deployments
Content-Type: multipart/form-data

file=@my-process.bpmn
deploymentName=My Deployment   (optional)
```

```bash
curl -F "file=@my-process.bpmn" -F "deploymentName=My Deployment" \
     http://localhost:8080/api/camunda/deployments
```

Response `201 Created`:
```json
{
  "deploymentId": "a1b2c3d4-...",
  "deploymentName": "My Deployment",
  "deploymentTime": "2026-08-19T10:00:00",
  "deployedProcessDefinitions": [
    { "id": "myProcess:1:abcd", "key": "myProcess", "name": "My Process", "version": 1,
      "resourceName": "my-process.bpmn", "diagramResourceName": null }
  ]
}
```

### 2. Start a process instance — variables are entirely optional

```
POST /api/camunda/process-instances/start
Content-Type: application/json
```

With variables:
```json
{
  "processDefinitionKey": "sampleApprovalProcess",
  "businessKey": "ORDER-1001",
  "variables": { "amount": 250.75, "approved": false, "requester": "jane" }
}
```

Without any variables — just as valid:
```json
{ "processDefinitionKey": "sampleApprovalProcess" }
```

You can also target a specific version via `"processDefinitionId"` instead of
`"processDefinitionKey"` (provide exactly one of the two).

Response `201 Created`:
```json
{
  "processInstanceId": "9f8e7d6c-...",
  "processDefinitionId": "sampleApprovalProcess:1:xyz",
  "processDefinitionKey": "sampleApprovalProcess",
  "businessKey": "ORDER-1001",
  "ended": false,
  "variables": { "amount": 250.75, "approved": false, "requester": "jane" }
}
```

### 3. Inspect a process instance

```
GET /api/camunda/process-instances/{processInstanceId}
GET /api/camunda/process-instances/{processInstanceId}/variables
```

### 4. User tasks — generic across every process definition

**Search / list tasks** — all query params optional and combine as AND filters:
```
GET /api/camunda/tasks?processInstanceId=...&processDefinitionKey=...&assignee=...
    &candidateGroup=...&candidateUser=...&taskDefinitionKey=...&unassigned=true
```

**Get a specific task:**
```
GET /api/camunda/tasks/{taskId}
```

**Assign** (sets the assignee unconditionally, overwriting any existing one):
```
POST /api/camunda/tasks/{taskId}/assign
{ "userId": "jane" }
```

**Unassign** (clears the assignee):
```
POST /api/camunda/tasks/{taskId}/unassign
```

**Claim** (like assign, but fails with `409 Conflict` if already claimed by someone else):
```
POST /api/camunda/tasks/{taskId}/claim
{ "userId": "jane" }
```

**Unclaim:**
```
POST /api/camunda/tasks/{taskId}/unclaim
```

**Read task variables:**
```
GET /api/camunda/tasks/{taskId}/variables
```

**Add/update task variables** without completing the task (body is a plain JSON object
of variable name → value; existing variables not included are left untouched):
```
POST /api/camunda/tasks/{taskId}/variables
{ "comment": "looks good", "score": 8 }
```

**Complete a task** — `variables` is entirely optional, exactly like starting a process:
```
POST /api/camunda/tasks/{taskId}/complete
{ "variables": { "approved": true, "comment": "lgtm" } }
```
Returns `204 No Content` on success.

### 5. Incidents — engine-raised failures and manually-reported custom incidents

Camunda distinguishes two families of incidents, and they're handled differently:
- **Engine-raised** (`failedJob`, `failedExternalTask`) — created automatically when a
  job or external task runs out of retries. These can **only** be cleared by giving the
  underlying job more retries via `/retry` — Camunda's own `resolveIncident` does not
  support them, and neither does this API's `/resolve`.
- **Custom** (any other type) — reported manually via `POST /api/camunda/incidents`,
  e.g. to surface a failure detected by an external system. These are cleared via `/resolve`.

**Search / list incidents** — all query params optional and combine as AND filters:
```
GET /api/camunda/incidents?processInstanceId=...&processDefinitionId=...&incidentType=...
    &executionId=...&activityId=...&failedActivityId=...&causeIncidentId=...
    &rootCauseIncidentId=...&configuration=...&jobDefinitionId=...&tenantId=...
```

**Get a specific incident:**
```
GET /api/camunda/incidents/{incidentId}
```

**Manually report a custom incident** against a running execution (`configuration` and
`message` are optional):
```
POST /api/camunda/incidents
{ "incidentType": "dataQualityIssue", "executionId": "...", "message": "Missing field" }
```

**Resolve a custom incident** (400 if the incident is actually `failedJob`/`failedExternalTask`):
```
POST /api/camunda/incidents/{incidentId}/resolve
```

**Retry the job behind a `failedJob` incident** — clears the incident once the job's
retries are set above zero:
```
POST /api/camunda/incidents/{incidentId}/retry
{ "retries": 1 }
```

**Inspect the failed job and its exception** behind a `failedJob` incident:
```
GET /api/camunda/incidents/{incidentId}/job
GET /api/camunda/incidents/{incidentId}/stacktrace
```

**Dashboard-style summary** — counts of all currently open incidents grouped by type:
```
GET /api/camunda/incidents/statistics
```

A demo process (`processes/incident-demo-process.bpmn`, key `incidentDemoProcess`) is
auto-deployed at startup specifically to let you generate a real `failedJob` incident
end-to-end: start it, and its single service task fails immediately (zero retries
configured) via a small demo delegate (`SimulatedFailureDelegate`), producing an
incident you can drive through every endpoint above.

### 6. Generic BPMN error handling pattern

Two auto-deployed processes demonstrate a reusable pattern for handling **any** BPMN
error thrown anywhere in your process landscape, without hardcoding error codes:

- **`generic-error-handler-subprocess.bpmn`** (key `genericErrorHandlerProcess`) — a
  standalone, reusable process. It is *not* an embedded event subprocess (those can't be
  reused across process definitions); instead it's a normal callable process that any
  main workflow can invoke via a Call Activity, handing it whatever error details it
  captured. It logs the error generically (`GenericErrorHandlerDelegate`) and returns
  `errorHandled=true` / `errorHandledAt=<timestamp>`.
- **`main-error-demo-process.bpmn`** (key `mainErrorDemoProcess`) — shows how to wire a
  main workflow into it:
  1. "Risky Task" simulates business logic that may throw a `BpmnError`
     (`BusinessErrorSimulatingDelegate`), with the error code/message driven by input
     variables so you can prove the pattern works for *any* code, not just one.
  2. A boundary error event on "Risky Task" is defined **with no `errorRef`** — per the
     BPMN spec, this makes it catch *any* error code, not just a specific one. It captures
     the code/message into `errorCode`/`errorMessage` process variables via
     `camunda:errorCodeVariable` / `camunda:errorMessageVariable`.
  3. A Call Activity passes those variables into `genericErrorHandlerProcess` and reads
     `errorHandled`/`errorHandledAt` back out.

Test it with the existing generic process-start API — no new endpoint needed, since this
is just a process definition:
```bash
# Default run: throws "GENERIC_ERROR", caught and handled generically
curl -X POST http://localhost:8080/api/camunda/process-instances/start \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey": "mainErrorDemoProcess"}'

# Any other error code works identically — the handler doesn't care what it's called
curl -X POST http://localhost:8080/api/camunda/process-instances/start \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey": "mainErrorDemoProcess",
          "variables": {"simulateErrorCode": "PAYMENT_DECLINED", "simulateErrorMessage": "Card declined"}}'

# Success path instead (no error thrown, handler never runs)
curl -X POST http://localhost:8080/api/camunda/process-instances/start \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey": "mainErrorDemoProcess", "variables": {"simulateError": false}}'
```
The response's `variables` will include `errorCode`, `errorMessage`, `errorHandled`, and
`errorHandledAt` for the error path, and none of those for the success path.

To reuse this pattern in your own processes: add a boundary error event (no `errorRef`)
to any risky activity, capture `errorCode`/`errorMessage` the same way, and add a Call
Activity to `genericErrorHandlerProcess` — no changes to the subprocess itself required.

## Notes

- The bundled `processes/sample-approval-process.bpmn` (key `sampleApprovalProcess`) is
  deployed automatically on startup — Camunda's Spring Boot starter auto-deploys any
  `.bpmn`/`.dmn`/`.cmmn` resource found on the classpath, so it's ready to use immediately
  via the `start` endpoint above without needing to call the deploy endpoint first.
- Every deployment made through `/api/camunda/deployments` is also recorded in the MySQL
  `deployment_log` table via JPA, as a simple audit trail independent of Camunda's own
  `ACT_RE_DEPLOYMENT` tables.
- History level is set to `full` so historic variables remain queryable even after a
  process instance completes.
