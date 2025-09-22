package com.shinsegaelaw.admin.model.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class AgentMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // request 영역
    private String routingKey;  // 메시지 전달하는 라우팅키
    private Long scrapQueueSeq; // 스크랩 대기열 시퀀스
    private String id;          // 네이버 아이디
    private String password;    // 네이버 패스워드
    private String ipAddress;
    private String blogUrl;
    private String description;
    private int limitRetryCount;    // 최대 스크랩 시도 횟수 (3회까지만 시도하려면 3으로 설정한다.)
    private String requestTimestamp;    // 어드민에서 큐에 던진일시
    private String receiveTimestamp;    // 컨슈머에서 받은 일시
    private String resultTimestamp;     // 에이전트에서 결과를 다시 큐로 전송하는 일시
    private int elapsedTime;    // 스크랩 소요시간 (큐에서 받아서 최종 결과를 전송하기 직전까지의 걸린시간)

    // result 영역
    private String resultRoutingKey;    // 결과를 전달할 routing key
    private int tryCount;   // 총시도한 횟수, 1번에 성공하면 1이 된다.
    private String code;  // success , failure
    private String message;  // 에러 메시지.

    @JsonIgnore
    public boolean isSuccess() {
        return code.equalsIgnoreCase("success");
    }
}