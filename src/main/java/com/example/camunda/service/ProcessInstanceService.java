package com.example.camunda.service;

import com.example.camunda.dto.ProcessInstanceStatusResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import com.example.camunda.dto.TaskInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Starts and inspects process instances for ANY deployed process definition, with
 * variables that are entirely optional and generically typed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;

    public StartProcessResponse start(StartProcessRequest request) {
        boolean hasKey = StringUtils.hasText(request.getProcessDefinitionKey());
        boolean hasId = StringUtils.hasText(request.getProcessDefinitionId());

        if (hasKey == hasId) {
            // both blank OR both provided -> ambiguous / invalid
            throw new IllegalArgumentException(
                    "Provide exactly one of 'processDefinitionKey' or 'processDefinitionId'.");
        }

        Map<String, Object> variables = CollectionUtils.isEmpty(request.getVariables())
                ? new HashMap<>()
                : new HashMap<>(request.getVariables());

        String businessKey = request.getBusinessKey();

        ProcessInstance instance;
        if (hasKey) {
            instance = businessKey != null
                    ? runtimeService.startProcessInstanceByKey(request.getProcessDefinitionKey(), businessKey, variables)
                    : runtimeService.startProcessInstanceByKey(request.getProcessDefinitionKey(), variables);
        } else {
            instance = businessKey != null
                    ? runtimeService.startProcessInstanceById(request.getProcessDefinitionId(), businessKey, variables)
                    : runtimeService.startProcessInstanceById(request.getProcessDefinitionId(), variables);
        }

        log.info("Started process instance {} for definition {} (businessKey={})",
                instance.getId(), instance.getProcessDefinitionId(), instance.getBusinessKey());

        return StartProcessResponse.builder()
                .processInstanceId(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(hasKey ? request.getProcessDefinitionKey() : null)
                .businessKey(instance.getBusinessKey())
                .ended(isEnded(instance.getId()))
                .variables(getCurrentVariables(instance.getId()))
                .build();
    }

    public Map<String, Object> getVariables(String processInstanceId) {
        boolean existsInHistory = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
        if (!existsInHistory) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No process instance found with id '" + processInstanceId + "'.");
        }
        return getCurrentVariables(processInstanceId);
    }

    /**
     * Adds one or more new variables, or updates the value of existing ones, on an
     * ACTIVE process instance. Existing variables not present in the given map are left
     * untouched. Unlike {@link #getVariables}, this only works on process instances
     * that haven't completed yet — {@link RuntimeService#setVariables} operates on a
     * live execution, so there's nothing to write to once a process instance has ended.
     */
    public Map<String, Object> setVariables(String processInstanceId, Map<String, Object> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            throw new IllegalArgumentException("'variables' is required and must not be empty.");
        }

        boolean existsInHistory = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
        if (!existsInHistory) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No process instance found with id '" + processInstanceId + "'.");
        }
        if (isEnded(processInstanceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Process instance '" + processInstanceId + "' has already ended; variables can only be "
                            + "added or updated on an active process instance.");
        }

        runtimeService.setVariables(processInstanceId, variables);
        log.info("Updated {} variable(s) on process instance {}: {}",
                variables.size(), processInstanceId, variables.keySet());

        return getCurrentVariables(processInstanceId);
    }

    /**
     * Correlates a named BPMN message to a specific, already-running process instance —
     * a generic building block for processes that model branches as message-triggered
     * Receive Tasks / message event sub-processes, letting an external caller decide at
     * runtime which message to send, in any order, any number of times, for as long as
     * the process instance stays active and able to receive it.
     * <p>
     * {@code variables} is the message's payload and is entirely optional.
     * <p>
     * Throws {@link org.camunda.bpm.engine.MismatchingMessageCorrelationException}
     * (mapped centrally to 409 Conflict) if the process instance exists but isn't
     * currently able to receive a message with this name — e.g. it has already ended,
     * or it's not currently at a point in the flow that's waiting for it.
     */
    public void correlateMessage(String processInstanceId, String messageName, Map<String, Object> variables) {
        if (!StringUtils.hasText(messageName)) {
            throw new IllegalArgumentException("'messageName' is required.");
        }

        boolean existsInHistory = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
        if (!existsInHistory) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No process instance found with id '" + processInstanceId + "'.");
        }

        try {
            MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(messageName)
                    .processInstanceId(processInstanceId);
            if (!CollectionUtils.isEmpty(variables)) {
                builder.setVariables(variables);
            }
            builder.correlate();
        } catch (MismatchingMessageCorrelationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Process instance '" + processInstanceId + "' is not currently able to receive message '"
                            + messageName + "' (it may have already ended, or may not currently be waiting for "
                            + "this message — e.g. a previously triggered action hasn't been completed yet).");
        }

        log.info("Correlated message '{}' to process instance {}", messageName, processInstanceId);
    }

    /**
     * Dynamically instantiates any named activity in a running process instance on
     * demand, via {@link RuntimeService#createProcessInstanceModification}. This is the
     * actual mechanism behind on-demand, UI-driven task creation: a case management UI
     * (or, until that UI exists, a direct API call) decides at runtime which activity
     * to trigger, in any order, any number of times — completely independent of
     * whatever the process definition's own gateway/sequence-flow logic would normally
     * do. See {@code case-management-process.bpmn} for a full worked example: its five
     * named task branches all carry an unsatisfiable {@code ${false}} condition, so
     * they're structurally valid (deployable) but can only ever be reached this way.
     * <p>
     * {@code activityId} must be the BPMN element id (not name), e.g.
     * "UserTask_LegalReview". {@code variables} is optional.
     * <p>
     * Returns the resulting task(s) for that activity, if any were created (a User Task
     * creates exactly one; other activity types may create none, if they complete
     * immediately rather than waiting).
     */
    public List<TaskInfo> triggerActivity(String processInstanceId, String activityId, Map<String, Object> variables) {
        if (!StringUtils.hasText(activityId)) {
            throw new IllegalArgumentException("'activityId' is required.");
        }
        requireActiveProcessInstance(processInstanceId);

        var modification = runtimeService.createProcessInstanceModification(processInstanceId)
                .startBeforeActivity(activityId);
        if (!CollectionUtils.isEmpty(variables)) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                modification = modification.setVariable(entry.getKey(), entry.getValue());
            }
        }
        modification.execute();

        log.info("Triggered activity '{}' on process instance {}", activityId, processInstanceId);

        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(activityId)
                .list()
                .stream()
                .map(this::toTaskInfo)
                .collect(Collectors.toList());
    }

    /**
     * Cancels all currently active instances of a named activity in a running process
     * instance, via {@link RuntimeService#createProcessInstanceModification}. Used to
     * force-complete a wrapping activity regardless of what's active inside it — e.g.
     * closing out a case by cancelling its whole "case tasks" sub-process in one call,
     * whatever tasks happen to be open inside it at the time — after which the process
     * instance proceeds along the cancelled activity's own outgoing flow as normal.
     */
    public void cancelActivity(String processInstanceId, String activityId) {
        if (!StringUtils.hasText(activityId)) {
            throw new IllegalArgumentException("'activityId' is required.");
        }
        requireActiveProcessInstance(processInstanceId);

        runtimeService.createProcessInstanceModification(processInstanceId)
                .cancelAllForActivity(activityId)
                .execute();

        log.info("Cancelled all instances of activity '{}' on process instance {}", activityId, processInstanceId);
    }

    public ProcessInstanceStatusResponse getStatus(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (instance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No process instance found with id '" + processInstanceId + "'.");
        }

        return ProcessInstanceStatusResponse.builder()
                .processInstanceId(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .businessKey(instance.getBusinessKey())
                .state(instance.getState())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .durationInMillis(instance.getDurationInMillis())
                .build();
    }

    /**
     * Whether a process instance has actually completed, checked via a live query
     * rather than trusting {@code ProcessInstance#isEnded()} on an object handed back
     * at start time. For processes that complete synchronously through several
     * cascading steps (e.g. a boundary event cancelling an activity and rerouting into
     * a Call Activity before reaching an end event), that in-memory snapshot can be
     * stale even though the persisted state is fully consistent — so we re-check
     * directly instead.
     */
    private boolean isEnded(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count() == 0;
    }

    /** Throws 404 if the process instance never existed, or 409 if it has already ended. */
    private void requireActiveProcessInstance(String processInstanceId) {
        boolean existsInHistory = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() > 0;
        if (!existsInHistory) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No process instance found with id '" + processInstanceId + "'.");
        }
        if (isEnded(processInstanceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Process instance '" + processInstanceId + "' has already ended.");
        }
    }

    private TaskInfo toTaskInfo(Task task) {
        return TaskInfo.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .executionId(task.getExecutionId())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .priority(task.getPriority())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .build();
    }

    private Map<String, Object> getCurrentVariables(String processInstanceId) {
        // The process may have already run to completion synchronously (no wait states),
        // in which case runtime variables are gone but history still has them.
        if (!isEnded(processInstanceId)) {
            return runtimeService.getVariables(processInstanceId);
        }
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getName,
                        HistoricVariableInstance::getValue,
                        (a, b) -> b));
    }
}
