package com.tbm.admin.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Ec2State {

    RUNNING("running"),
    STOPPED("stopped"),
    TERMINATED("terminated"),
    PENDING("pending"),
    SHUTTING_DOWN("shutting-down"),
    STOPPING("stopping"),
    ;

    private final String state;

    public static Ec2State of(String state) {
        return Ec2State.valueOf(state.toUpperCase());
    }
}
