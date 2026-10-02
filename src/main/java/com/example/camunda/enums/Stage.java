package com.example.camunda.enums;

import java.util.Map;
import java.util.Optional;

/**
 * The three sequential stages in SAM_Team_Process.bpmn.
 *
 * subProcessActivityId  - bpmn:subProcess id, used as the "ancestor scope"
 *                          when instantiating a task via Process Instance
 *                          Modification.
 * readyMessageName       - the message correlated to the stage's
 *                          INTERRUPTING boundary event: firing it cancels
 *                          this stage's sub-process (and any of its tasks
 *                          still open) and opens the NEXT stage. Once fired,
 *                          this stage is permanently closed - it cannot be
 *                          reopened, and its tasks can no longer be
 *                          triggered. null for the last stage (Stage 3),
 *                          since there is nothing after it to open.
 * taskActivityIds         - maps each TaskKey (A/B/C) to the exact
 *                          bpmn:userTask id inside this stage's
 *                          sub-process.
 */
public enum Stage {

    STAGE_1(
            "Stage_1",
            "Stage1ReadyMessage",
            Map.of(
                    TaskKey.TASK_A, "Stage1_Task_A",
                    TaskKey.TASK_B, "Stage1_Task_B",
                    TaskKey.TASK_C, "Stage1_Task_C"
            )
    ),
    STAGE_2(
            "Stage_2",
            "Stage2ReadyMessage",
            Map.of(
                    TaskKey.TASK_A, "Stage2_Task_A",
                    TaskKey.TASK_B, "Stage2_Task_B",
                    TaskKey.TASK_C, "Stage2_Task_C"
            )
    ),
    STAGE_3(
            "Stage_3",
            null,
            Map.of(
                    TaskKey.TASK_A, "Stage3_Task_A",
                    TaskKey.TASK_B, "Stage3_Task_B",
                    TaskKey.TASK_C, "Stage3_Task_C"
            )
    );

    private final String subProcessActivityId;
    private final String readyMessageName;
    private final Map<TaskKey, String> taskActivityIds;

    Stage(String subProcessActivityId, String readyMessageName, Map<TaskKey, String> taskActivityIds) {
        this.subProcessActivityId = subProcessActivityId;
        this.readyMessageName = readyMessageName;
        this.taskActivityIds = taskActivityIds;
    }

    public String getSubProcessActivityId() {
        return subProcessActivityId;
    }

    public Optional<String> getReadyMessageName() {
        return Optional.ofNullable(readyMessageName);
    }

    /** The bpmn:userTask id for a given task slot within this stage. */
    public String taskActivityId(TaskKey taskKey) {
        return taskActivityIds.get(taskKey);
    }

    public boolean hasNextStage() {
        return readyMessageName != null;
    }
}
