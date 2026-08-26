package com.example.camunda.service;

import com.example.camunda.dto.ProcessInstanceStatusResponse;
import com.example.camunda.dto.StartProcessRequest;
import com.example.camunda.dto.StartProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
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
