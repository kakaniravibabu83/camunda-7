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

**Add or update variables** on a running process instance — existing variables not
included in the body are left untouched. Only works while the instance is still active
(`409 Conflict` if it has already ended):
```
POST /api/camunda/process-instances/{processInstanceId}/variables
{ "amount": 300.00, "approved": true }
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

**Bulk assign** — assign multiple tasks to the same user in one call. A single unknown/
invalid task id does not fail the whole batch; the response reports success/failure per
id (Camunda's engine has no native bulk-assign operation, so this loops per task
internally):
```
POST /api/camunda/tasks/bulk-assign
{ "taskIds": ["task-1", "task-2", "task-3"], "userId": "jane" }
```
Response:
```json
{
  "successCount": 2,
  "failureCount": 1,
  "successfulTaskIds": ["task-1", "task-3"],
  "failures": [
    { "taskId": "task-2", "errorMessage": "..." }
  ]
}
```

**Bulk unassign** — same partial-failure semantics as bulk assign:
```
POST /api/camunda/tasks/bulk-unassign
{ "taskIds": ["task-1", "task-2", "task-3"] }
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

### 7. DMN-driven dynamic routing (Call Activity + calledElementExpression)

Four auto-deployed artifacts demonstrate evaluating a DMN decision and using its result
to dynamically choose which process to invoke next — no gateway/branching needed in the
calling process itself:

- **`classification-decision.dmn`** (decision key `classificationDecision`) — a decision
  table classifying a `requestAmount` into `STANDARD` / `PRIORITY` / `URGENT`
  (`hitPolicy="FIRST"`, thresholds at 1,000 and 10,000).
- **`classification-routing-process.bpmn`** (key `classificationRoutingProcess`) — the
  main workflow:
  1. A **Business Rule Task** evaluates `classificationDecision` and writes its single
     scalar result straight into the `classificationType` process variable
     (`camunda:mapDecisionResult="singleEntry"`).
  2. A **Call Activity** uses `calledElementExpression` (instead of a static
     `calledElement`) to pick which process to invoke **at runtime** based on
     `classificationType`:
     ```
     ${classificationType == 'URGENT' ? 'urgentHandlerProcess'
        : (classificationType == 'PRIORITY' ? 'priorityHandlerProcess' : 'standardHandlerProcess')}
     ```
- **`standard-handler-process.bpmn` / `priority-handler-process.bpmn` /
  `urgent-handler-process.bpmn`** — the three possible target processes (keys
  `standardHandlerProcess`, `priorityHandlerProcess`, `urgentHandlerProcess`). Each just
  records which handler ran (`handledBy`), mapped back to the caller via `camunda:out`,
  so the routing can be verified end-to-end.

Test it with the existing generic process-start API:
```bash
curl -X POST http://localhost:8080/api/camunda/process-instances/start \
     -H "Content-Type: application/json" \
     -d '{"processDefinitionKey": "classificationRoutingProcess", "variables": {"requestAmount": 50000}}'
# -> variables.classificationType = "URGENT", variables.handledBy = "urgent-handler"
```

All five files (one DMN, four BPMN) open cleanly in Camunda Desktop Modeler — each has
complete diagram interchange info, and the DMN includes its own DMNDI diagram section.

### 8. On-demand case management (external UI triggers tasks in any order)

`case-management-process.bpmn` (key `caseManagementProcess`) models one process instance
per "case", where an external case management UI decides **at runtime** which task to
create next — not the process definition. This intentionally avoids Camunda 7's ad-hoc
sub-process BPMN construct, which has no reliable runtime support in the Camunda 7 engine
(only in Camunda 8/Zeebe, a different product). Instead it uses
`RuntimeService#createProcessInstanceModification` — Camunda 7's actual, documented
mechanism for instantiating any named activity in a running process instance on demand —
exposed generically as two new endpoints:

```
POST /api/camunda/process-instances/{processInstanceId}/trigger-activity
{ "activityId": "UserTask_LegalReview", "variables": { "note": "please expedite" } }
```
Creates the named activity (its BPMN element id, not its display name) right now,
regardless of the process's own default flow. `variables` is optional. Returns the
resulting task(s). Can be called any number of times, in any order, for any of the five
task activity ids below.

```
POST /api/camunda/process-instances/{processInstanceId}/cancel-activity
{ "activityId": "SubProcess_CaseTasks" }
```
Cancels all currently-open instances of the named activity in one call, regardless of
what's active inside it — used here to close a case.

**Structure:** starting a case auto-creates a **SAM** task by default (the gateway's
default branch — this keeps the process instance genuinely alive as a wait-state rather
than doing nothing at all). SAM's task is never touched by anything else and stays open
for the whole life of the case, so after any on-demand task completes, control is simply
"back with SAM" because SAM's task was never interrupted. The five task branches
(`UserTask_BusinessConfirmation`, `UserTask_LegalReview`, `UserTask_BusinessApproval`,
`UserTask_FinanceApproval`, `UserTask_Procurement`) each carry an unsatisfiable
`${false}` gateway condition, so they're structurally valid/deployable but can **only**
ever be created via `trigger-activity`.

**Step-by-step API walkthrough** (no UI needed — this is exactly what
`CaseManagementProcessTest` automates):

