package com.example.camunda.exception;

import com.example.camunda.enums.Stage;

public class StageHasOpenTasksException extends RuntimeException {

    public StageHasOpenTasksException(Stage stage, int openTaskCount) {
        super(stage + " still has " + openTaskCount + " open task(s). Complete or otherwise resolve "
                + "them before marking this stage ready - once the next stage opens, "
                + stage + " is cancelled and its tasks can no longer be completed.");
    }
}
