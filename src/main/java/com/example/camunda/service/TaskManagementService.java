package com.example.camunda.service;

import com.example.camunda.dto.BulkTaskOperationResult;
import com.example.camunda.dto.TaskInfo;
import com.example.camunda.dto.TaskOperationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskAlreadyClaimedException;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Generic operations on Camunda user tasks: search, fetch, assign, claim/unclaim,
 * complete, and read/write task variables — independent of which process definition
 * the task belongs to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskManagementService {

    private final TaskService taskService;

    public List<TaskInfo> findTasks(String processInstanceId, String processDefinitionKey,
                                     String assignee, String candidateGroup, String candidateUser,
                                     String taskDefinitionKey, Boolean unassigned) {

        TaskQuery query = taskService.createTaskQuery();

        if (StringUtils.hasText(processInstanceId)) {
            query.processInstanceId(processInstanceId);
        }
        if (StringUtils.hasText(processDefinitionKey)) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (StringUtils.hasText(assignee)) {
            query.taskAssignee(assignee);
        }
        if (StringUtils.hasText(candidateGroup)) {
            query.taskCandidateGroup(candidateGroup);
        }
        if (StringUtils.hasText(candidateUser)) {
            query.taskCandidateUser(candidateUser);
        }
        if (StringUtils.hasText(taskDefinitionKey)) {
            query.taskDefinitionKey(taskDefinitionKey);
        }
        if (Boolean.TRUE.equals(unassigned)) {
            query.taskUnassigned();
        }

        return query.orderByTaskCreateTime().desc().list().stream()
                .map(this::toTaskInfo)
                .collect(Collectors.toList());
    }

    public TaskInfo getTask(String taskId) {
        return toTaskInfo(requireTask(taskId));
    }

    public TaskInfo assign(String taskId, String userId) {
        requireUserId(userId);
        // taskService.setAssignee throws for an unknown taskId itself (mapped to 404
        // by GlobalExceptionHandler), so no separate existence check is needed here.
        taskService.setAssignee(taskId, userId);
        log.info("Assigned task {} to {}", taskId, userId);
        return toTaskInfo(requireTask(taskId));
    }

    public TaskInfo unassign(String taskId) {
        taskService.setAssignee(taskId, null);
        log.info("Unassigned task {}", taskId);
        return toTaskInfo(requireTask(taskId));
    }

    public TaskInfo claim(String taskId, String userId) {
        requireUserId(userId);
        try {
            // Throws org.camunda.bpm.engine.TaskAlreadyClaimedException if already
            // claimed by a different user, or a not-found exception for an unknown
            // taskId (handled centrally by GlobalExceptionHandler -> HTTP 404).
            taskService.claim(taskId, userId);
        } catch (TaskAlreadyClaimedException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Task '" + taskId + "' is already claimed by '" + ex.getTaskAssignee() + "'.");
        }
        log.info("Task {} claimed by {}", taskId, userId);
        return toTaskInfo(requireTask(taskId));
    }

    public TaskInfo unclaim(String taskId) {
        taskService.claim(taskId, null);
        log.info("Task {} unclaimed", taskId);
        return toTaskInfo(requireTask(taskId));
    }

    /**
     * Assigns multiple tasks to the same user in one call. Camunda's engine has no
     * native bulk-assign operation, so each task id is processed individually; a
     * single bad/unknown id records a failure for that id but does not abort the rest
     * of the batch.
     */
    public BulkTaskOperationResult bulkAssign(List<String> taskIds, String userId) {
        requireTaskIds(taskIds);
        requireUserId(userId);
        return processBulk(taskIds, taskId -> taskService.setAssignee(taskId, userId));
    }

    /** Clears the assignee on multiple tasks in one call, with the same per-task partial-failure semantics as {@link #bulkAssign}. */
    public BulkTaskOperationResult bulkUnassign(List<String> taskIds) {
        requireTaskIds(taskIds);
        return processBulk(taskIds, taskId -> taskService.setAssignee(taskId, null));
    }

    private BulkTaskOperationResult processBulk(List<String> taskIds, Consumer<String> operation) {
        List<String> successfulTaskIds = new ArrayList<>();
        List<TaskOperationFailure> failures = new ArrayList<>();

        for (String taskId : taskIds) {
            if (!StringUtils.hasText(taskId)) {
                failures.add(TaskOperationFailure.builder()
                        .taskId(taskId)
                        .errorMessage("Task id must not be blank.")
                        .build());
                continue;
            }
            try {
                operation.accept(taskId);
                successfulTaskIds.add(taskId);
            } catch (RuntimeException ex) {
                failures.add(TaskOperationFailure.builder()
                        .taskId(taskId)
                        .errorMessage(ex.getMessage())
                        .build());
            }
        }

        log.info("Bulk task operation processed {} id(s): {} succeeded, {} failed",
                taskIds.size(), successfulTaskIds.size(), failures.size());

        return BulkTaskOperationResult.builder()
                .successCount(successfulTaskIds.size())
                .failureCount(failures.size())
                .successfulTaskIds(successfulTaskIds)
                .failures(failures)
                .build();
    }

    public void complete(String taskId, Map<String, Object> variables) {
        // taskService.complete throws for an unknown taskId itself.
        if (CollectionUtils.isEmpty(variables)) {
            taskService.complete(taskId);
        } else {
            taskService.complete(taskId, variables);
        }
        log.info("Completed task {}", taskId);
    }

    public Map<String, Object> getVariables(String taskId) {
        // Explicit existence check so an unknown taskId reliably surfaces our own
        // clear 404 (rather than depending on the exact failure behavior of the
        // underlying variable-lookup command, which isn't guaranteed here).
        ensureTaskExists(taskId);
        return taskService.getVariables(taskId);
    }

    /**
     * Adds/updates one or more variables on the task's scope without completing it.
     * Existing variables not present in the given map are left untouched.
     */
    public Map<String, Object> setVariables(String taskId, Map<String, Object> variables) {
        ensureTaskExists(taskId);
        if (!CollectionUtils.isEmpty(variables)) {
            taskService.setVariables(taskId, variables);
        }
        return taskService.getVariables(taskId);
    }

    /** Fetches the task, throwing 404 if it doesn't exist. Use when the caller needs the Task object. */
    private Task requireTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No active task found with id '" + taskId + "'.");
        }
        return task;
    }

    /** Validates the task exists, throwing 404 if not. Use when only the existence check is needed. */
    private void ensureTaskExists(String taskId) {
        boolean exists = taskService.createTaskQuery().taskId(taskId).count() > 0;
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No active task found with id '" + taskId + "'.");
        }
    }

    private void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("'userId' is required.");
        }
    }

    private void requireTaskIds(List<String> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            throw new IllegalArgumentException("'taskIds' is required and must not be empty.");
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
}
