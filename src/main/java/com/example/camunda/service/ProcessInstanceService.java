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
                .ended(instance.isEnded())
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

    private Map<String, Object> getCurrentVariables(String processInstanceId) {
        // The process may have already run to completion synchronously (no wait states),
        // in which case runtime variables are gone but history still has them.
        if (runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count() > 0) {
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