1. **Open a case** — creates the case process instance and its default SAM task:
   ```bash
   curl -X POST http://localhost:8080/api/camunda/process-instances/start \
        -H "Content-Type: application/json" \
        -d '{"processDefinitionKey": "caseManagementProcess"}'
   ```
   Copy `processInstanceId` from the response for the steps below.

2. **See what's open right now:**
   ```bash
   curl "http://localhost:8080/api/camunda/tasks?processInstanceId=<processInstanceId>"
   ```
   Shows one task: SAM (`taskDefinitionKey: "UserTask_Sam"`).

3. **Trigger Legal Review on demand:**
   ```bash
   curl -X POST http://localhost:8080/api/camunda/process-instances/<processInstanceId>/trigger-activity \
        -H "Content-Type: application/json" \
        -d '{"activityId": "UserTask_LegalReview"}'
   ```
   Copy the returned task's `id`. Querying tasks again now shows two: SAM and Legal Review.

4. **Complete Legal Review** (existing generic task endpoint):
   ```bash
   curl -X POST http://localhost:8080/api/camunda/tasks/<legalReviewTaskId>/complete
   ```
   SAM remains open — nothing else needed to get "back to SAM".

5. **Trigger any other task, in any order** — e.g. skip straight to Finance Approval:
   ```bash
   curl -X POST http://localhost:8080/api/camunda/process-instances/<processInstanceId>/trigger-activity \
        -H "Content-Type: application/json" \
        -d '{"activityId": "UserTask_FinanceApproval"}'
   ```
   Repeat steps 3–5 for `UserTask_BusinessConfirmation`, `UserTask_BusinessApproval`, and
   `UserTask_Procurement` in whatever order the case needs — each is independent, and any
   of them can be triggered more than once if needed.

6. **Close the case** once everything needed is done:
   ```bash
   curl -X POST http://localhost:8080/api/camunda/process-instances/<processInstanceId>/cancel-activity \
        -H "Content-Type: application/json" \
        -d '{"activityId": "SubProcess_CaseTasks"}'
   ```
   Cancels SAM plus anything else still open, and the process instance completes.
   Confirm with:
   ```bash
   curl http://localhost:8080/api/camunda/process-instances/<processInstanceId>
   # -> "state": "INTERNALLY_TERMINATED"
   ```
   (Camunda's correct label for a cancellation-driven close, as opposed to `COMPLETED`
   which it reserves for reaching an end event through uninterrupted normal flow — the
   case still closed correctly either way.)

### 9. User & role management

Standard CRUD for users and roles, independent of the Camunda engine (plain JPA/MySQL,
same pattern as the `deployment_log` audit table). A user can be associated with any
number of roles; roles are managed independently and referenced by id when assigning
them to a user.

**Roles** (`app_role` table — not `role`, since ROLE is a reserved word in H2):
```
POST   /api/roles           { "name": "ROLE_APPROVER", "description": "Can approve requests" }
PUT    /api/roles/{id}      same body, full replacement
DELETE /api/roles/{id}      409 if still assigned to any user — remove it from them first
GET    /api/roles           list all
```

**Users** (`app_user` table):
```
POST   /api/users
{
  "firstName": "Jane", "lastName": "Doe", "phone": "+1-555-0100",
  "email": "jane.doe@example.com", "businessUnit": "Operations",
  "roleIds": [1, 2]
}

PUT    /api/users/{id}      same body — roleIds is a full replacement of the role set,
                             not an add; omit/empty it to clear all roles
DELETE /api/users/{id}
GET    /api/users/{id}      -> user details with full role objects (not just ids)
GET    /api/users           list all, each with their roles
```

Both `DELETE` endpoints return a confirmation body (`{ "id", "deleted", "message" }`)
rather than `204 No Content`, per this feature's spec. Validation (required fields,
email format, field length limits) returns `400` with a message naming every failed
field; duplicate email/role name returns `409`; referencing an unknown role id in
`roleIds` returns `400`.

### 10. Groups — multiple users sharing one role

`app_group` table. Each group is scoped to exactly **one** role, and can hold any
number of users — but only users who **currently hold that role**. This is enforced on
every create/update: adding a user who doesn't have the group's role returns `400`
naming exactly which user(s) and why.

```
POST   /api/groups
{
  "name": "Legal Reviewers", "description": "Handles legal review tasks",
  "roleId": 3, "userIds": [5, 8, 11]
}

PUT    /api/groups/{id}     same body — userIds is a full replacement of membership,
                             not an add; omit/empty it to clear all members
DELETE /api/groups/{id}     returns a confirmation body, same pattern as users/roles
GET    /api/groups/{id}     -> role + members (compact: id/firstName/lastName/email —
                             full role lists per member would be redundant here, since
                             every member shares the group's own role)
GET    /api/groups          list all
```

**Two integrity guards this introduces on the existing endpoints**, both returning
`409` rather than corrupting a group or hitting a raw foreign-key constraint error:
- `DELETE /api/roles/{id}` also now fails if any group is still scoped to that role.
- `DELETE /api/users/{id}` also now fails if the user is still a member of any group.

In both cases: update/delete the dependent group first (or update it to no longer
reference the role/user), then the original delete succeeds.

**Known limitation:** removing a role from a user via `PUT /api/users/{id}` does **not**
check whether that user is a member of a group scoped to that role — it's possible to
end up with a group member who no longer actually holds the group's role. Enforcing
that would mean checking group membership on every user role change, which felt like
more cross-cutting complexity than this feature asked for; flagging it here rather than
leaving it a silent gap.

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
