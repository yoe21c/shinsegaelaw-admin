package com.shinsegaelaw.admin.model.param.req;

import lombok.Data;

@Data
public class ScrapRequest {

    private String blogUrl;

    private int targetCount;

    private long memberSeq;


    private String description;
}
